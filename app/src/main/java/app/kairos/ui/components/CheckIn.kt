package app.kairos.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

private val labels = mapOf(1 to "Calm", 2 to "Mild", 3 to "Moderate", 4 to "Strong", 5 to "Very strong")

/**
 * The optional ten-second check-in. One tap on a number, or Skip.
 * It is never required, and nothing is lost by skipping.
 */
@Composable
fun CheckIn(onAnswer: (Int?) -> Unit, modifier: Modifier = Modifier, question: String = "How strong does it feel now?") {
    Column(modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Heading(question, center = true)
        Spacer(Modifier.height(8.dp))
        Text(
            "Optional. 1 is calm, 5 is very strong.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            (1..5).forEach { n ->
                OutlinedButton(
                    onClick = { onAnswer(n) },
                    modifier = Modifier.weight(1f).heightIn(min = 64.dp).semantics { contentDescription = "$n, ${labels[n]}" },
                    shape = CardShape,
                ) { Text("$n", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onBackground) }
            }
        }
        Spacer(Modifier.height(16.dp))
        LinkButton("Skip", onClick = { onAnswer(null) })
    }
}
