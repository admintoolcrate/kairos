package app.kairos.engine

/**
 * A pausable countdown for riding out an urge. Pure and immutable; time comes in
 * as a monotonic clock value (e.g. SystemClock.elapsedRealtime) so it survives
 * process death if the start is saved.
 *
 * When it reaches zero it simply arrives: there is no "failed" or "overtime" state.
 */
data class UrgeTimer(
    val durationMillis: Long,
    val startedAt: Long,
    val pausedAt: Long? = null,
    val pausedTotal: Long = 0,
) {
    init {
        require(durationMillis > 0) { "duration must be positive" }
    }

    val isPaused: Boolean get() = pausedAt != null

    fun elapsed(now: Long): Long {
        val end = pausedAt ?: now
        return (end - startedAt - pausedTotal).coerceIn(0, durationMillis)
    }

    fun remaining(now: Long): Long = durationMillis - elapsed(now)

    fun isFinished(now: Long): Boolean = remaining(now) == 0L

    /** 0..1, how much of the time has passed. */
    fun progress(now: Long): Float = elapsed(now).toFloat() / durationMillis

    fun pause(now: Long): UrgeTimer = if (isPaused || isFinished(now)) this else copy(pausedAt = now)

    fun resume(now: Long): UrgeTimer {
        val at = pausedAt ?: return this
        return copy(pausedAt = null, pausedTotal = pausedTotal + (now - at).coerceAtLeast(0))
    }

    /** Add more time, e.g. "five more minutes". Works after finishing too. */
    fun extend(millis: Long): UrgeTimer = copy(durationMillis = durationMillis + millis.coerceAtLeast(0))

    companion object {
        val CHOICES_MINUTES = listOf(10, 20)
        const val EXTEND_MINUTES = 5

        fun start(minutes: Int, now: Long) = UrgeTimer(minutes * 60_000L, now)

        /** "9:41" style, rounding up so it never shows 0:00 while time is left. */
        fun format(millis: Long): String {
            val totalSeconds = (millis.coerceAtLeast(0) + 999) / 1000
            return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
        }
    }
}
