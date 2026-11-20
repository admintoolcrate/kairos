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
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.RobolectricTestRunner
import java.io.File
import javax.inject.Inject

/**
 * Google Play phone screenshots: 1080 x 1920 (9:16), real screens, light theme.
 * ./gradlew testDebugUnitTest -Pscreenshots=true --tests 'app.kairos.screenshots.StoreScreenshots'
 * → app/build/store-screenshots, then copy to docs/play-store/screenshots.
 */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], application = HiltTestApplication::class, qualifiers = "w360dp-h640dp-xxhdpi")
class StoreScreenshots {
    @get:Rule(order = 0) val hilt = HiltAndroidRule(this)
    @get:Rule(order = 1) val compose = createEmptyComposeRule()

    @Inject lateinit var prefs: Prefs
    @Inject lateinit var contacts: ContactRepository

    @Before fun setUp() {
        hilt.inject()
        compose.mainClock.autoAdvance = false
        prefs.skipOnboarding()
        runBlocking { contacts.add("Sam", "+1 555 0100"); contacts.add("Mum", "+1 555 0101") }
    }

    private fun <A : Activity> shoot(s: ActivityScenario<A>, name: String) {
        compose.settle()
        s.onActivity { a ->
            val v = a.window.decorView
            val bmp = Bitmap.createBitmap(v.width, v.height, Bitmap.Config.ARGB_8888)
            v.draw(Canvas(bmp))
            val dir = File("build/store-screenshots").apply { mkdirs() }
            File(dir, "$name.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }

    private fun tap(text: String) { compose.onNodeWithText(text).performClick(); compose.settle() }

    @Test fun panic() {
        val s = ActivityScenario.launch<PanicActivity>(PanicActivity.intent(ApplicationProvider.getApplicationContext()))
        compose.settle()
        compose.mainClock.advanceTimeBy(3_000) // most of the way through an in-breath
        shoot(s, "1-breathe")
        tap("Ground"); shoot(s, "3-ground")
        tap("Words"); shoot(s, "4-words")
        tap("Call"); shoot(s, "5-call")
    }

    @Test fun main() {
        val s = ActivityScenario.launch<MainActivity>(Intent(ApplicationProvider.getApplicationContext(), MainActivity::class.java))
        compose.settle()
        shoot(s, "0-home")
        tap("An urge"); tap("Start")
        compose.mainClock.advanceTimeBy(1_000)
        shoot(s, "2-urge")
    }
}
