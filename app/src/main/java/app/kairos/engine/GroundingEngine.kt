package app.kairos.engine

/** The five senses of the 5-4-3-2-1 exercise, in order, with how many to notice. */
enum class Sense(val count: Int) { See(5), Touch(4), Hear(3), Smell(2), Taste(1) }

/** One prompt, shown alone on its own screen. */
data class GroundingPrompt(
    val sense: Sense,
    /** 1-based: "thing 2 of 5". */
    val item: Int,
    /** 0-based position across all 15 prompts. */
    val index: Int,
) {
    val count: Int get() = sense.count
    val isFirstOfSense: Boolean get() = item == 1
}

/**
 * The 5-4-3-2-1 walkthrough as an immutable value: [next], [back] and
 * [skipSense] return a new walkthrough. One prompt per screen.
 */
data class GroundingWalkthrough(val index: Int = 0) {
    init {
        require(index in 0..TOTAL) { "index out of range: $index" }
    }

    val isComplete: Boolean get() = index == TOTAL
    val isAtStart: Boolean get() = index == 0

    /** The prompt to show, or null once the walkthrough is complete. */
    val current: GroundingPrompt? get() = if (isComplete) null else PROMPTS[index]

    /** 0..1 for a quiet progress line. */
    val progress: Float get() = index.toFloat() / TOTAL

    fun next(): GroundingWalkthrough = if (isComplete) this else copy(index = index + 1)

    fun back(): GroundingWalkthrough = if (isAtStart) this else copy(index = index - 1)

    /** Jump to the first prompt of the next sense, for someone who can't find one more. */
    fun skipSense(): GroundingWalkthrough {
        val prompt = current ?: return this
        return copy(index = prompt.index - (prompt.item - 1) + prompt.count)
    }

    fun restart(): GroundingWalkthrough = GroundingWalkthrough()

    companion object {
        val PROMPTS: List<GroundingPrompt> = buildList {
            Sense.entries.forEach { sense -> (1..sense.count).forEach { add(GroundingPrompt(sense, it, size)) } }
        }
        val TOTAL: Int = PROMPTS.size
    }
}
