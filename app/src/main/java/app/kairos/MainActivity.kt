package app.kairos

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import app.kairos.data.Prefs
import app.kairos.ui.nav.KairosNav
import app.kairos.ui.nav.Routes
import app.kairos.ui.theme.KairosTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var prefs: Prefs

    /** A pending deep link (from the widget, tile or a shortcut) to act on once composed. */
    private var pending by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) pending = routeFor(intent)
        setContent {
            val settings by prefs.settings.collectAsStateWithLifecycle(initialValue = null)
            KairosTheme(settings?.theme ?: app.kairos.ui.theme.ThemeMode.System, settings?.motion ?: app.kairos.ui.theme.MotionPref.System) {
                Surface(Modifier.fillMaxSize()) {
                    val nav = rememberNavController()
                    KairosNav(nav, Routes.Home, onPanic = ::openPanic, stagira = null)
                    LaunchedEffect(pending) {
                        pending?.let { nav.navigate(it) { launchSingleTop = true } }
                        pending = null
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        pending = routeFor(intent)
    }

    private fun routeFor(intent: Intent?): String? = when (intent?.getStringExtra(EXTRA_START)) {
        START_URGE -> Routes.urge(intent.getIntExtra(EXTRA_MINUTES, -1), auto = intent.getIntExtra(EXTRA_MINUTES, -1) > 0)
        else -> null
    }

    private fun openPanic() {
        // Panic help arrives in the next milestone.
    }

    companion object {
        const val EXTRA_START = "app.kairos.START"
        const val EXTRA_MINUTES = "app.kairos.MINUTES"
        const val START_URGE = "urge"

        fun urgeIntent(context: Context, minutes: Int = -1): Intent =
            Intent(context, MainActivity::class.java)
                .setAction("app.kairos.URGE")
                .putExtra(EXTRA_START, START_URGE)
                .putExtra(EXTRA_MINUTES, minutes)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
    }
}
