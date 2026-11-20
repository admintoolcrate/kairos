package app.kairos.backup

import android.app.Activity
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import app.kairos.data.Prefs
import app.kairos.data.Settings
import app.kairos.lock.AppLock
import app.kairos.ui.components.LinkButton
import app.kairos.ui.components.PrimaryButton
import app.kairos.ui.components.QuietButton
import app.kairos.ui.settings.Body
import app.kairos.ui.settings.SectionTitle
import app.kairos.ui.settings.SettingsPage
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.time.LocalDate
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class BackupViewModel @Inject constructor(
    private val repo: BackupRepository,
    prefs: Prefs,
    val lock: AppLock,
) : ViewModel() {
    val settings: StateFlow<Settings?> = prefs.settings.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val status = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
    val busy = kotlinx.coroutines.flow.MutableStateFlow(false)

    private fun run(block: suspend () -> String) = viewModelScope.launch {
        busy.value = true
        status.value = try {
            block()
        } catch (e: PassphraseNeededException) {
            "This backup is protected. Enter its passphrase above and try again."
        } catch (e: WrongPassphraseException) {
            "That passphrase didn't open this backup."
        } catch (e: BackupFormatException) {
            e.message
        } catch (e: DriveException) {
            "Google Drive couldn't be reached (${e.code}). Nothing was changed."
        } catch (e: Exception) {
            "Something went wrong. Nothing was changed."
        }
        busy.value = false
    }

    fun exportTo(context: Context, uri: Uri, passphrase: String) = run {
        val text = repo.export(passphrase.toCharArray().takeIf { it.isNotEmpty() })
        withContext(Dispatchers.IO) {
            context.contentResolver.openOutputStream(uri, "wt")!!.use { it.write(text.toByteArray(Charsets.UTF_8)) }
        }
        if (passphrase.isEmpty()) "Saved. This file isn't encrypted, so keep it somewhere private." else "Saved and encrypted."
    }

    fun importFrom(context: Context, uri: Uri, passphrase: String, mode: ImportMode) = run {
        val text = withContext(Dispatchers.IO) {
            context.contentResolver.openInputStream(uri)!!.use { it.readBytes().toString(Charsets.UTF_8) }
        }
        repo.import(text, passphrase.toCharArray().takeIf { it.isNotEmpty() }, mode)
        if (mode == ImportMode.Merge) "Imported and merged." else "Imported."
    }

    fun driveBackup(token: String, passphrase: String) = run {
        val text = repo.export(passphrase.toCharArray())
        withContext(Dispatchers.IO) { DriveClient(token).upload(text) }
        repo.noteDriveBackup(System.currentTimeMillis())
        "Backed up to your Google Drive."
    }

    fun driveRestore(token: String, passphrase: String, mode: ImportMode) = run {
        val text = withContext(Dispatchers.IO) {
            val client = DriveClient(token)
            client.findFile()?.let(client::download)
        } ?: return@run "There's no Kairos backup in this Google account yet."
        repo.import(text, passphrase.toCharArray(), mode)
        "Restored from Google Drive."
    }
}

private enum class DriveAction { Backup, Restore }

