package app.kairos.data

import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Built-in wording must not diagnose, label what someone is going through, promise
 * safety or an outcome, or make medical claims. (The person's own cards are theirs.)
 */
class CopyTest {
    private val banned = listOf(
        "panic attack", "attack", "false alarm", "you are safe", "you're safe", "i am safe", "i'm safe",
        "safe enough", "will pass", "it passes", "temporary", "harmless", "can't hurt", "cannot hurt",
        "nothing is wrong", "anxiety", "disorder", "symptom", "heart", "diagnos", "cure", "heal", "therap",
        "clinically", "proven", "!",
    )

    private val builtIn = Defaults.reassuranceCards + Defaults.urgeMessages + Defaults.distractions

    @Test fun `built-in copy makes no diagnosis, promise or medical claim`() {
        builtIn.forEach { line ->
            val l = line.lowercase()
            banned.forEach { word -> assertTrue("\"$line\" contains \"$word\"", word !in l) }
        }
    }

    @Test fun `the disclaimer still points to emergency services`() {
        val d = Defaults.disclaimer.lowercase()
        listOf("self-help", "isn't medical care", "chest pain", "trouble breathing", "emergency").forEach {
            assertTrue("disclaimer should mention \"$it\"", it in d)
        }
    }
}
