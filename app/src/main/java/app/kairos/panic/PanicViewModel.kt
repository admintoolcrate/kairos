package app.kairos.panic

import android.content.Context
import android.telephony.TelephonyManager
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.kairos.data.CardRepository
import app.kairos.data.ContactEntity
import app.kairos.data.ContactRepository
import app.kairos.data.Defaults
import app.kairos.data.Mode
import app.kairos.data.Prefs
import app.kairos.data.SessionRepository
import app.kairos.data.Settings
import app.kairos.engine.CrisisContacts
import app.kairos.engine.CrisisLines
import app.kairos.engine.GroundingWalkthrough
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

enum class PanicPage(val label: String) { Breathe("Breathe"), Ground("Ground"), Words("Words"), Call("Call") }

/**
 * Everything here starts from built-in defaults so the first frame never waits on
 * disk; the user's own cards, contacts and timings replace them a moment later.
 */
@HiltViewModel
class PanicViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefs: Prefs,
    cards: CardRepository,
    contacts: ContactRepository,
    private val sessions: SessionRepository,
    private val saved: SavedStateHandle,
) : ViewModel() {

    val settings: StateFlow<Settings> = prefs.settings.stateIn(viewModelScope, SharingStarted.Eagerly, Settings())
    val cardTexts: StateFlow<List<String>> = cards.texts.stateIn(viewModelScope, SharingStarted.Eagerly, Defaults.reassuranceCards)
    val contacts: StateFlow<List<ContactEntity>> = contacts.contacts.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val crisis: StateFlow<CrisisContacts> = prefs.settings
        .map { CrisisLines.resolve(it.regionOverride.ifBlank { detectRegion() }, it.crisisOverride) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, CrisisLines.resolve(detectRegion()))

    val page = MutableStateFlow(saved.get<String>("page")?.let { p -> PanicPage.entries.firstOrNull { it.name == p } } ?: PanicPage.Breathe)
    val grounding = MutableStateFlow(GroundingWalkthrough(saved.get<Int>("ground") ?: 0))
    val checkingIn = MutableStateFlow(saved.get<Boolean>("checkin") ?: false)

    private var sessionId: Long? = saved.get<Long>("sid")

    init {
        if (sessionId == null) viewModelScope.launch {
            prefs.update { it.copy(lastMode = Mode.Panic) }
            sessionId = sessions.start(Mode.Panic).also { saved["sid"] = it }
        }
    }

    fun go(p: PanicPage) { page.value = p; saved["page"] = p.name }

    fun ground(change: (GroundingWalkthrough) -> GroundingWalkthrough) {
        grounding.value = change(grounding.value)
        saved["ground"] = grounding.value.index
    }

    fun imOkay() {
        sessionId?.let { id -> viewModelScope.launch { sessions.finish(id) } }
        checkingIn.value = true
        saved["checkin"] = true
    }

    fun checkIn(intensity: Int?) {
        val id = sessionId
        if (intensity != null && id != null) viewModelScope.launch { sessions.setIntensity(id, intensity) }
    }

    /** Leaving without saying "I'm okay" still closes the session, with nothing asked. */
    fun leave() {
        sessionId?.let { id -> viewModelScope.launch { sessions.finish(id) } }
    }

    private fun detectRegion(): String? {
        val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
        return listOfNotNull(
            runCatching { tm?.networkCountryIso }.getOrNull(),
            runCatching { tm?.simCountryIso }.getOrNull(),
            Locale.getDefault().country,
        ).firstOrNull { it.length == 2 }
    }
}
