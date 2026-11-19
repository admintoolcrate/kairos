package app.kairos.ui

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import app.kairos.data.Prefs
import app.kairos.panic.PanicActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import javax.inject.Inject

/** The promises panic help makes, checked on the real activity. */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = HiltTestApplication::class, qualifiers = "w393dp-h852dp-xxhdpi")
class PanicSafetyTest {
    @get:Rule(order = 0) val hilt = HiltAndroidRule(this)
    @get:Rule(order = 1) val compose = createEmptyComposeRule()

    @Inject lateinit var prefs: Prefs

    @Before fun setUp() {
        hilt.inject()
        compose.mainClock.autoAdvance = false
    }

    private fun launch(): ActivityScenario<PanicActivity> =
        ActivityScenario.launch<PanicActivity>(PanicActivity.intent(ApplicationProvider.getApplicationContext()))
            .also { compose.settle() }

    @Test fun `panic opens straight to breathing even with app lock on`() {
        prefs.lockWithPin()
        launch()
        compose.onNodeWithText("Breathe with the circle").assertExists()
        compose.onAllNodesWithText("Locked").assertCountEquals(0)
        compose.onAllNodesWithText("PIN").assertCountEquals(0)
    }

    @Test fun `breathing only ever says in or out, never hold`() {
        launch()
        val seen = mutableSetOf<String>()
        repeat(120) { // 30 seconds: three full 4-in, 6-out breaths
            compose.mainClock.advanceTimeBy(250)
            listOf("Breathe in", "Breathe out").forEach { w ->
                if (compose.onAllNodesWithText(w).fetchSemanticsNodes().isNotEmpty()) seen += w
            }
            compose.onAllNodes(hasText("Hold", substring = true)).assertCountEquals(0)
        }
        assertEquals(setOf("Breathe in", "Breathe out"), seen)
        compose.onNodeWithText("In for 4, out for 6. No need to hold.").assertExists()
    }

    @Test fun `grounding walks all fifteen prompts, one per screen`() {
        launch()
        compose.onNodeWithText("Ground").performClick()
        compose.settle()
        compose.onNodeWithText("Name something you can see.").assertExists()
        compose.onNodeWithText("1 of 5, things you can see").assertExists()
        repeat(15) {
            compose.onNodeWithText("Next").performClick()
            compose.settle(2)
        }
        compose.onNodeWithText("You noticed what's around you. You are here, right now.").assertExists()
    }

    @Test fun `call page always offers emergency services and reassurance cards are there`() {
        launch()
        compose.onNodeWithText("Call").performClick()
        compose.settle()
        compose.onNode(hasText("Emergency services", substring = true)).assertExists()
        compose.onNodeWithText("If you have chest pain or trouble breathing, call emergency services.").assertExists()
        compose.onNodeWithText("Words").performClick()
        compose.settle()
        compose.onNodeWithText("This is a panic attack. It peaks and passes. You are safe.").assertExists()
    }

    @Test fun `check-in afterwards can be skipped and then panic help closes`() {
        var activity: PanicActivity? = null
        launch().onActivity { activity = it }
        compose.onNodeWithText("I'm okay now").performClick()
        compose.settle()
        compose.onNodeWithText("How strong does it feel now?").assertExists()
        compose.onNodeWithText("Skip").performClick()
        compose.settle()
        assertTrue(activity!!.isFinishing)
    }
}
