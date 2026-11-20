package app.kairos.panic

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.kairos.engine.CrisisLines
import app.kairos.ui.theme.KairosTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Panic help. Separate from MainActivity so that:
 *  - it can show over the lock screen,
 *  - app lock can never stand in front of it (it never checks the lock),
 *  - it opens straight to slow breathing with nothing to choose first.
 */
@AndroidEntryPoint
class PanicActivity : ComponentActivity() {
    private val vm: PanicViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        enableEdgeToEdge()
        setContent {
            val settings by vm.settings.collectAsStateWithLifecycle()
            KairosTheme(settings.theme, settings.motion) {
                Surface(Modifier.fillMaxSize()) {
                    PanicScreen(vm, onDial = ::dial, onClose = ::finish, onLeave = { vm.leave(); finish() })
                }
            }
        }
    }

    /** Opens the dialer with the number filled in; the person presses call. No CALL_PHONE permission. */
    private fun dial(number: String) {
        val open = {
            runCatching {
                startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + CrisisLines.dialable(number))))
            }
        }
        val km = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        if (km.isKeyguardLocked) {
            km.requestDismissKeyguard(this, object : KeyguardManager.KeyguardDismissCallback() {
                override fun onDismissSucceeded() { open() }
            })
        } else {
            open()
        }
    }

    companion object {
        fun intent(context: Context): Intent =
            Intent(context, PanicActivity::class.java)
                .setAction("app.kairos.PANIC")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
    }
}
