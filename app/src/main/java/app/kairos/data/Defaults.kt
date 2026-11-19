package app.kairos.data

/**
 * Built-in wording. Plain, calm, and free of medical claims or promises.
 * Users are encouraged to replace the cards with their own words.
 */
object Defaults {
    val reassuranceCards = listOf(
        "This is a panic attack. It peaks and passes. You are safe.",
        "I have felt this before, and it passed.",
        "My body is sounding a false alarm. I can let it ring itself out.",
        "I don't have to fight this feeling. I can let it rise and fall.",
        "Breathe out slowly. Nothing needs to happen right now.",
        "I am safe enough in this moment.",
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
        "This feeling is strong, and it is temporary.",
        "Notice the urge. You don't have to obey it.",
        "You've ridden these out before.",
        "Waiting is enough. You're doing it right now.",
        "Let it be here. It will move on.",
    )

    const val disclaimer =
        "Kairos is a self-help tool. It isn't medical care, and it can't diagnose or treat anything. " +
            "If you have chest pain, trouble breathing, or think you may be in danger, " +
            "call your local emergency number now."
}
