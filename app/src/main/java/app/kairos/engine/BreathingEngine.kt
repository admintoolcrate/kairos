package app.kairos.engine

import kotlin.math.PI
import kotlin.math.cos

/**
 * The only two phases. There is deliberately no hold: breath-holding can make
 * panic feel worse, so Kairos never asks for it, not even as an option.
 */
enum class BreathingPhase { Inhale, Exhale }

/**
 * Seconds in and seconds out. The exhale is always longer than the inhale;
 * construct through [of] to clamp user input into the safe range.
 */
data class BreathPattern(val inhaleSeconds: Int, val exhaleSeconds: Int) {
    init {
        require(inhaleSeconds in INHALE_RANGE) { "inhale must be in $INHALE_RANGE, was $inhaleSeconds" }
        require(exhaleSeconds in EXHALE_RANGE) { "exhale must be in $EXHALE_RANGE, was $exhaleSeconds" }
        require(exhaleSeconds > inhaleSeconds) { "exhale ($exhaleSeconds) must be longer than inhale ($inhaleSeconds)" }
    }

    val cycleMillis: Long get() = (inhaleSeconds + exhaleSeconds) * 1000L

    companion object {
        val INHALE_RANGE = 2..8
        val EXHALE_RANGE = 3..12
        val Default = BreathPattern(4, 6)

        /** Clamps any input into a valid pattern, lengthening the exhale if needed. */
        fun of(inhaleSeconds: Int, exhaleSeconds: Int): BreathPattern {
            val inhale = inhaleSeconds.coerceIn(INHALE_RANGE)
            val exhale = exhaleSeconds.coerceIn(EXHALE_RANGE).coerceAtLeast(inhale + 1)
            return BreathPattern(inhale, exhale)
        }
    }
}

/**
 * A moment in the breathing guide.
 * @param phaseProgress 0..1 through the current phase.
 * @param expansion 0..1, how full the visual circle should be (eased, so it never jerks).
 * @param cycle 0-based count of completed breaths.
 * @param secondsLeftInPhase whole seconds remaining, for a gentle "4, 3, 2, 1" if shown.
 */
data class BreathFrame(
    val phase: BreathingPhase,
    val phaseProgress: Float,
    val expansion: Float,
    val cycle: Int,
    val secondsLeftInPhase: Int,
)

/** Pure function of elapsed time: deterministic, easy to test, no Android. */
class BreathingEngine(val pattern: BreathPattern = BreathPattern.Default) {

    private val inhaleMs = pattern.inhaleSeconds * 1000L
    private val exhaleMs = pattern.exhaleSeconds * 1000L

    fun frameAt(elapsedMillis: Long): BreathFrame {
        val t = elapsedMillis.coerceAtLeast(0)
        val cycle = (t / pattern.cycleMillis).toInt()
        val inCycle = t % pattern.cycleMillis
        return if (inCycle < inhaleMs) {
            val p = inCycle.toFloat() / inhaleMs
            BreathFrame(BreathingPhase.Inhale, p, ease(p), cycle, secondsLeft(inhaleMs - inCycle))
        } else {
            val into = inCycle - inhaleMs
            val p = into.toFloat() / exhaleMs
            BreathFrame(BreathingPhase.Exhale, p, 1f - ease(p), cycle, secondsLeft(exhaleMs - into))
        }
    }

    /** True if the phase changed between two times, used for a single soft haptic tick. */
    fun phaseChangedBetween(previousMillis: Long, nowMillis: Long): Boolean {
        if (nowMillis <= previousMillis) return false
        val a = frameAt(previousMillis)
        val b = frameAt(nowMillis)
        return a.phase != b.phase || a.cycle != b.cycle
    }

    /** Milliseconds for [breaths] full breaths, e.g. "a minute of breathing" ≈ 6 breaths at 4/6. */
    fun durationOf(breaths: Int): Long = breaths * pattern.cycleMillis

    private fun secondsLeft(ms: Long) = ((ms + 999) / 1000).toInt()

    /** Sine ease-in-out, so the circle starts and ends each phase softly. */
    private fun ease(p: Float): Float = ((1 - cos(PI * p.coerceIn(0f, 1f))) / 2).toFloat()
}
