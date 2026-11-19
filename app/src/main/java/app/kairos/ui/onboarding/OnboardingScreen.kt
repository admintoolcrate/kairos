package app.kairos.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.kairos.data.Defaults
import app.kairos.ui.components.Heading
import app.kairos.ui.components.LinkButton
import app.kairos.ui.components.PaperCard
import app.kairos.ui.components.PrimaryButton
import app.kairos.ui.components.QuietButton
import app.kairos.ui.settings.SettingsRoutes

/** Three short pages. The disclaimer can't be skipped; the setup page can. */
@Composable
fun OnboardingScreen(onDone: () -> Unit, onSetUp: (String) -> Unit) {
    var page by rememberSaveable { mutableIntStateOf(0) }
    Column(
        Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Spacer(Modifier.height(32.dp))
        when (page) {
            0 -> {
                Heading("Kairos", style = MaterialTheme.typography.displayMedium)
                Text(
                    "A quiet place to ride out urges, cravings and panic.",
                    style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    "It's free, with no ads and no accounts. Everything stays on your phone.",
                    style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(16.dp))
                PrimaryButton("Next", { page = 1 })
            }
            1 -> {
                Heading("Before you start", style = MaterialTheme.typography.displaySmall)
                PaperCard {
                    Text(Defaults.disclaimer, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                }
                Text(
                    "You can find this again in Settings, under About.",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                PrimaryButton("I understand", { page = 2 })
            }
            else -> {
                Heading("Make it yours", style = MaterialTheme.typography.displaySmall)
                Text(
                    "Optional, and you can do it later. A few minutes now means it's ready when you need it.",
                    style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                QuietButton("Write cards in your own words", { onSetUp(SettingsRoutes.Cards) })
                QuietButton("Add someone to call", { onSetUp(SettingsRoutes.Contacts) })
                QuietButton("Check your support line", { onSetUp(SettingsRoutes.Crisis) })
                QuietButton("Add the quick tile or widget", { onSetUp(SettingsRoutes.Access) })
                Spacer(Modifier.height(8.dp))
                PrimaryButton("Done", onDone)
                LinkButton("Skip for now", onDone)
            }
        }
    }
}
