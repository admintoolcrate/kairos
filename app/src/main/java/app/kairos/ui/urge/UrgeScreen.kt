package app.kairos.ui.urge

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.kairos.data.Defaults
import app.kairos.data.Outcome
import app.kairos.engine.BreathPattern
import app.kairos.engine.UrgeTimer
import app.kairos.ui.components.BreathingGuide
import app.kairos.ui.components.CheckIn
import app.kairos.ui.components.Heading
import app.kairos.ui.components.LinkButton
import app.kairos.ui.components.PrimaryButton
import app.kairos.ui.components.QuietButton
import app.kairos.ui.theme.LocalReducedMotion
import kotlinx.coroutines.delay

@Composable
fun UrgeScreen(
    onClose: () -> Unit,
    onPanic: () -> Unit,
    stagira: StagiraOffer? = null,
    vm: UrgeViewModel = hiltViewModel(),
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()

    BackHandler(enabled = ui.stage == UrgeStage.Running) { vm.toOutcome() }

    Box(Modifier.fillMaxSize().safeDrawingPadding()) {
        when (ui.stage) {
            UrgeStage.Choose -> ChooseMinutes(ui.minutes, vm::choose, vm::start, onClose)
            UrgeStage.Running -> Running(
                ui = ui,
                now = vm::now,
                pattern = settings?.breathPattern ?: BreathPattern.Default,
                haptics = settings?.haptics ?: true,
                distractions = settings?.distractions ?: Defaults.distractions,
                onPause = vm::pause, onResume = vm::resume, onExtend = vm::extend, onDone = vm::toOutcome,
            )
            UrgeStage.Outcome -> OutcomeStep(vm::answer, stagira, ui.sessionId)
            UrgeStage.StillHere -> StillHere(
                onAgain = { vm.choose(ui.minutes); vm.start() },
                onPanic = onPanic,
                onOkay = vm::toCheckIn,
            )
            UrgeStage.CheckIn -> Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) { CheckIn(vm::checkIn) }
            UrgeStage.Done -> Done(ui.outcome, onClose)
        }
    }
}

