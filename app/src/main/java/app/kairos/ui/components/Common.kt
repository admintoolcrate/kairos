package app.kairos.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/** Family shapes (docs/family-look.md): cards 24 dp, small tiles 14 dp, buttons Material's pill. */
val CardShape = RoundedCornerShape(24.dp)
val TileShape = RoundedCornerShape(14.dp)

@Composable
fun Heading(text: String, modifier: Modifier = Modifier, style: TextStyle = MaterialTheme.typography.headlineMedium, center: Boolean = false) {
    Text(
        text,
        style = style,
        color = MaterialTheme.colorScheme.onBackground,
        textAlign = if (center) TextAlign.Center else null,
        modifier = modifier.semantics { heading() },
    )
}

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, style: TextStyle = MaterialTheme.typography.labelLarge) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().heightIn(min = 56.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp),
    ) { Text(text, style = style, textAlign = TextAlign.Center) }
}

@Composable
fun QuietButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, style: TextStyle = MaterialTheme.typography.labelLarge) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().heightIn(min = 56.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp),
    ) { Text(text, style = style, color = MaterialTheme.colorScheme.onBackground, textAlign = TextAlign.Center) }
}

@Composable
fun LinkButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    TextButton(onClick = onClick, modifier = modifier.heightIn(min = 48.dp)) {
        Text(text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
    }
}

/** A large tappable paper card used for the two mode choices. */
@Composable
fun ChoiceCard(
    title: String,
    body: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    emphasised: Boolean = false,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().heightIn(min = 140.dp).semantics { role = Role.Button },
        shape = CardShape,
        color = if (emphasised) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, if (emphasised) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.onSurface)
            Text(body, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun PaperCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = CardShape,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) { Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content) }
}
