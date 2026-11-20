package app.kairos.ui.settings

import android.app.StatusBarManager
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.kairos.BuildConfig
import app.kairos.R
import app.kairos.data.Defaults
import app.kairos.data.Settings
import app.kairos.engine.BreathPattern
import app.kairos.engine.CrisisLines
import app.kairos.engine.UrgeTimer
import app.kairos.entry.KairosTileService
import app.kairos.entry.KairosWidgetReceiver
import app.kairos.lock.AppLock
import app.kairos.lock.PinHasher
import app.kairos.ui.components.PaperCard
import app.kairos.ui.components.PrimaryButton
import app.kairos.ui.components.QuietButton
import app.kairos.ui.theme.MotionPref
import app.kairos.ui.theme.ThemeMode

object SettingsRoutes {
    const val Index = "settings"
    const val Cards = "settings/cards"
    const val Contacts = "settings/contacts"
    const val Crisis = "settings/crisis"
    const val Breathing = "settings/breathing"
    const val Urge = "settings/urge"
    const val Look = "settings/look"
    const val Access = "settings/access"
    const val Lock = "settings/lock"
    const val Backup = "settings/backup"
    const val Stagira = "settings/stagira"
    const val About = "settings/about"
}

@Composable
fun SettingsIndex(vm: SettingsViewModel, onBack: () -> Unit, go: (String) -> Unit, showStagira: Boolean, showBackup: Boolean = true) {
    val s by vm.settings.collectAsStateWithLifecycle()
    val cards by vm.cards.collectAsStateWithLifecycle()
    val contacts by vm.contacts.collectAsStateWithLifecycle()
    val crisis by vm.crisis.collectAsStateWithLifecycle()
    val secrets by vm.secrets.collectAsStateWithLifecycle()
    val settings = s ?: return
    SettingsPage("Settings", onBack) {
        Body("Set these up while you feel steady, so they're ready when you need them.")
        NavRow("Your words", "${cards.size} reassurance cards", { go(SettingsRoutes.Cards) })
        NavRow("People to call", if (contacts.isEmpty()) "No one yet" else contacts.joinToString { it.name }, { go(SettingsRoutes.Contacts) })
        NavRow("Support line", crisis?.line?.let { "${it.name}, ${it.number}" } ?: "Not set for your region", { go(SettingsRoutes.Crisis) })
        NavRow("Breathing", "In ${settings.inhaleSeconds}, out ${settings.exhaleSeconds}" + if (settings.haptics) ", gentle taps" else "", { go(SettingsRoutes.Breathing) })
        NavRow("Urge mode", "${settings.urgeMinutes} minutes, ${settings.distractions.size} things to do", { go(SettingsRoutes.Urge) })
        NavRow("Appearance and motion", "${settings.theme.label()}, ${settings.motion.label()}", { go(SettingsRoutes.Look) })
        NavRow("Getting here fast", "Quick Settings tile, widget, lock screen", { go(SettingsRoutes.Access) })
        NavRow("App lock", if (secrets?.enabled == true) "On for history and settings" else "Off", { go(SettingsRoutes.Lock) })
        if (showBackup) NavRow("Backup", "Export, import, Google Drive", { go(SettingsRoutes.Backup) })
        if (showStagira) NavRow("Stagira", if (settings.stagira) "Offer to record urges" else "Off", { go(SettingsRoutes.Stagira) })
        NavRow("About and disclaimer", "Kairos is a self-help tool, not medical care", { go(SettingsRoutes.About) })
    }
}

private fun ThemeMode.label() = when (this) { ThemeMode.System -> "Follows the system"; ThemeMode.Light -> "Light"; ThemeMode.Dark -> "Dark" }
private fun MotionPref.label() = when (this) { MotionPref.System -> "system motion"; MotionPref.Reduced -> "reduced motion"; MotionPref.Full -> "full motion" }

