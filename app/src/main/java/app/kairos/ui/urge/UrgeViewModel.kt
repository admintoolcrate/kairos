package app.kairos.ui.urge

import android.os.SystemClock
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.kairos.data.Mode
import app.kairos.data.Outcome
import app.kairos.data.Prefs
import app.kairos.data.SessionRepository
import app.kairos.data.Settings
import app.kairos.engine.UrgeTimer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class UrgeStage { Choose, Running, Outcome, StillHere, CheckIn, Done }

data class UrgeUi(
    val stage: UrgeStage = UrgeStage.Choose,
    val minutes: Int = 10,
    val timer: UrgeTimer? = null,
    val sessionId: Long? = null,
    val outcome: Outcome? = null,
)

@HiltViewModel
class UrgeViewModel @Inject constructor(
    private val prefs: Prefs,
    private val sessions: SessionRepository,
    private val saved: SavedStateHandle,
) : ViewModel() {

    val settings: StateFlow<Settings?> = prefs.settings.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _ui = MutableStateFlow(restore())
    val ui: StateFlow<UrgeUi> = _ui.asStateFlow()

    init {
        val requested = saved.get<Int>("minutes")?.takeIf { it > 0 }
        val auto = saved.get<Boolean>("auto") == true
        if (_ui.value.stage == UrgeStage.Choose) {
            if (requested != null) _ui.update { it.copy(minutes = requested) }
            else viewModelScope.launch { val m = prefs.current().urgeMinutes; _ui.update { it.copy(minutes = m) } }
            if (auto && requested != null) start()
        }
    }

    fun now() = SystemClock.elapsedRealtime()

    fun choose(minutes: Int) = _ui.update { it.copy(minutes = minutes) }

    fun start() {
        val minutes = _ui.value.minutes
        set(UrgeUi(UrgeStage.Running, minutes, UrgeTimer.start(minutes, now())))
        viewModelScope.launch {
            prefs.update { it.copy(urgeMinutes = minutes, lastMode = Mode.Urge) }
            val id = sessions.start(Mode.Urge, minutes)
            set(_ui.value.copy(sessionId = id))
        }
    }

    fun pause() = _ui.value.timer?.let { set(_ui.value.copy(timer = it.pause(now()))) }
    fun resume() = _ui.value.timer?.let { set(_ui.value.copy(timer = it.resume(now()))) }
    fun extend() = _ui.value.timer?.let {
        set(_ui.value.copy(timer = it.resume(now()).extend(UrgeTimer.EXTEND_MINUTES * 60_000L)))
    }

    /** Finished or ended early: ask how it went (optional). */
    fun toOutcome() = set(_ui.value.copy(stage = UrgeStage.Outcome))

    fun answer(outcome: Outcome) {
        val ui = _ui.value
        val planned = ui.timer?.let { (it.durationMillis / 60_000L).toInt() }
        ui.sessionId?.let { id -> viewModelScope.launch { sessions.finish(id, outcome, planned) } }
        set(ui.copy(outcome = outcome, stage = if (outcome == Outcome.StillHere) UrgeStage.StillHere else UrgeStage.CheckIn))
    }

    fun toCheckIn() = set(_ui.value.copy(stage = UrgeStage.CheckIn))

    fun checkIn(intensity: Int?) {
        val id = _ui.value.sessionId
        if (intensity != null && id != null) viewModelScope.launch { sessions.setIntensity(id, intensity) }
        set(_ui.value.copy(stage = UrgeStage.Done))
    }

    /** Leaving mid-way still records the session as ended, with no outcome. */
    fun abandon() {
        val ui = _ui.value
        if (ui.stage == UrgeStage.Running) ui.sessionId?.let { id -> viewModelScope.launch { sessions.finish(id, null) } }
    }

    private fun set(ui: UrgeUi) {
        _ui.value = ui
        saved["stage"] = ui.stage.name
        saved["m"] = ui.minutes
        saved["sid"] = ui.sessionId ?: -1L
        saved["outcome"] = ui.outcome?.name
        ui.timer.let { t ->
            saved["t_dur"] = t?.durationMillis ?: -1L
            saved["t_start"] = t?.startedAt ?: -1L
            saved["t_paused"] = t?.pausedAt ?: -1L
            saved["t_ptotal"] = t?.pausedTotal ?: 0L
        }
    }

    private fun restore(): UrgeUi {
        val stage = saved.get<String>("stage")?.let { s -> UrgeStage.entries.firstOrNull { it.name == s } } ?: return UrgeUi()
        val dur = saved.get<Long>("t_dur") ?: -1L
        val timer = if (dur > 0) UrgeTimer(
            durationMillis = dur,
            startedAt = saved.get<Long>("t_start") ?: 0L,
            pausedAt = saved.get<Long>("t_paused")?.takeIf { it >= 0 },
            pausedTotal = saved.get<Long>("t_ptotal") ?: 0L,
        ) else null
        return UrgeUi(
            stage = stage,
            minutes = saved.get<Int>("m") ?: 10,
            timer = timer,
            sessionId = saved.get<Long>("sid")?.takeIf { it > 0 },
            outcome = saved.get<String>("outcome")?.let { o -> Outcome.entries.firstOrNull { it.name == o } },
        )
    }
}
