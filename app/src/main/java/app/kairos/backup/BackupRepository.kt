package app.kairos.backup

import androidx.room.withTransaction
import app.kairos.data.KairosDatabase
import app.kairos.data.Prefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

enum class ImportMode { Merge, Replace }

@Singleton
class BackupRepository @Inject constructor(private val db: KairosDatabase, private val prefs: Prefs) {

    suspend fun snapshot(now: Long = System.currentTimeMillis()): KairosExport = withContext(Dispatchers.IO) {
        KairosExport(
            exportedAt = now,
            settings = prefs.current(),
            cards = db.cards().all(),
            contacts = db.contacts().all(),
            sessions = db.sessions().all(),
        )
    }

    /** The backup file's text, sealed when a passphrase is given. */
    suspend fun export(passphrase: CharArray?): String = withContext(Dispatchers.Default) {
        Envelope.write(BackupCodec.encode(snapshot()), passphrase)
    }

    /** Throws [PassphraseNeededException], [WrongPassphraseException] or [BackupFormatException]. */
    suspend fun import(text: String, passphrase: CharArray?, mode: ImportMode) = withContext(Dispatchers.Default) {
        val incoming = BackupCodec.decode(Envelope.read(text, passphrase))
        val result = when (mode) {
            ImportMode.Replace -> incoming
            ImportMode.Merge -> BackupCodec.merge(snapshot(), incoming)
        }
        withContext(Dispatchers.IO) {
            db.withTransaction {
                db.sessions().clear()
                db.sessions().insertAll(result.sessions.map { it.copy(id = 0) })
                db.cards().replaceAll(result.cards)
                db.contacts().replaceAll(result.contacts)
            }
            if (mode == ImportMode.Replace) {
                prefs.update { current ->
                    // Keep this phone's own Drive and onboarding state.
                    result.settings.copy(
                        onboardingDone = true,
                        driveBackup = current.driveBackup,
                        driveLastBackup = current.driveLastBackup,
                    )
                }
            }
        }
    }

    suspend fun noteDriveBackup(at: Long) = prefs.update { it.copy(driveBackup = true, driveLastBackup = at) }
}
