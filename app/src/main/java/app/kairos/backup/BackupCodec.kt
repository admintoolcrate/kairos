package app.kairos.backup

import app.kairos.data.CardEntity
import app.kairos.data.ContactEntity
import app.kairos.data.SessionEntity
import app.kairos.data.Settings
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Everything a person might want to keep. The app-lock PIN is never included. */
@Serializable
data class KairosExport(
    val format: String = FORMAT,
    val version: Int = VERSION,
    val exportedAt: Long,
    val settings: Settings,
    val cards: List<CardEntity>,
    val contacts: List<ContactEntity>,
    val sessions: List<SessionEntity>,
) {
    companion object {
        const val FORMAT = "kairos"
        const val VERSION = 1
    }
}

class BackupFormatException(message: String) : Exception(message)

object BackupCodec {
    val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
    }

    fun encode(export: KairosExport): String = json.encodeToString(KairosExport.serializer(), export)

    fun decode(text: String): KairosExport {
        val export = try {
            json.decodeFromString(KairosExport.serializer(), text)
        } catch (e: Exception) {
            throw BackupFormatException("This doesn't look like a Kairos backup.")
        }
        if (export.format != KairosExport.FORMAT) throw BackupFormatException("This doesn't look like a Kairos backup.")
        if (export.version > KairosExport.VERSION) throw BackupFormatException("This backup is from a newer version of Kairos. Please update first.")
        return export
    }

    /**
     * Merge [incoming] into [current]: keeps everything already here and adds what's new.
     * Sessions match on mode and start time, cards on text, contacts on phone number.
     */
    fun merge(current: KairosExport, incoming: KairosExport): KairosExport {
        val sessionKeys = current.sessions.map { it.mode to it.startedAt }.toSet()
        val cardTexts = current.cards.map { it.text.trim() }.toSet()
        val phones = current.contacts.map { it.phone.filter(Char::isDigit) }.toSet()
        return current.copy(
            sessions = current.sessions + incoming.sessions.filter { (it.mode to it.startedAt) !in sessionKeys },
            cards = current.cards + incoming.cards.filter { it.text.trim() !in cardTexts },
            contacts = current.contacts + incoming.contacts.filter { it.phone.filter(Char::isDigit) !in phones },
        )
    }
}
