package app.kairos.ui

import android.os.Looper
import androidx.compose.ui.test.junit4.ComposeTestRule
import app.kairos.data.LockSecrets
import app.kairos.data.Prefs
import app.kairos.lock.PinHasher
import kotlinx.coroutines.runBlocking
import org.robolectric.Shadows.shadowOf

const val TEST_PIN = "2468"

/**
 * Lets coroutines on real dispatchers (Room, DataStore) finish and recompose, without
 * letting the test clock run free: the breathing guide animates forever, so the
 * Compose clock only moves when a test moves it.
 */
fun ComposeTestRule.settle(rounds: Int = 6) {
    repeat(rounds) {
        Thread.sleep(25)
        shadowOf(Looper.getMainLooper()).idle()
        mainClock.advanceTimeBy(50)
    }
}

fun Prefs.lockWithPin() = runBlocking {
    val h = PinHasher.hash(TEST_PIN)
    setLock(LockSecrets(enabled = true, biometric = false, pinHash = h.hash, pinSalt = h.salt))
}

fun Prefs.skipOnboarding() = runBlocking { update { it.copy(onboardingDone = true) } }
