package app.kairos.ui.nav

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import app.kairos.backup.BackupScreen
import app.kairos.lock.LockGate
import app.kairos.ui.history.HistoryScreen
import app.kairos.ui.home.HomeScreen
import app.kairos.ui.onboarding.OnboardingScreen
import app.kairos.ui.settings.AboutScreen
import app.kairos.ui.settings.AccessScreen
import app.kairos.ui.settings.BreathingSettingsScreen
import app.kairos.ui.settings.CardsScreen
import app.kairos.ui.settings.ContactsScreen
import app.kairos.ui.settings.CrisisScreen
import app.kairos.ui.settings.LockScreen
import app.kairos.ui.settings.LookScreen
import app.kairos.ui.settings.SettingsIndex
import app.kairos.ui.settings.SettingsRoutes
import app.kairos.ui.settings.SettingsViewModel
import app.kairos.ui.settings.StagiraSettingsScreen
import app.kairos.ui.settings.UrgeSettingsScreen
import app.kairos.ui.urge.StagiraOffer
import app.kairos.ui.urge.UrgeScreen

object Routes {
    const val Home = "home"
    const val Urge = "urge?minutes={minutes}&auto={auto}"
    const val History = "history"
    const val Onboarding = "onboarding"
    fun urge(minutes: Int = -1, auto: Boolean = false) = "urge?minutes=$minutes&auto=$auto"
}

@Composable
fun KairosNav(
    nav: NavHostController,
    start: String,
    onPanic: () -> Unit,
    onOnboarded: () -> Unit,
    stagira: StagiraOffer?,
    stagiraInstalled: Boolean,
    extra: NavGraphBuilder.(back: () -> Unit, locked: @Composable (@Composable (SettingsViewModel) -> Unit) -> Unit) -> Unit = { _, _ -> },
) {
    val back: () -> Unit = { if (!nav.popBackStack()) nav.navigate(Routes.Home) }

    /** History and every settings page sit behind the optional app lock; nothing else does. */
    val locked: @Composable (@Composable (SettingsViewModel) -> Unit) -> Unit = { content ->
        val vm: SettingsViewModel = hiltViewModel()
        val secrets by vm.secrets.collectAsStateWithLifecycle()
        LockGate(vm.lock, secrets, onBack = back) { content(vm) }
    }

    NavHost(nav, startDestination = start) {
        composable(Routes.Home) {
            HomeScreen(
                onUrge = { nav.navigate(Routes.urge()) },
                onPanic = onPanic,
                onHistory = { nav.navigate(Routes.History) },
                onSettings = { nav.navigate(SettingsRoutes.Index) },
            )
        }
        composable(
            Routes.Urge,
            arguments = listOf(
                navArgument("minutes") { type = NavType.IntType; defaultValue = -1 },
                navArgument("auto") { type = NavType.BoolType; defaultValue = false },
            ),
        ) {
            UrgeScreen(
                onClose = { if (!nav.popBackStack(Routes.Home, inclusive = false)) nav.navigate(Routes.Home) { popUpTo(0) } },
                onPanic = onPanic,
                stagira = stagira,
            )
        }
        composable(Routes.Onboarding) {
            OnboardingScreen(
                onDone = { onOnboarded(); nav.navigate(Routes.Home) { popUpTo(0) } },
                onSetUp = { nav.navigate(it) },
            )
        }
        composable(Routes.History) { locked { HistoryScreen(onBack = back) } }
        settings(SettingsRoutes.Index, locked) { vm ->
            SettingsIndex(vm, back, { nav.navigate(it) }, showStagira = stagiraInstalled)
        }
        settings(SettingsRoutes.Cards, locked) { CardsScreen(it, back) }
        settings(SettingsRoutes.Contacts, locked) { ContactsScreen(it, back) }
        settings(SettingsRoutes.Crisis, locked) { CrisisScreen(it, back) }
        settings(SettingsRoutes.Breathing, locked) { BreathingSettingsScreen(it, back) }
        settings(SettingsRoutes.Urge, locked) { UrgeSettingsScreen(it, back) }
        settings(SettingsRoutes.Look, locked) { LookScreen(it, back) }
        settings(SettingsRoutes.Access, locked) { AccessScreen(back) }
        settings(SettingsRoutes.Lock, locked) { LockScreen(it, back) }
        settings(SettingsRoutes.Stagira, locked) { StagiraSettingsScreen(it, back) }
        settings(SettingsRoutes.About, locked) { AboutScreen(back) }
        settings(SettingsRoutes.Backup, locked) { BackupScreen(back) }
        extra(back, locked)
    }
}

private fun NavGraphBuilder.settings(
    route: String,
    locked: @Composable (@Composable (SettingsViewModel) -> Unit) -> Unit,
    content: @Composable (SettingsViewModel) -> Unit,
) = composable(route) { _: NavBackStackEntry -> locked(content) }