@Composable
fun BackupScreen(onBack: () -> Unit, vm: BackupViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val settings by vm.settings.collectAsStateWithLifecycle()
    val status by vm.status.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    var filePass by rememberSaveable { mutableStateOf("") }
    var drivePass by rememberSaveable { mutableStateOf("") }
    var drivePass2 by rememberSaveable { mutableStateOf("") }
    var askMode by remember { mutableStateOf<((ImportMode) -> Unit)?>(null) }
    var pendingDrive by remember { mutableStateOf<DriveAction?>(null) }
    var restoreMode by remember { mutableStateOf(ImportMode.Merge) }

    val createDoc = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let { vm.exportTo(context, it, filePass) }
    }
    val openDoc = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { u -> askMode = { mode -> vm.importFrom(context, u, filePass, mode) } }
    }

    fun withToken(token: String?) {
        val action = pendingDrive ?: return
        pendingDrive = null
        if (token == null) { vm.status.value = "Google Drive access wasn't given. Nothing was changed."; return }
        when (action) {
            DriveAction.Backup -> vm.driveBackup(token, drivePass)
            DriveAction.Restore -> vm.driveRestore(token, drivePass, restoreMode)
        }
    }
    val consent = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        val token = if (result.resultCode == Activity.RESULT_OK) runCatching {
            Identity.getAuthorizationClient(context).getAuthorizationResultFromIntent(result.data).accessToken
        }.getOrNull() else null
        withToken(token)
    }
    fun authorize(action: DriveAction) {
        pendingDrive = action
        vm.lock.expectReturn()
        val request = AuthorizationRequest.builder().setRequestedScopes(listOf(Scope(DriveClient.SCOPE))).build()
        Identity.getAuthorizationClient(context).authorize(request)
            .addOnSuccessListener { r ->
                val pi = r.pendingIntent
                if (r.hasResolution() && pi != null) consent.launch(IntentSenderRequest.Builder(pi.intentSender).build())
                else withToken(r.accessToken)
            }
            .addOnFailureListener { withToken(null) }
    }

    SettingsPage("Backup", onBack) {
        Body("Your cards, contacts, settings and history. Your app-lock PIN is never included.")
        status?.let { Body(it, muted = false) }

        SectionTitle("A file on this phone")
        OutlinedTextField(
            filePass, { filePass = it }, Modifier.fillMaxWidth(), singleLine = true,
            label = { Text("Passphrase (optional)") },
            supportingText = { Text("With a passphrase the file is encrypted. Without one it's plain text.") },
            visualTransformation = PasswordVisualTransformation(),
        )
        PrimaryButton("Export to a file", {
            vm.lock.expectReturn(); createDoc.launch("kairos-${LocalDate.now()}.json")
        })
        QuietButton("Import from a file", { vm.lock.expectReturn(); openDoc.launch(arrayOf("application/json", "text/plain", "*/*")) })

        SectionTitle("Your Google Drive")
        Body(
            "Optional. Kairos keeps one encrypted copy in a hidden folder that only Kairos can see in your own Drive. " +
                "This is the only time Kairos uses the internet. A passphrase is required, and nobody can recover it for you.",
        )
        settings?.driveLastBackup?.takeIf { it > 0 }?.let {
            Body("Last backup: " + DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(it)))
        }
        OutlinedTextField(
            drivePass, { drivePass = it }, Modifier.fillMaxWidth(), singleLine = true,
            label = { Text("Passphrase") }, visualTransformation = PasswordVisualTransformation(),
        )
        OutlinedTextField(
            drivePass2, { drivePass2 = it }, Modifier.fillMaxWidth(), singleLine = true,
            label = { Text("Passphrase again (for backing up)") }, visualTransformation = PasswordVisualTransformation(),
        )
        PrimaryButton(if (busy) "Working…" else "Back up to Drive", {
            when {
                busy -> Unit
                drivePass.length < 8 -> vm.status.value = "Please use a passphrase of at least 8 characters."
                drivePass != drivePass2 -> vm.status.value = "The two passphrases don't match yet."
                else -> authorize(DriveAction.Backup)
            }
        })
        QuietButton("Restore from Drive", {
            if (drivePass.isEmpty()) vm.status.value = "Enter the passphrase you backed up with."
            else askMode = { mode -> restoreMode = mode; authorize(DriveAction.Restore) }
        })
    }

    askMode?.let { go ->
        AlertDialog(
            onDismissRequest = { askMode = null },
            title = { Text("Bring it in how?") },
            text = { Text("Merge keeps everything here and adds what's new. Replace swaps what's here for the backup.") },
            confirmButton = { LinkButton("Merge", { askMode = null; go(ImportMode.Merge) }) },
            dismissButton = { LinkButton("Replace", { askMode = null; go(ImportMode.Replace) }) },
        )
    }
}
