package app.kairos.ui.history

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import app.kairos.data.Mode
import app.kairos.data.Outcome
import app.kairos.data.SessionEntity
import app.kairos.data.SessionRepository
import app.kairos.ui.components.LinkButton
import app.kairos.ui.components.PaperCard
import app.kairos.ui.settings.Body
import app.kairos.ui.settings.SectionTitle
import app.kairos.ui.settings.SettingsPage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(private val sessions: SessionRepository) : ViewModel() {
    val history: StateFlow<List<SessionEntity>?> = sessions.history.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    fun delete(id: Long) = viewModelScope.launch { sessions.delete(id) }
}

private val dateFmt = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)

private fun Outcome.words() = when (this) {
    Outcome.Passed -> "It passed"
    Outcome.Eased -> "It eased"
    Outcome.StillHere -> "It was still there"
    Outcome.Skipped -> "No answer"
}

/** A plain record. No streaks, no scores, nothing to keep up. */
@Composable
fun HistoryScreen(onBack: () -> Unit, vm: HistoryViewModel = hiltViewModel()) {
    val list by vm.history.collectAsStateWithLifecycle()
    var removing by remember { mutableStateOf<SessionEntity?>(null) }
    val all = list ?: return
    val urges = all.filter { it.mode == Mode.Urge && it.endedAt != null }
    val panics = all.filter { it.mode == Mode.Panic }
    SettingsPage("History", onBack) {
        PaperCard {
            Text("${urges.size} ${if (urges.size == 1) "urge" else "urges"} given time", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
            val passed = urges.count { it.outcome == Outcome.Passed || it.outcome == Outcome.Eased }
            if (urges.isNotEmpty()) Body("$passed passed or eased")
            Text("${panics.size} panic help ${if (panics.size == 1) "session" else "sessions"}", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
        }
        if (all.isEmpty()) Body("Nothing here yet. Each time you use Kairos, a short note appears here, just for you.")
        if (all.isNotEmpty()) SectionTitle("Recent")
        all.forEach { s ->
            Surface(onClick = { removing = s }, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp), color = MaterialTheme.colorScheme.background) {
                Column(Modifier.padding(vertical = 10.dp)) {
                    Text(
                        if (s.mode == Mode.Urge) "Urge" + (s.plannedMinutes?.let { ", $it minutes" } ?: "") else "Panic help",
                        style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground,
                    )
                    val parts = listOfNotNull(
                        dateFmt.format(Instant.ofEpochMilli(s.startedAt).atZone(ZoneId.systemDefault())),
                        s.outcome?.words(),
                        s.intensity?.let { "felt $it of 5 afterwards" },
                        if (s.sentToStagira) "noted in Stagira" else null,
                    )
                    Text(parts.joinToString(" · "), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
    removing?.let { s ->
        AlertDialog(
            onDismissRequest = { removing = null },
            title = { Text("Remove this entry?") },
            text = { Text("It will be gone from your history.") },
            confirmButton = { LinkButton("Remove", { vm.delete(s.id); removing = null }) },
            dismissButton = { LinkButton("Keep", { removing = null }) },
        )
    }
}
