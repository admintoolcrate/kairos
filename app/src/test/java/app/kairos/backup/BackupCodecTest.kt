package app.kairos.backup

import app.kairos.data.CardEntity
import app.kairos.data.ContactEntity
import app.kairos.data.Mode
import app.kairos.data.Outcome
import app.kairos.data.SessionEntity
import app.kairos.data.Settings
import app.kairos.ui.theme.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class BackupCodecTest {
    private val sample = KairosExport(
        exportedAt = 1_700_000_000_000,
        settings = Settings(urgeMinutes = 20, inhaleSeconds = 5, exhaleSeconds = 8, theme = ThemeMode.Dark, crisisNumber = "123"),
        cards = listOf(CardEntity(1, "I am safe.", 0), CardEntity(2, "It passes.", 1)),
        contacts = listOf(ContactEntity(1, "Sam", "+44 7700 900000", 0)),
        sessions = listOf(
            SessionEntity(1, Mode.Urge, 1000, 2000, 10, Outcome.Passed, 2),
            SessionEntity(2, Mode.Panic, 3000, null),
        ),
    )

    @Test fun `round trip keeps everything`() {
        val text = BackupCodec.encode(sample)
        assertEquals(sample, BackupCodec.decode(text))
        assertTrue(text.contains("\"format\": \"kairos\""))
    }

    @Test fun `export never contains a pin`() {
        val text = BackupCodec.encode(sample).lowercase()
        assertFalse(text.contains("pin"))
    }

    @Test fun `unknown keys are ignored for forward compatibility`() {
        val text = BackupCodec.encode(sample).replaceFirst("{", "{\"somethingNew\": 1,")
        assertEquals(sample, BackupCodec.decode(text))
    }

    @Test fun `rejects other files and newer versions`() {
        listOf("{}", "not json", """{"format":"other","version":1}""").forEach {
            try { BackupCodec.decode(it); fail("accepted $it") } catch (_: BackupFormatException) {}
        }
        val newer = BackupCodec.encode(sample.copy(version = 99))
        try { BackupCodec.decode(newer); fail() } catch (e: BackupFormatException) { assertTrue(e.message!!.contains("newer")) }
    }

    @Test fun `merge adds only what is new`() {
        val incoming = sample.copy(
            cards = listOf(CardEntity(9, "I am safe.", 0), CardEntity(10, "New card", 1)),
            contacts = listOf(ContactEntity(5, "Sam again", "+447700900000", 0), ContactEntity(6, "Alex", "555", 1)),
            sessions = sample.sessions + SessionEntity(3, Mode.Urge, 5000, 6000),
        )
        val merged = BackupCodec.merge(sample, incoming)
        assertEquals(listOf("I am safe.", "It passes.", "New card"), merged.cards.map { it.text })
        assertEquals(listOf("Sam", "Alex"), merged.contacts.map { it.name })
        assertEquals(3, merged.sessions.size)
        assertEquals(sample.settings, merged.settings)
    }
}
