package app.kairos.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import app.kairos.engine.BreathPattern
import app.kairos.engine.CrisisLine
import app.kairos.ui.theme.MotionPref
import app.kairos.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton

/** Everything the user has chosen. Secrets (PIN hash, salt) are kept out of exports. */
@Serializable
data class Settings(
    /** Null until a mode has actually been used. */
    val lastMode: Mode? = null,
    val urgeMinutes: Int = 10,
    val inhaleSeconds: Int = BreathPattern.Default.inhaleSeconds,
    val exhaleSeconds: Int = BreathPattern.Default.exhaleSeconds,
    val haptics: Boolean = true,
    val motion: MotionPref = MotionPref.System,
    val theme: ThemeMode = ThemeMode.System,
    val crisisName: String = "",
    val crisisNumber: String = "",
    val regionOverride: String = "",
    val stagira: Boolean = false,
    val distractions: List<String> = Defaults.distractions,
    val onboardingDone: Boolean = false,
    val driveBackup: Boolean = false,
    val driveLastBackup: Long = 0,
) {
    val breathPattern: BreathPattern get() = BreathPattern.of(inhaleSeconds, exhaleSeconds)
    val crisisOverride: CrisisLine?
        get() = if (crisisNumber.isBlank()) null else CrisisLine(crisisName.ifBlank { "My crisis line" }, crisisNumber)
}

/** App-lock secrets, stored separately and never exported. */
data class LockSecrets(val enabled: Boolean, val biometric: Boolean, val pinHash: String, val pinSalt: String)

@Singleton
class Prefs @Inject constructor(private val store: DataStore<Preferences>) {

    private object K {
        val lastMode = stringPreferencesKey("lastMode")
        val urgeMinutes = intPreferencesKey("urgeMinutes")
        val inhale = intPreferencesKey("inhale")
        val exhale = intPreferencesKey("exhale")
        val haptics = booleanPreferencesKey("haptics")
        val motion = stringPreferencesKey("motion")
        val theme = stringPreferencesKey("theme")
        val crisisName = stringPreferencesKey("crisisName")
        val crisisNumber = stringPreferencesKey("crisisNumber")
        val region = stringPreferencesKey("region")
        val stagira = booleanPreferencesKey("stagira")
        val distractions = stringPreferencesKey("distractions")
        val onboarding = booleanPreferencesKey("onboarding")
        val drive = booleanPreferencesKey("drive")
        val driveLast = longPreferencesKey("driveLast")
        val lockOn = booleanPreferencesKey("lockOn")
        val lockBio = booleanPreferencesKey("lockBio")
        val pinHash = stringPreferencesKey("pinHash")
        val pinSalt = stringPreferencesKey("pinSalt")
    }

    val settings: Flow<Settings> = store.data.map { it.toSettings() }

    val lock: Flow<LockSecrets> = store.data.map {
        LockSecrets(it[K.lockOn] ?: false, it[K.lockBio] ?: true, it[K.pinHash] ?: "", it[K.pinSalt] ?: "")
    }

    suspend fun current(): Settings = settings.first()

    /** The mode actually used last, or null on a fresh install (no default filled in). */
    suspend fun lastUsedMode(): Mode? = current().lastMode

    suspend fun update(change: (Settings) -> Settings) {
        store.edit { p ->
            val s = change(p.toSettings())
            s.lastMode?.let { p[K.lastMode] = it.name }
            p[K.urgeMinutes] = s.urgeMinutes
            val pattern = BreathPattern.of(s.inhaleSeconds, s.exhaleSeconds)
            p[K.inhale] = pattern.inhaleSeconds
            p[K.exhale] = pattern.exhaleSeconds
            p[K.haptics] = s.haptics
            p[K.motion] = s.motion.name
            p[K.theme] = s.theme.name
            p[K.crisisName] = s.crisisName
            p[K.crisisNumber] = s.crisisNumber
            p[K.region] = s.regionOverride
            p[K.stagira] = s.stagira
            p[K.distractions] = s.distractions.joinToString(SEP)
            p[K.onboarding] = s.onboardingDone
            p[K.drive] = s.driveBackup
            p[K.driveLast] = s.driveLastBackup
        }
    }

    suspend fun setLock(secrets: LockSecrets) {
        store.edit {
            it[K.lockOn] = secrets.enabled
            it[K.lockBio] = secrets.biometric
            it[K.pinHash] = secrets.pinHash
            it[K.pinSalt] = secrets.pinSalt
        }
    }

    private fun Preferences.toSettings() = Settings(
        lastMode = this[K.lastMode]?.let { n -> Mode.entries.firstOrNull { it.name == n } },
        urgeMinutes = this[K.urgeMinutes] ?: 10,
        inhaleSeconds = this[K.inhale] ?: BreathPattern.Default.inhaleSeconds,
        exhaleSeconds = this[K.exhale] ?: BreathPattern.Default.exhaleSeconds,
        haptics = this[K.haptics] ?: true,
        motion = enumOr(this[K.motion], MotionPref.System),
        theme = enumOr(this[K.theme], ThemeMode.System),
        crisisName = this[K.crisisName] ?: "",
        crisisNumber = this[K.crisisNumber] ?: "",
        regionOverride = this[K.region] ?: "",
        stagira = this[K.stagira] ?: false,
        distractions = this[K.distractions]?.split(SEP)?.filter { it.isNotBlank() } ?: Defaults.distractions,
        onboardingDone = this[K.onboarding] ?: false,
        driveBackup = this[K.drive] ?: false,
        driveLastBackup = this[K.driveLast] ?: 0,
    )

    private inline fun <reified E : Enum<E>> enumOr(name: String?, default: E): E =
        name?.let { n -> enumValues<E>().firstOrNull { it.name == n } } ?: default

    private companion object {
        const val SEP = "\u001F"
    }
}
