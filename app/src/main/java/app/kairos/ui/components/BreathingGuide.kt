package app.kairos.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.kairos.engine.BreathFrame
import app.kairos.engine.BreathPattern
import app.kairos.engine.BreathingEngine
import app.kairos.engine.BreathingPhase
import app.kairos.ui.theme.LocalReducedMotion
import kotlinx.coroutines.delay

fun BreathingPhase.word(): String = when (this) {
    BreathingPhase.Inhale -> "Breathe in"
    BreathingPhase.Exhale -> "Breathe out"
}

/**
 * Paced breathing: a circle that swells on the in-breath and settles on the longer
 * out-breath. With reduced motion the circle stays still and only the words change.
 * A soft haptic tick marks each phase change when [haptics] is on.
 *
 * @param breaths stop after this many breaths and call [onFinished]; null runs until removed.
 */
@Composable
fun BreathingGuide(
    pattern: BreathPattern,
    haptics: Boolean,
    modifier: Modifier = Modifier,
    circleSize: Dp = 240.dp,
    phaseStyle: TextStyle = MaterialTheme.typography.displaySmall,
    breaths: Int? = null,
    showCount: Boolean = true,
    onFinished: () -> Unit = {},
) {
    val engine = remember(pattern) { BreathingEngine(pattern) }
    val reduced = LocalReducedMotion.current
    val context = LocalContext.current
    val buzz = remember(context) { Haptics(context) }
    val finished by rememberUpdatedState(onFinished)
    var elapsed by remember(pattern) { mutableLongStateOf(0L) }

    LaunchedEffect(pattern, reduced, haptics, breaths) {
        var start = -1L
        var last = 0L
        while (true) {
            val now = if (reduced) {
                delay(200); System.nanoTime() / 1_000_000
            } else {
                withFrameMillis { it }
            }
            if (start < 0) start = now - elapsed
            val t = now - start
            if (haptics && engine.phaseChangedBetween(last, t)) buzz.tick()
            last = t
            elapsed = t
            if (breaths != null && t >= engine.durationOf(breaths)) {
                finished(); break
            }
        }
    }

    val frame: BreathFrame = engine.frameAt(elapsed)
    val ink = MaterialTheme.colorScheme.secondary
    val fill = MaterialTheme.colorScheme.secondaryContainer
    val line = MaterialTheme.colorScheme.outlineVariant

    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Box(Modifier.size(circleSize).semantics { contentDescription = "Breathing circle" }, contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(circleSize)) {
                val maxR = size.minDimension / 2
                val minR = maxR * 0.42f
                drawCircle(line, radius = maxR - 1.dp.toPx(), style = Stroke(1.dp.toPx()))
                val r = if (reduced) maxR * 0.72f else minR + (maxR - minR) * frame.expansion
                drawCircle(fill, radius = r)
                drawCircle(ink, radius = r, style = Stroke(2.dp.toPx()))
            }
        }
        Spacer(Modifier.height(28.dp))
        Text(
            frame.phase.word(),
            style = phaseStyle,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
        if (showCount) {
            Spacer(Modifier.height(8.dp))
            val secs = if (frame.phase == BreathingPhase.Inhale) pattern.inhaleSeconds else pattern.exhaleSeconds
            Text(
                "${frame.secondsLeftInPhase} of $secs",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                // The count changes every second; announcing it would be noisy.
                modifier = Modifier.semantics { contentDescription = "" },
            )
        }
    }
}
