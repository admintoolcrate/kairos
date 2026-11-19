package app.kairos.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.unit.dp
import app.kairos.ui.components.CardShape
import app.kairos.ui.components.Heading
import app.kairos.ui.components.LinkButton

@Composable
fun SettingsPage(title: String, onBack: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        LinkButton("Back", onBack)
        Heading(title, style = MaterialTheme.typography.displaySmall)
        Spacer(Modifier.height(4.dp))
        content()
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
fun Body(text: String, muted: Boolean = true) = Text(
    text,
    style = MaterialTheme.typography.bodyLarge,
    color = if (muted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onBackground,
)

@Composable
fun SectionTitle(text: String) {
    Spacer(Modifier.height(8.dp))
    Heading(text, style = MaterialTheme.typography.titleLarge)
}

/** A row in the settings index. */
@Composable
fun NavRow(title: String, summary: String, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.padding(vertical = 12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
            if (summary.isNotEmpty()) Text(summary, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
fun SwitchRow(title: String, summary: String = "", checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onChange),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
            if (summary.isNotEmpty()) Text(summary, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Composable
fun <T> ChoiceRow(options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { (value, label) ->
            FilterChip(
                selected = value == selected,
                onClick = { onSelect(value) },
                label = { Text(label, style = MaterialTheme.typography.bodyMedium) },
                modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer),
            )
        }
    }
}

/** − value + with large targets, for breathing seconds. */
@Composable
fun Stepper(label: String, value: Int, onChange: (Int) -> Unit, unit: String = "seconds") {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onBackground)
        OutlinedButton({ onChange(value - 1) }, Modifier.heightIn(min = 48.dp).semantics { contentDescription = "Shorter $label" }, shape = CardShape) {
            Text("−", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground)
        }
        Text(
            "$value",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(horizontal = 16.dp).semantics { stateDescription = "$value $unit" },
            color = MaterialTheme.colorScheme.onBackground,
        )
        OutlinedButton({ onChange(value + 1) }, Modifier.heightIn(min = 48.dp).semantics { contentDescription = "Longer $label" }, shape = CardShape) {
            Text("+", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground)
        }
    }
}

@Composable
fun SmallAction(text: String, onClick: () -> Unit, description: String? = null) {
    LinkButton(text, onClick, Modifier.semantics { if (description != null) contentDescription = description; role = Role.Button })
}
