package app.kairos.data

/**
 * Built-in wording. Plain, calm, and free of medical claims or promises.
 *
 * Kairos never tells someone what they are experiencing ("this is a panic attack")
 * or that they are safe: it can't know either. The built-in cards are written in the
 * person's own voice, about breathing, noticing and waiting. People are encouraged to
 * replace them with their own words, which may say whatever they choose.
 */
object Defaults {
    val reassuranceCards = listOf(
        "One breath at a time. That's all I need to do right now.",
        "I don't have to fight this feeling. I can let it rise and fall.",
        "Breathe out slowly. Nothing needs to happen right now.",
        "I can feel my feet on the floor and the air on my skin.",
        "Feelings come and go. I can wait and watch this one.",
        "This is hard, and I'm doing my best.",
    )

    val distractions = listOf(
        "Walk around the room",
        "Splash cold water on your face",
        "Text a friend",
        "Name five blue things you can see",
        "Make a cup of tea",
        "Write down what you're feeling",
        "Step outside for some air",
        "Put on a song you love",
    )

    /** Shown one at a time during an urge countdown. */
    val urgeMessages = listOf(
        "Urges rise, crest and fall, like a wave.",
        "You don't have to act on this. You only have to wait.",
        "This feeling is strong. You don't have to do anything with it.",
        "Notice the urge. You don't have to obey it.",
        "One minute at a time is enough.",
        "Waiting is enough. You're doing it right now.",
        "Let it be here. It will move on.",
    )

    const val disclaimer =
        "Kairos is a self-help tool. It isn't medical care, and it can't diagnose or treat anything. " +
            "If you have chest pain, trouble breathing, or think you may be in danger, " +
            "call your local emergency number now."
}
