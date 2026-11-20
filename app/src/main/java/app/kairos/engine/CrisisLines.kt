package app.kairos.engine

/** A support line the user can call, e.g. "988 Suicide & Crisis Lifeline". */
data class CrisisLine(val name: String, val number: String)

/** What the Call screen shows: a support line (if known) and the local emergency number. */
data class CrisisContacts(
    val region: String?,
    val line: CrisisLine?,
    val emergencyNumber: String,
    /** False when we don't know the region and the user hasn't set a line yet. */
    val known: Boolean,
    val isOverride: Boolean,
)

/**
 * A small built-in table of free support lines and emergency numbers by ISO
 * 3166 region. The user can always replace the line with their own in Settings,
 * and that override always wins.
 *
 * VERIFY BEFORE EACH RELEASE: numbers change. Each entry should be checked against
 * the service's own website. Unknown regions fall back to 112, which reaches
 * emergency services from most mobile phones, and ask the user to set a line.
 */
object CrisisLines {
    data class Region(val emergency: String, val line: CrisisLine?)

    val table: Map<String, Region> = mapOf(
        "US" to Region("911", CrisisLine("988 Suicide & Crisis Lifeline", "988")),
        "CA" to Region("911", CrisisLine("9-8-8 Suicide Crisis Helpline", "988")),
        "GB" to Region("999", CrisisLine("Samaritans", "116 123")),
        "IE" to Region("112", CrisisLine("Samaritans", "116 123")),
        "AU" to Region("000", CrisisLine("Lifeline", "13 11 14")),
        "NZ" to Region("111", CrisisLine("Need to talk? 1737", "1737")),
        "DE" to Region("112", CrisisLine("TelefonSeelsorge", "0800 111 0 111")),
        "AT" to Region("112", CrisisLine("Telefonseelsorge", "142")),
        "CH" to Region("112", CrisisLine("Die Dargebotene Hand", "143")),
        "FR" to Region("112", CrisisLine("3114 Prévention du suicide", "3114")),
        "BE" to Region("112", CrisisLine("Zelfmoordlijn 1813", "1813")),
        "NL" to Region("112", CrisisLine("113 Zelfmoordpreventie", "113")),
        "ES" to Region("112", CrisisLine("Línea 024", "024")),
        "PT" to Region("112", CrisisLine("SOS Voz Amiga", "213 544 545")),
        "IT" to Region("112", CrisisLine("Telefono Amico", "02 2327 2327")),
        "SE" to Region("112", CrisisLine("Självmordslinjen", "90101")),
        "NO" to Region("112", CrisisLine("Mental Helse", "116 123")),
        "DK" to Region("112", CrisisLine("Livslinien", "70 201 201")),
        "FI" to Region("112", CrisisLine("MIELI Kriisipuhelin", "09 2525 0111")),
        "IN" to Region("112", CrisisLine("Tele-MANAS", "14416")),
        "ZA" to Region("112", CrisisLine("SADAG", "0800 567 567")),
    )

    const val FALLBACK_EMERGENCY = "112"

    /**
     * @param region ISO code from the SIM/network or locale, or a user-chosen one.
     * @param override the user's own line, which always wins.
     */
    fun resolve(region: String?, override: CrisisLine? = null): CrisisContacts {
        val code = region?.trim()?.uppercase()?.takeIf { it.length == 2 }
        val entry = code?.let { table[it] }
        val emergency = entry?.emergency ?: FALLBACK_EMERGENCY
        val custom = override?.takeIf { it.number.isNotBlank() }
        val line = custom ?: entry?.line
        return CrisisContacts(
            region = code,
            line = line,
            emergencyNumber = emergency,
            known = line != null,
            isOverride = custom != null,
        )
    }

    /** Digits and a leading + only, for a tel: URI. */
    fun dialable(number: String): String {
        val trimmed = number.trim()
        val digits = trimmed.filter { it.isDigit() }
        return if (trimmed.startsWith("+")) "+$digits" else digits
    }
}
