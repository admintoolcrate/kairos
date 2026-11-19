package app.kairos.panic

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.kairos.engine.CrisisContacts
import app.kairos.engine.GroundingPrompt
import app.kairos.engine.GroundingWalkthrough
import app.kairos.engine.Sense
import app.kairos.data.ContactEntity
import app.kairos.ui.components.BreathingGuide
import app.kairos.ui.components.CardShape
import app.kairos.ui.components.CheckIn
import app.kairos.ui.components.LinkButton
import app.kairos.ui.components.PrimaryButton
import app.kairos.ui.components.QuietButton
import app.kairos.ui.theme.LocalReducedMotion
import app.kairos.ui.theme.PanicText
import kotlinx.coroutines.launch

@Composable
fun PanicScreen(vm: PanicViewModel, onDial: (String) -> Unit, onClose: () -> Unit, onLeave: () -> Unit) {
    val page by vm.page.collectAsStateWithLifecycle()
    val checkingIn by vm.checkingIn.collectAsStateWithLifecycle()
    var confirmLeave by rememberSaveable { mutableStateOf(false) }

    BackHandler(enabled = !checkingIn) { confirmLeave = true }

    if (checkingIn) {
        Box(Modifier.fillMaxSize().safeDrawingPadding(), contentAlignment = Alignment.Center) {
            CheckIn(onAnswer = { vm.checkIn(it); onClose() })
        }
        return
    }

    Column(Modifier.fillMaxSize().safeDrawingPadding()) {
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when (page) {
                PanicPage.Breathe -> BreathePage(vm)
                PanicPage.Ground -> GroundPage(vm)
                PanicPage.Words -> WordsPage(vm)
                PanicPage.Call -> CallPage(vm, onDial)
            }
        }
        LinkButton("I'm okay now", vm::imOkay, Modifier.align(Alignment.CenterHorizontally))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        PageBar(page, vm::go)
    }

    if (confirmLeave) {
        AlertDialog(
            onDismissRequest = { confirmLeave = false },
            title = { Text("Leave panic help?", style = MaterialTheme.typography.headlineSmall) },
            text = { Text("You can come back any time.", style = MaterialTheme.typography.bodyLarge) },
            confirmButton = { LinkButton("Leave", onLeave) },
            dismissButton = { LinkButton("Stay", { confirmLeave = false }) },
        )
    }
}

/** Four large, labelled places. Not a menu: always visible, one tap each. */
@Composable
private fun PageBar(current: PanicPage, onGo: (PanicPage) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        PanicPage.entries.forEach { p ->
            val on = p == current
            Surface(
                onClick = { onGo(p) },
                modifier = Modifier.weight(1f).heightIn(min = 64.dp).semantics { selected = on },
                shape = CardShape,
                color = if (on) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,
                border = if (on) BorderStroke(1.dp, MaterialTheme.colorScheme.secondary) else null,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(p.label, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}

@Composable
private fun BreathePage(vm: PanicViewModel) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val pattern = settings.breathPattern
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            "Breathe with the circle",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Text(
            "In for ${pattern.inhaleSeconds}, out for ${pattern.exhaleSeconds}. No need to hold.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        BreathingGuide(pattern = pattern, haptics = settings.haptics, circleSize = 260.dp, phaseStyle = PanicText.Phase)
        Spacer(Modifier.height(24.dp))
        QuietButton("Next: grounding", { vm.go(PanicPage.Ground) }, style = PanicText.Button)
    }
}

private fun GroundingPrompt.instruction(): String = when (sense) {
    Sense.See -> "Name something you can see."
    Sense.Touch -> "Notice something you can touch or feel."
    Sense.Hear -> "Listen for something you can hear."
    Sense.Smell -> "Notice something you can smell."
    Sense.Taste -> "Notice something you can taste."
}

private fun GroundingPrompt.counter(): String {
    val noun = when (sense) {
        Sense.See -> "you can see"
        Sense.Touch -> "you can feel"
        Sense.Hear -> "you can hear"
        Sense.Smell -> "you can smell"
        Sense.Taste -> "you can taste"
    }
    return "$item of $count, things $noun"
}

@Composable
private fun GroundPage(vm: PanicViewModel) {
    val walk by vm.grounding.collectAsStateWithLifecycle()
    val prompt = walk.current
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (prompt == null) {
            Text(
                "You noticed what's around you. You are here, right now.",
                style = PanicText.Prompt,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
            Spacer(Modifier.height(16.dp))
            PrimaryButton("Read your words", { vm.go(PanicPage.Words) }, style = PanicText.Button)
            QuietButton("Start again", { vm.ground { it.restart() } }, style = PanicText.Button)
            return@Column
        }
        Text(
            "${prompt.count}",
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.secondary,
            // The number is read as part of the counter below.
            modifier = Modifier.clearAndSetSemantics { },
        )
        Text(
            prompt.instruction(),
            style = PanicText.Prompt,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
        Text(
            prompt.counter(),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        PrimaryButton("Next", { vm.ground { it.next() } }, style = PanicText.Button)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (!walk.isAtStart) LinkButton("Back", { vm.ground { it.back() } })
            if (prompt.count > 1) LinkButton("Can't find one, move on", { vm.ground { it.skipSense() } })
        }
    }
}

@Composable
private fun WordsPage(vm: PanicViewModel) {
    val cards by vm.cardTexts.collectAsStateWithLifecycle()
    val pager = rememberPagerState { cards.size }
    val scope = rememberCoroutineScope()
    val reduced = LocalReducedMotion.current
    Column(Modifier.fillMaxSize().padding(vertical = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        HorizontalPager(pager, Modifier.weight(1f).fillMaxWidth()) { i ->
            Box(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 28.dp), contentAlignment = Alignment.Center) {
                Text(
                    cards.getOrElse(i) { "" },
                    style = PanicText.Card,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
        }
        Text(
            "${pager.currentPage + 1} of ${cards.size}",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        Row(Modifier.padding(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            QuietButton("Previous", {
                scope.launch { val t = (pager.currentPage - 1).coerceAtLeast(0); if (reduced) pager.scrollToPage(t) else pager.animateScrollToPage(t) }
            }, Modifier.weight(1f))
            PrimaryButton("Next", {
                scope.launch { val t = (pager.currentPage + 1) % cards.size.coerceAtLeast(1); if (reduced) pager.scrollToPage(t) else pager.animateScrollToPage(t) }
            }, Modifier.weight(1f))
        }
    }
}

@Composable
private fun CallPage(vm: PanicViewModel, onDial: (String) -> Unit) {
    val contacts by vm.contacts.collectAsStateWithLifecycle()
    val crisis by vm.crisis.collectAsStateWithLifecycle()
    CallList(contacts, crisis, onDial)
}

@Composable
private fun CallList(contacts: List<ContactEntity>, crisis: CrisisContacts, onDial: (String) -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Call someone", style = PanicText.Prompt, color = MaterialTheme.colorScheme.onBackground)
        if (contacts.isEmpty()) {
            Text(
                "When you feel steadier, you can add people you trust in Kairos settings.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        contacts.forEach { c ->
            PrimaryButton("Call ${c.name}", { onDial(c.phone) }, style = PanicText.Button)
        }
        crisis.line?.let { line ->
            QuietButton("${line.name}\n${line.number}", { onDial(line.number) }, style = PanicText.Button)
        } ?: Text(
            "You can add a support line for where you live in Kairos settings.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "If you have chest pain or trouble breathing, call emergency services.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        QuietButton("Emergency services · ${crisis.emergencyNumber}", { onDial(crisis.emergencyNumber) })
    }
}
