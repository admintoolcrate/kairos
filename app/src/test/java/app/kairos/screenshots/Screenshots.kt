package app.kairos.screenshots

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import app.kairos.MainActivity
import app.kairos.data.ContactRepository
import app.kairos.data.Prefs
import app.kairos.panic.PanicActivity
import app.kairos.ui.settle
import app.kairos.ui.skipOnboarding
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import javax.inject.Inject

/**
 * Real screens drawn to app/build/screenshots for reviewing the design.
 * Only runs with: ./gradlew testDebugUnitTest -Pscreenshots=true
 */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], application = HiltTestApplication::class, qualifiers = "w393dp-h852dp-xxhdpi")
class Screenshots {
    @get:Rule(order = 0) val hilt = HiltAndroidRule(this)
    @get:Rule(order = 1) val compose = createEmptyComposeRule()

    @Inject lateinit var prefs: Prefs
    @Inject lateinit var contacts: ContactRepository

    @Before fun setUp() {
        hilt.inject()
        compose.mainClock.autoAdvance = false
    }

    private fun <A : Activity> shoot(scenario: ActivityScenario<A>, name: String) {
        compose.settle()
        scenario.onActivity { a ->
            val v = a.window.decorView
            val bmp = Bitmap.createBitmap(v.width, v.height, Bitmap.Config.ARGB_8888)
            v.draw(Canvas(bmp))
            val dir = File("build/screenshots").apply { mkdirs() }
            File(dir, "$name.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }

    private fun main() = ActivityScenario.launch<MainActivity>(Intent(ApplicationProvider.getApplicationContext(), MainActivity::class.java)).also { compose.settle() }
    private fun panic() = ActivityScenario.launch<PanicActivity>(PanicActivity.intent(ApplicationProvider.getApplicationContext()))
    private fun tap(text: String) { compose.onNodeWithText(text).performClick(); compose.settle() }
    private fun withContacts() = runBlocking { contacts.add("Sam", "+44 7700 900123"); contacts.add("Mum", "+44 7700 900456") }

    private fun panicSet(prefix: String) {
        withContacts()
        val s = panic()
        compose.mainClock.advanceTimeBy(2_000)
        shoot(s, "$prefix-1-breathe")
        tap("Ground"); shoot(s, "$prefix-2-ground")
        tap("Words"); shoot(s, "$prefix-3-words")
        tap("Call"); shoot(s, "$prefix-4-call")
        tap("I'm okay now"); shoot(s, "$prefix-5-checkin")
    }

    private fun mainSet(prefix: String) {
        prefs.skipOnboarding()
        val s = main()
        shoot(s, "$prefix-1-home")
        tap("An urge"); shoot(s, "$prefix-2-urge-choose")
        tap("Start"); compose.mainClock.advanceTimeBy(1_000); shoot(s, "$prefix-3-urge-running")
        tap("I'm done"); shoot(s, "$prefix-4-urge-outcome")
    }

    @Test fun light_panic() = panicSet("light-panic")
    @Test fun light_main() = mainSet("light-main")

    @Test fun dark_panic() { RuntimeEnvironment.setQualifiers("+night"); panicSet("dark-panic") }
    @Test fun dark_main() { RuntimeEnvironment.setQualifiers("+night"); mainSet("dark-main") }

    @Test fun large_font_panic() { RuntimeEnvironment.setFontScale(2f); panicSet("big-panic") }
    @Test fun large_font_main() { RuntimeEnvironment.setFontScale(2f); mainSet("big-main") }

    @Test fun onboarding() {
        val s = main()
        shoot(s, "onboarding-1")
        tap("Next"); shoot(s, "onboarding-2-disclaimer")
        tap("I understand"); shoot(s, "onboarding-3-setup")
    }

    @Test fun settings() {
        prefs.skipOnboarding()
        withContacts()
        val s = main()
        tap("Settings"); shoot(s, "settings-1-index")
        tap("Your words"); shoot(s, "settings-2-cards")
    }
}
