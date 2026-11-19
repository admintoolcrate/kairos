package app.kairos.ui.settings

import android.content.Context
import android.telephony.TelephonyManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.kairos.data.CardEntity
import app.kairos.data.CardRepository
import app.kairos.data.ContactEntity
import app.kairos.data.ContactRepository
import app.kairos.data.LockSecrets
import app.kairos.data.Prefs
import app.kairos.data.Settings
import app.kairos.engine.CrisisContacts
import app.kairos.engine.CrisisLines
import app.kairos.lock.AppLock
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefs: Prefs,
    private val cardRepo: CardRepository,
    private val contactRepo: ContactRepository,
    val lock: AppLock,
) : ViewModel() {
    val settings: StateFlow<Settings?> = prefs.settings.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val secrets: StateFlow<LockSecrets?> = prefs.lock.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val cards: StateFlow<List<CardEntity>> = cardRepo.cards.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val contacts: StateFlow<List<ContactEntity>> = contactRepo.contacts.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val detectedRegion: String? get() {
        val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
        return listOfNotNull(
            runCatching { tm?.networkCountryIso }.getOrNull(),
            runCatching { tm?.simCountryIso }.getOrNull(),
            Locale.getDefault().country,
        ).firstOrNull { it.length == 2 }?.uppercase()
    }

    val crisis: StateFlow<CrisisContacts?> = prefs.settings
        .map { CrisisLines.resolve(it.regionOverride.ifBlank { detectedRegion }, it.crisisOverride) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun update(change: (Settings) -> Settings) = viewModelScope.launch { prefs.update(change) }

    fun addCard(text: String) = viewModelScope.launch { if (text.isNotBlank()) cardRepo.add(text) }
    fun editCard(card: CardEntity, text: String) = viewModelScope.launch { if (text.isNotBlank()) cardRepo.edit(card, text) }
    fun deleteCard(id: Long) = viewModelScope.launch { cardRepo.delete(id) }
    fun moveCard(id: Long, by: Int) = viewModelScope.launch { cardRepo.move(id, by) }
    fun restoreCards() = viewModelScope.launch { cardRepo.restoreDefaults() }

    fun addContact(name: String, phone: String) = viewModelScope.launch {
        if (phone.isNotBlank()) contactRepo.add(name.ifBlank { phone }, phone)
    }
    fun deleteContact(id: Long) = viewModelScope.launch { contactRepo.delete(id) }
    fun moveContact(id: Long, by: Int) = viewModelScope.launch { contactRepo.move(id, by) }

    fun setPin(pin: String, biometric: Boolean) = viewModelScope.launch { lock.setPin(pin, biometric) }
    fun setBiometric(on: Boolean) = viewModelScope.launch { secrets.value?.let { lock.setBiometric(on, it) } }
    fun lockOff() = viewModelScope.launch { lock.turnOff() }
}
