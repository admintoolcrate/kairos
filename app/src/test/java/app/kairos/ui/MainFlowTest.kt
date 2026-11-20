package app.kairos.ui

import android.content.Intent
import android.os.Looper
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import app.kairos.MainActivity
import app.kairos.data.Prefs
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Duration
import javax.inject.Inject

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = HiltTestApplication::class, qualifiers = "w393dp-h852dp-xxhdpi")
class MainFlowTest {
    @get:Rule(order = 0) val hilt = HiltAndroidRule(this)
    @get:Rule(order = 1) val compose = createEmptyComposeRule()

    @Inject lateinit var prefs: Prefs

    @Before fun setUp() {
        hilt.inject()
        compose.mainClock.autoAdvance = false
    }

    private fun launch() {
        ActivityScenario.launch<MainActivity>(Intent(ApplicationProvider.getApplicationContext(), MainActivity::class.java))
        compose.settle()
    }

    @Test fun `first launch shows the disclaimer, which can't be skipped`() {
        launch()
        compose.onNodeWithText("Next").performClick()
        compose.settle()
        compose.onNodeWithText("Before you start").assertExists()
        compose.onNode(hasText("isn't medical care", substring = true)).assertExists()
        compose.onNode(hasText("chest pain, trouble breathing", substring = true)).assertExists()
        compose.onNode(hasText("call your local emergency number", substring = true)).assertExists()
        compose.onAllNodesWithText("Skip for now").assertCountEquals(0)
        compose.onNodeWithText("I understand").performClick()
        compose.settle()
        compose.onNodeWithText("Make it yours").assertExists()
    }

    @Test fun `app lock guards settings but never home or urge mode`() {
        prefs.skipOnboarding()
        prefs.lockWithPin()
        launch()
        compose.onNodeWithText("What's here right now?").assertExists()
        compose.onAllNodesWithText("Locked").assertCountEquals(0)

        compose.onNodeWithText("An urge").performClick()
        compose.settle()
        compose.onNodeWithText("Ride it out").assertExists()
        compose.onAllNodesWithText("Locked").assertCountEquals(0)
    }

    @Test fun `settings ask for the pin, gently, and open with the right one`() {
        prefs.skipOnboarding()
        prefs.lockWithPin()
        launch()
        compose.onNodeWithText("Settings").performClick()
        compose.settle()
        compose.onNodeWithText("Locked").assertExists()
        compose.onNode(hasSetTextAction()).performTextInput("1111")
        compose.onNodeWithText("Unlock").performClick()
        compose.settle()
        compose.onNodeWithText("That PIN didn't match. Try again.").assertExists()
        compose.onNode(hasSetTextAction()).performTextInput(TEST_PIN)
        compose.onNodeWithText("Unlock").performClick()
        compose.settle()
        compose.onNodeWithText("Your words").assertExists()
    }

    @Test fun `an urge timer runs out quietly and asks, without requiring, how it went`() {
        prefs.skipOnboarding()
        launch()
        compose.onNodeWithText("An urge").performClick()
        compose.settle()
        compose.onNodeWithText("Start").performClick()
        compose.settle()
        compose.onNodeWithText("Riding it out").assertExists()
        compose.onNodeWithText("10:00").assertExists()

        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMinutes(10))
        compose.settle()
        compose.onNodeWithText("You gave it time").assertExists()
        compose.onNodeWithText("The time is up. How is the urge now?").assertExists()
        compose.onNodeWithText("Five more minutes").assertExists()

        compose.onNodeWithText("How did it go?").performClick()
        compose.settle()
        compose.onNodeWithText("Did the urge pass?").assertExists()
        compose.onNodeWithText("It passed").performClick()
        compose.settle()
        compose.onNodeWithText("How strong does it feel now?").assertExists()
        compose.onNodeWithText("Skip").performClick()
        compose.settle()
        compose.onNodeWithText("It passed.").assertExists()
    }
}
