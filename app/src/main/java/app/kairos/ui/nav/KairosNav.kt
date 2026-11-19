package app.kairos.ui.nav

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import app.kairos.ui.home.HomeScreen
import app.kairos.ui.urge.StagiraOffer
import app.kairos.ui.urge.UrgeScreen

object Routes {
    const val Home = "home"
    const val Urge = "urge?minutes={minutes}&auto={auto}"
    const val History = "history"
    const val Settings = "settings"
    const val Onboarding = "onboarding"
    fun urge(minutes: Int = -1, auto: Boolean = false) = "urge?minutes=$minutes&auto=$auto"
}

@Composable
fun KairosNav(
    nav: NavHostController,
    start: String,
    onPanic: () -> Unit,
    stagira: StagiraOffer?,
    extra: androidx.navigation.NavGraphBuilder.() -> Unit = {},
) {
    NavHost(nav, startDestination = start) {
        composable(Routes.Home) {
            HomeScreen(
                onUrge = { nav.navigate(Routes.urge()) },
                onPanic = onPanic,
                onHistory = { nav.navigate(Routes.History) },
                onSettings = { nav.navigate(Routes.Settings) },
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
        extra()
    }
}