@Composable
fun CardsScreen(vm: SettingsViewModel, onBack: () -> Unit) {
    val cards by vm.cards.collectAsStateWithLifecycle()
    var draft by rememberSaveable { mutableStateOf("") }
    var editing by rememberSaveable { mutableStateOf<Long?>(null) }
    var editText by rememberSaveable { mutableStateOf("") }
    SettingsPage("Your words", onBack) {
        Body("These cards are shown during panic help. Write them now, in your own voice, for the you who will read them later.")
        cards.forEachIndexed { i, card ->
            PaperCard {
                if (editing == card.id) {
                    OutlinedTextField(editText, { editText = it }, Modifier.fillMaxWidth(), label = { Text("Card") })
                    Row {
                        SmallAction("Save", { vm.editCard(card, editText); editing = null })
                        SmallAction("Cancel", { editing = null })
                    }
                } else {
                    Text(card.text, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
                    Row {
                        SmallAction("Edit", { editing = card.id; editText = card.text }, "Edit card ${i + 1}")
                        if (i > 0) SmallAction("Up", { vm.moveCard(card.id, -1) }, "Move card ${i + 1} up")
                        if (i < cards.lastIndex) SmallAction("Down", { vm.moveCard(card.id, 1) }, "Move card ${i + 1} down")
                        SmallAction("Remove", { vm.deleteCard(card.id) }, "Remove card ${i + 1}")
                    }
                }
            }
        }
        if (cards.isEmpty()) Body("With no cards of your own, Kairos shows its built-in ones.")
        SectionTitle("Add a card")
        OutlinedTextField(draft, { draft = it }, Modifier.fillMaxWidth(), label = { Text("Something true and kind") }, minLines = 2)
        PrimaryButton("Add card", { vm.addCard(draft); draft = "" })
        QuietButton("Restore the built-in cards", vm::restoreCards)
    }
}

/** Picks one phone number from Contacts. The temporary grant means no READ_CONTACTS permission. */
private class PickPhone : ActivityResultContract<Unit, Uri?>() {
    override fun createIntent(context: Context, input: Unit) =
        Intent(Intent.ACTION_PICK).setType(ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE)
    override fun parseResult(resultCode: Int, intent: Intent?): Uri? = intent?.data
}

private fun readPhone(context: Context, uri: Uri): Pair<String, String>? = runCatching {
    context.contentResolver.query(
        uri,
        arrayOf(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER),
        null, null, null,
    )?.use { c -> if (c.moveToFirst()) (c.getString(0) ?: "") to (c.getString(1) ?: "") else null }
}.getOrNull()

@Composable
fun ContactsScreen(vm: SettingsViewModel, onBack: () -> Unit) {
    val contacts by vm.contacts.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var name by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    val pick = rememberLauncherForActivityResult(PickPhone()) { uri ->
        uri?.let { readPhone(context, it) }?.let { (n, p) -> vm.addContact(n, p) }
    }
    SettingsPage("People to call", onBack) {
        Body("People you trust to answer. During panic help, each one is a single large button that opens your dialer.")
        contacts.forEachIndexed { i, c ->
            PaperCard {
                Text(c.name, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                Text(c.phone, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row {
                    if (i > 0) SmallAction("Up", { vm.moveContact(c.id, -1) }, "Move ${c.name} up")
                    if (i < contacts.lastIndex) SmallAction("Down", { vm.moveContact(c.id, 1) }, "Move ${c.name} down")
                    SmallAction("Remove", { vm.deleteContact(c.id) }, "Remove ${c.name}")
                }
            }
        }
        PrimaryButton("Choose from contacts", { vm.lock.expectReturn(); pick.launch(Unit) })
        SectionTitle("Or type one in")
        OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text("Name") }, singleLine = true)
        OutlinedTextField(
            phone, { phone = it }, Modifier.fillMaxWidth(), label = { Text("Phone number") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
        )
        QuietButton("Add", { vm.addContact(name, phone); name = ""; phone = "" })
    }
}

@Composable
fun CrisisScreen(vm: SettingsViewModel, onBack: () -> Unit) {
    val s by vm.settings.collectAsStateWithLifecycle()
    val crisis by vm.crisis.collectAsStateWithLifecycle()
    val settings = s ?: return
    var region by rememberSaveable(settings.regionOverride) { mutableStateOf(settings.regionOverride) }
    var lineName by rememberSaveable(settings.crisisName) { mutableStateOf(settings.crisisName) }
    var number by rememberSaveable(settings.crisisNumber) { mutableStateOf(settings.crisisNumber) }
    val builtIn = CrisisLines.resolve(region.ifBlank { vm.detectedRegion })
    SettingsPage("Support line", onBack) {
        Body("A free support line shown on the Call page, next to the people you trust.")
        PaperCard {
            Text("Now showing", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(
                crisis?.line?.let { "${it.name}\n${it.number}" } ?: "No support line for your region yet.",
                style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                "Emergency services: ${crisis?.emergencyNumber ?: CrisisLines.FALLBACK_EMERGENCY}" +
                    (crisis?.region?.let { " · region $it" } ?: ""),
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Body("Built-in numbers can change. Please check the line works where you live, or add your own.")
        SectionTitle("Region")
        OutlinedTextField(
            region, { region = it.filter { c -> c.isLetter() }.take(2).uppercase() }, Modifier.fillMaxWidth(),
            label = { Text("Two-letter country code") },
            supportingText = { Text("Leave empty to use your phone's region (${vm.detectedRegion ?: "unknown"}).") },
            singleLine = true,
        )
        SectionTitle("Your own line")
        Body("Optional. This replaces the built-in line${builtIn.line?.let { " (${it.name})" } ?: ""}.")
        OutlinedTextField(lineName, { lineName = it }, Modifier.fillMaxWidth(), label = { Text("Name") }, singleLine = true)
        OutlinedTextField(
            number, { number = it }, Modifier.fillMaxWidth(), label = { Text("Number") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
        )
        PrimaryButton("Save", { vm.update { it.copy(regionOverride = region, crisisName = lineName.trim(), crisisNumber = number.trim()) } })
        QuietButton("Use the built-in line", {
            lineName = ""; number = ""
            vm.update { it.copy(crisisName = "", crisisNumber = "") }
        })
    }
}

@Composable
fun BreathingSettingsScreen(vm: SettingsViewModel, onBack: () -> Unit) {
    val s by vm.settings.collectAsStateWithLifecycle()
    val settings = s ?: return
    SettingsPage("Breathing", onBack) {
        Body("The out-breath is always longer than the in-breath, and there is never a hold.")
        Stepper("Breathe in", settings.inhaleSeconds, { v ->
            val p = BreathPattern.of(v, settings.exhaleSeconds)
            vm.update { it.copy(inhaleSeconds = p.inhaleSeconds, exhaleSeconds = p.exhaleSeconds) }
        })
        Stepper("Breathe out", settings.exhaleSeconds, { v ->
            val p = BreathPattern.of(settings.inhaleSeconds, v.coerceAtLeast(settings.inhaleSeconds + 1))
            vm.update { it.copy(inhaleSeconds = p.inhaleSeconds, exhaleSeconds = p.exhaleSeconds) }
        })
        QuietButton("Back to 4 in, 6 out", {
            vm.update { it.copy(inhaleSeconds = BreathPattern.Default.inhaleSeconds, exhaleSeconds = BreathPattern.Default.exhaleSeconds) }
        })
        SwitchRow("Gentle taps", "A soft vibration as each breath turns.", settings.haptics) { on -> vm.update { it.copy(haptics = on) } }
    }
}

@Composable
fun UrgeSettingsScreen(vm: SettingsViewModel, onBack: () -> Unit) {
    val s by vm.settings.collectAsStateWithLifecycle()
    val settings = s ?: return
    var draft by rememberSaveable { mutableStateOf("") }
    SettingsPage("Urge mode", onBack) {
        SectionTitle("Usual length")
        ChoiceRow(UrgeTimer.CHOICES_MINUTES.map { it to "$it minutes" }, settings.urgeMinutes) { m -> vm.update { it.copy(urgeMinutes = m) } }
        SectionTitle("Things to do while you wait")
        settings.distractions.forEach { d ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(d, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onBackground)
                SmallAction("Remove", { vm.update { it.copy(distractions = it.distractions - d) } }, "Remove $d")
            }
        }
        OutlinedTextField(draft, { draft = it }, Modifier.fillMaxWidth(), label = { Text("Something that helps you") }, singleLine = true)
        PrimaryButton("Add", {
            val t = draft.trim()
            if (t.isNotEmpty()) vm.update { it.copy(distractions = (it.distractions + t).distinct()) }
            draft = ""
        })
        QuietButton("Restore the built-in list", { vm.update { it.copy(distractions = Defaults.distractions) } })
    }
}

@Composable
fun LookScreen(vm: SettingsViewModel, onBack: () -> Unit) {
    val s by vm.settings.collectAsStateWithLifecycle()
    val settings = s ?: return
    SettingsPage("Appearance and motion", onBack) {
        SectionTitle("Theme")
        ChoiceRow(listOf(ThemeMode.System to "System", ThemeMode.Light to "Light", ThemeMode.Dark to "Dark"), settings.theme) { t -> vm.update { it.copy(theme = t) } }
        SectionTitle("Motion")
        Body("Reduced motion keeps the breathing circle still and changes only the words.")
        ChoiceRow(listOf(MotionPref.System to "System", MotionPref.Reduced to "Reduced", MotionPref.Full to "Full"), settings.motion) { m -> vm.update { it.copy(motion = m) } }
        Body("Text size follows your phone's font size setting. Kairos is built to stay readable at the largest sizes.")
    }
}

@Composable
fun AccessScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    SettingsPage("Getting here fast", onBack) {
        SectionTitle("Quick Settings tile")
        Body("Swipe down and tap Kairos. It opens the mode you used last, and panic help opens even from the lock screen.")
        if (Build.VERSION.SDK_INT >= 33) {
            PrimaryButton("Add the tile", {
                val sbm = context.getSystemService(StatusBarManager::class.java)
                sbm?.requestAddTileService(
                    ComponentName(context, KairosTileService::class.java),
                    context.getString(R.string.tile_label),
                    Icon.createWithResource(context, R.drawable.ic_tile),
                    context.mainExecutor,
                ) { }
            })
        } else {
            Body("Open Quick Settings, tap the pencil to edit, and drag Kairos into your tiles.")
        }
        SectionTitle("Home-screen widget")
        Body("Two buttons: an urge, or panic help. Each is one tap.")
        val awm = AppWidgetManager.getInstance(context)
        if (awm.isRequestPinAppWidgetSupported) {
            QuietButton("Add the widget", { awm.requestPinAppWidget(ComponentName(context, KairosWidgetReceiver::class.java), null, null) })
        } else {
            Body("Long-press your home screen, choose Widgets, and find Kairos.")
        }
        SectionTitle("Lock screen")
        Body(
            "Android doesn't let apps add their own lock-screen shortcuts, but the Quick Settings tile works while your phone is locked. " +
                "Some phones also let you pick lock-screen shortcuts in their settings; choose Kairos there if you can.",
        )
        SectionTitle("App shortcuts")
        Body("Long-press the Kairos icon for panic help, or a 10 or 20 minute urge timer.")
    }
}

@Composable
fun LockScreen(vm: SettingsViewModel, onBack: () -> Unit) {
    val secrets by vm.secrets.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var pin by rememberSaveable { mutableStateOf("") }
    var confirm by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf<String?>(null) }
    val enabled = secrets?.enabled == true
    val bioAvailable = AppLock.biometricAvailable(context)
    SettingsPage("App lock", onBack) {
        Body("Locks your history, settings and backups behind a PIN" + (if (bioAvailable) " or your fingerprint or face" else "") + ".")
        Body("Panic help and urge mode are never locked. They always open straight away, even from the lock screen.", muted = false)
        if (enabled) {
            if (bioAvailable) SwitchRow("Unlock with fingerprint or face", "", secrets?.biometric == true) { vm.setBiometric(it) }
            QuietButton("Turn app lock off", vm::lockOff)
            SectionTitle("Change PIN")
        } else {
            SectionTitle("Choose a PIN")
        }
        OutlinedTextField(
            pin, { v -> pin = v.filter { it.isDigit() }.take(8) }, Modifier.fillMaxWidth(), label = { Text("4 to 8 digits") },
            singleLine = true, visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        )
        OutlinedTextField(
            confirm, { v -> confirm = v.filter { it.isDigit() }.take(8) }, Modifier.fillMaxWidth(), label = { Text("Same PIN again") },
            singleLine = true, visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        )
        note?.let { Body(it) }
        PrimaryButton(if (enabled) "Save new PIN" else "Turn on app lock", {
            note = when {
                !PinHasher.isValidPin(pin) -> "A PIN is 4 to 8 digits."
                pin != confirm -> "Those two PINs don't match yet."
                else -> { vm.setPin(pin, secrets?.biometric ?: true); pin = ""; confirm = ""; "Saved." }
            }
        })
    }
}

@Composable
fun AboutScreen(onBack: () -> Unit) {
    SettingsPage("About Kairos", onBack) {
        PaperCard {
            Text("Please read", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
            Text(Defaults.disclaimer, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
        }
        Body("Kairos is free. It has no ads, no accounts and no analytics. Everything stays on this phone unless you turn on the encrypted Google Drive backup, which goes only to your own Drive.")
        Body("Kairos (καιρός) is the Greek word for the right moment: the moment to pause, breathe, and let a wave pass.")
        SectionTitle("Licences")
        Body("EB Garamond and Inter are used under the SIL Open Font License 1.1.")
        Body("Version ${BuildConfig.VERSION_NAME}")
    }
}

@Composable
fun StagiraSettingsScreen(vm: SettingsViewModel, onBack: () -> Unit) {
    val s by vm.settings.collectAsStateWithLifecycle()
    val settings: Settings = s ?: return
    SettingsPage("Stagira", onBack) {
        Body("Stagira is a habit ledger. When an urge passes or eases, Kairos can offer to note it there as an urge resisted.")
        Body("Nothing is sent anywhere: Kairos opens Stagira on this phone, and Stagira asks you to confirm which goal it belongs to.")
        SwitchRow("Offer to record in Stagira", "", settings.stagira) { on -> vm.update { it.copy(stagira = on) } }
    }
}

