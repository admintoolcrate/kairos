package app.kairos

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.remember
import androidx.lifecycle.lifecycleScope
import app.kairos.data.Prefs
import app.kairos.data.SessionRepository
import app.kairos.stagira.StagiraBridge
import app.kairos.ui.urge.StagiraOffer
import app.kairos.lock.AppLock
import app.kairos.panic.PanicActivity
import app.kairos.ui.nav.KairosNav
import app.kairos.ui.nav.Routes
import app.kairos.ui.theme.KairosTheme
import app.kairos.ui.theme.MotionPref
import app.kairos.ui.theme.ThemeMode
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Home, urge mode, history and settings. A FragmentActivity only so BiometricPrompt
 * can attach; app lock is applied per screen (history and settings) by LockGate,
 * never to home or urge mode.
 */
@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    @Inject lateinit var prefs: Prefs
    @Inject lateinit var lock: AppLock

    @Inject lateinit var sessions: SessionRepository

    private var stagiraSession: Long? = null
    private val stagiraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        val id = stagiraSession
        stagiraSession = null
        if (r.resultCode == RESULT_OK && id != null) lifecycleScope.launch { sessions.markSentToStagira(id) }
    }

    private fun recordInStagira(sessionId: Long?) {
        stagiraSession = sessionId
        runCatching { stagiraLauncher.launch(StagiraBridge.intent()) }
    }

    /** A pending deep link (from the widget, tile or a shortcut) to act on once composed. */
    private var pending by mutableStateOf<String?>(null)
    private var launchedPlain = false

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        lock.observeProcess()
        if (savedInstanceState == null) {
            pending = routeFor(intent)
            launchedPlain = pending == null && intent?.getStringExtra(EXTRA_START) == null
        }
        setContent {
            val settings by prefs.settings.collectAsStateWithLifecycle(initialValue = null)
            val scope = rememberCoroutineScope()
            KairosTheme(settings?.theme ?: ThemeMode.System, settings?.motion ?: MotionPref.System) {
                Surface(Modifier.fillMaxSize()) {
                    val nav = rememberNavController()
                    val stagiraInstalled = remember { StagiraBridge.isAvailable(this@MainActivity) }
                    KairosNav(
                        nav, Routes.Home,
                        onPanic = ::openPanic,
                        onOnboarded = { scope.launch { prefs.update { it.copy(onboardingDone = true) } } },
                        stagira = if (stagiraInstalled && settings?.stagira == true) StagiraOffer(::recordInStagira) else null,
                        stagiraInstalled = stagiraInstalled,
                    )
                    LaunchedEffect(pending) {
                        pending?.let { nav.navigate(it) { launchSingleTop = true } }
                        pending = null
                    }
                    // First run from the launcher: a short welcome. Never in the way of a widget or tile.
                    LaunchedEffect(settings?.onboardingDone) {
                        if (launchedPlain && settings?.onboardingDone == false) {
                            launchedPlain = false
                            nav.navigate(Routes.Onboarding) { popUpTo(Routes.Home) { inclusive = true } }
                        }
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
        START_PANIC -> { openPanic(); null }
        START_URGE -> minutesOf(intent).let { Routes.urge(it, auto = it > 0) }
        else -> null
    }

    /** Minutes arrive as an Int from our own code, or as a String from static shortcuts. */
    private fun minutesOf(intent: Intent): Int =
        intent.extras?.let { it.getString(EXTRA_MINUTES)?.toIntOrNull() ?: it.getInt(EXTRA_MINUTES, -1) } ?: -1

    private fun openPanic() = startActivity(PanicActivity.intent(this))

    companion object {
        const val EXTRA_START = "app.kairos.START"
        const val EXTRA_MINUTES = "app.kairos.MINUTES"
        const val START_URGE = "urge"
        const val START_PANIC = "panic"

        fun urgeIntent(context: Context, minutes: Int = -1): Intent =
            Intent(context, MainActivity::class.java)
                .setAction("app.kairos.URGE")
                .putExtra(EXTRA_START, START_URGE)
                .putExtra(EXTRA_MINUTES, minutes)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
    }
}