@Composable
private fun ChooseMinutes(minutes: Int, onChoose: (Int) -> Unit, onStart: () -> Unit, onClose: () -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        LinkButton("Back", onClose)
        Heading("Ride it out", style = MaterialTheme.typography.displaySmall)
        Text(
            "Urges rise and fall. Give this one some time before deciding anything.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            UrgeTimer.CHOICES_MINUTES.forEach { m ->
                FilterChip(
                    selected = m == minutes,
                    onClick = { onChoose(m) },
                    label = { Text("$m minutes", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(12.dp)) },
                    modifier = Modifier.weight(1f).heightIn(min = 64.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                    ),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        PrimaryButton("Start", onStart)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Running(
    ui: UrgeUi,
    now: () -> Long,
    pattern: BreathPattern,
    haptics: Boolean,
    distractions: List<String>,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onExtend: () -> Unit,
    onDone: () -> Unit,
) {
    val timer = ui.timer ?: return
    var tick by androidx.compose.runtime.remember { mutableLongStateOf(now()) }
    LaunchedEffect(timer) {
        while (true) { tick = now(); delay(250) }
    }
    val remaining = timer.remaining(tick)
    val finished = timer.isFinished(tick)
    val elapsed = timer.elapsed(tick)
    val message = Defaults.urgeMessages[((elapsed / 40_000L) % Defaults.urgeMessages.size).toInt()]
    var breathing by rememberSaveable { mutableStateOf(false) }
    var trying by rememberSaveable { mutableStateOf<String?>(null) }
    val reduced = LocalReducedMotion.current

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            if (finished) "You gave it time" else "Riding it out",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        CountdownRing(timer.progress(tick), if (finished) "Done" else UrgeTimer.format(remaining), timer.isPaused)

        AnimatedContent(
            targetState = if (finished) "The time is up. How is the urge now?" else message,
            transitionSpec = {
                val d = if (reduced) 0 else 900
                fadeIn(tween(d)) togetherWith fadeOut(tween(d))
            },
            label = "message",
        ) { text ->
            Text(
                text,
                style = MaterialTheme.typography.headlineSmall.copy(fontStyle = FontStyle.Italic),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        if (finished) {
            PrimaryButton("How did it go?", onDone)
            QuietButton("Five more minutes", onExtend)
            return@Column
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            QuietButton(if (timer.isPaused) "Resume" else "Pause", if (timer.isPaused) onResume else onPause, Modifier.weight(1f))
            QuietButton("I'm done", onDone, Modifier.weight(1f))
        }

        if (breathing) {
            BreathingGuide(
                pattern = pattern,
                haptics = haptics,
                circleSize = 160.dp,
                phaseStyle = MaterialTheme.typography.headlineMedium,
                breaths = 6,
                onFinished = { breathing = false },
            )
            LinkButton("Stop breathing guide", { breathing = false })
        } else {
            QuietButton("Breathe for a minute", { breathing = true })
        }

        Text(
            trying?.let { "Trying: $it" } ?: "Something else to do while you wait",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
        )
        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            distractions.forEach { d ->
                FilterChip(
                    selected = trying == d,
                    onClick = { trying = if (trying == d) null else d },
                    label = { Text(d, style = MaterialTheme.typography.bodyMedium) },
                    modifier = Modifier.heightIn(min = 48.dp),
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer),
                )
            }
        }
    }
}

/** A thin ring that empties slowly. It never changes colour, flashes or speeds up. */
@Composable
private fun CountdownRing(progress: Float, label: String, paused: Boolean) {
    val track = MaterialTheme.colorScheme.outlineVariant
    val ink = MaterialTheme.colorScheme.secondary
    // Grow with the font size so large text never spills out of the ring.
    val ring = 240.dp * LocalDensity.current.fontScale.coerceIn(1f, 1.8f)
    Box(
        // Always a circle: as wide as it wants up to the screen width, and square.
        Modifier.widthIn(max = ring).fillMaxWidth().aspectRatio(1f).semantics {
            contentDescription = "Time left"
            stateDescription = if (paused) "$label, paused" else label
        },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val w = 6.dp.toPx()
            val d = size.minDimension - 2 * w
            val corner = androidx.compose.ui.geometry.Offset((size.width - d) / 2, (size.height - d) / 2)
            drawCircle(track, radius = d / 2, style = Stroke(w / 3))
            drawArc(
                ink, startAngle = -90f, sweepAngle = 360f * (1f - progress), useCenter = false,
                topLeft = corner, size = androidx.compose.ui.geometry.Size(d, d),
                style = Stroke(w, cap = StrokeCap.Round),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, style = MaterialTheme.typography.displayLarge, color = MaterialTheme.colorScheme.onBackground)
            if (paused) Text("Paused", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Optional link to Stagira, shown only when it's installed and turned on. */
data class StagiraOffer(val record: (sessionId: Long?) -> Unit)

@Composable
private fun OutcomeStep(onAnswer: (Outcome) -> Unit, stagira: StagiraOffer?, sessionId: Long?) {
    var sendToStagira by rememberSaveable { mutableStateOf(false) }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
    ) {
        Heading("Did the urge pass?", style = MaterialTheme.typography.displaySmall)
        Text(
            "Any answer is fine. This is only for you.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        if (stagira != null) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Also record it in Stagira",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.onBackground,
                )
                androidx.compose.material3.Switch(checked = sendToStagira, onCheckedChange = { sendToStagira = it })
            }
        }
        val answer = { o: Outcome ->
            if (stagira != null && sendToStagira && (o == Outcome.Passed || o == Outcome.Eased)) stagira.record(sessionId)
            onAnswer(o)
        }
        PrimaryButton("It passed", { answer(Outcome.Passed) })
        QuietButton("It eased", { answer(Outcome.Eased) })
        QuietButton("It's still here", { answer(Outcome.StillHere) })
        LinkButton("Skip", { answer(Outcome.Skipped) })
    }
}

@Composable
private fun StillHere(onAgain: () -> Unit, onPanic: () -> Unit, onOkay: () -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
    ) {
        Heading("That's okay", style = MaterialTheme.typography.displaySmall)
        Text(
            "Urges can come back in waves. You've already waited once, and you can wait again.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(8.dp))
        PrimaryButton("Wait a while longer", onAgain)
        QuietButton("Slow breathing and grounding", onPanic)
        LinkButton("I'm alright for now", onOkay)
    }
}

@Composable
private fun Done(outcome: Outcome?, onClose: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Heading(
            when (outcome) {
                Outcome.Passed -> "It passed."
                Outcome.Eased -> "It eased."
                else -> "You gave it time."
            },
            style = MaterialTheme.typography.displaySmall,
            center = true,
        )
        Text(
            "Pausing before acting is a skill, and you just practised it.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        PrimaryButton("Close", onClose)
    }
}
