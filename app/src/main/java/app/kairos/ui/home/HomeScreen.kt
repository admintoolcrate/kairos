package app.kairos.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import app.kairos.data.Mode
import app.kairos.data.Prefs
import app.kairos.data.Settings
import app.kairos.ui.components.ChoiceCard
import app.kairos.ui.components.LinkButton
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(private val prefs: Prefs) : ViewModel() {
    val settings: StateFlow<Settings?> = prefs.settings.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun remember(mode: Mode) = viewModelScope.launch { prefs.update { it.copy(lastMode = mode) } }
}

/** One big screen with the two modes; the one used last comes first. */
@Composable
fun HomeScreen(
    onUrge: () -> Unit,
    onPanic: () -> Unit,
    onHistory: () -> Unit,
    onSettings: () -> Unit,
    vm: HomeViewModel = hiltViewModel(),
) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val last = settings?.lastMode ?: Mode.Urge
    val minutes = settings?.urgeMinutes ?: 10

    val urge = @Composable {
        ChoiceCard(
            title = "An urge",
            body = "Ride it out with a $minutes minute timer, a little breathing and something else to do.",
            onClick = { vm.remember(Mode.Urge); onUrge() },
            emphasised = last == Mode.Urge,
        )
    }
    val panic = @Composable {
        ChoiceCard(
            title = "Panic",
            body = "Slow breathing, grounding, your own words, and someone to call.",
            onClick = { vm.remember(Mode.Panic); onPanic() },
            emphasised = last == Mode.Panic,
        )
    }

    Column(
        Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Spacer(Modifier.height(24.dp))
        Text("Kairos", style = MaterialTheme.typography.displayMedium, color = MaterialTheme.colorScheme.onBackground)
        Text(
            "What's here right now?",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        if (last == Mode.Panic) { panic(); urge() } else { urge(); panic() }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            LinkButton("History", onHistory)
            Text("·", color = MaterialTheme.colorScheme.outline)
            LinkButton("Settings", onSettings)
        }
    }
}
