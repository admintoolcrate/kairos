package app.kairos.lock

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.activity.compose.LocalActivity
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import app.kairos.data.LockSecrets
import app.kairos.ui.components.Heading
import app.kairos.ui.components.LinkButton
import app.kairos.ui.components.PrimaryButton
import app.kairos.ui.components.QuietButton

/** Shows [content] if the lock is off or unlocked; otherwise a calm unlock screen. */
@Composable
fun LockGate(lock: AppLock, secrets: LockSecrets?, onBack: () -> Unit, content: @Composable () -> Unit) {
    val unlocked by lock.unlocked.collectAsState()
    if (secrets == null) return // still loading; a blank frame for a few ms
    if (!secrets.enabled || unlocked) { content(); return }

    val activity = LocalActivity.current as? FragmentActivity
    val canBio = secrets.biometric && activity != null && AppLock.biometricAvailable(activity)
    LaunchedEffect(Unit) { if (canBio) lock.prompt(activity!!) {} }

    var pin by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        Heading("Locked", style = MaterialTheme.typography.displaySmall)
        Text(
            "Your history and settings are locked. Panic help and urge mode never are.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
            value = pin,
            onValueChange = { v -> pin = v.filter { it.isDigit() }.take(8); message = null },
            label = { Text("PIN") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            modifier = Modifier.fillMaxWidth(),
        )
        message?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        PrimaryButton("Unlock", {
            message = when (val r = lock.tryPin(pin, secrets)) {
                AppLock.PinResult.Ok -> null
                AppLock.PinResult.Wrong -> "That PIN didn't match. Try again."
                is AppLock.PinResult.Wait -> "Let's pause a moment. You can try again in ${r.seconds} seconds."
            }
            pin = ""
        })
        if (canBio) QuietButton("Use fingerprint or face", { lock.prompt(activity!!) {} })
        Text(
            "Forgot your PIN? Clearing Kairos's storage in Android settings removes the lock, along with your saved data.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        LinkButton("Back", onBack)
    }
}
