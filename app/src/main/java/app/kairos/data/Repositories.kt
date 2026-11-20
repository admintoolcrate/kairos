package app.kairos.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionRepository @Inject constructor(private val db: KairosDatabase) {
    private val dao get() = db.sessions()

    val history: Flow<List<SessionEntity>> get() = dao.observeAll()

    suspend fun start(mode: Mode, plannedMinutes: Int? = null, now: Long = System.currentTimeMillis()): Long =
        dao.insert(SessionEntity(mode = mode, startedAt = now, plannedMinutes = plannedMinutes))

    suspend fun finish(id: Long, outcome: Outcome? = null, plannedMinutes: Int? = null, now: Long = System.currentTimeMillis()) {
        val s = dao.get(id) ?: return
        dao.update(s.copy(endedAt = s.endedAt ?: now, outcome = outcome ?: s.outcome, plannedMinutes = plannedMinutes ?: s.plannedMinutes))
    }

    suspend fun setIntensity(id: Long, intensity: Int?) {
        val s = dao.get(id) ?: return
        dao.update(s.copy(intensity = intensity?.coerceIn(1, 5)))
    }

    suspend fun markSentToStagira(id: Long) {
        val s = dao.get(id) ?: return
        dao.update(s.copy(sentToStagira = true))
    }

    suspend fun delete(id: Long) = dao.delete(id)
}

@Singleton
class CardRepository @Inject constructor(private val db: KairosDatabase) {
    private val dao get() = db.cards()

    /** The user's cards, or the built-in ones if they've removed them all. */
    val texts: Flow<List<String>> get() = dao.observe().map { list -> list.map { it.text }.ifEmpty { Defaults.reassuranceCards } }
    val cards: Flow<List<CardEntity>> get() = dao.observe()

    suspend fun add(text: String) {
        val all = dao.all()
        dao.insert(CardEntity(text = text.trim(), position = (all.maxOfOrNull { it.position } ?: -1) + 1))
    }

    suspend fun edit(card: CardEntity, text: String) = dao.update(card.copy(text = text.trim()))
    suspend fun delete(id: Long) = dao.delete(id)

    suspend fun move(id: Long, by: Int) {
        val all = dao.all().toMutableList()
        val from = all.indexOfFirst { it.id == id }.takeIf { it >= 0 } ?: return
        val to = (from + by).coerceIn(0, all.lastIndex)
        all.add(to, all.removeAt(from))
        dao.updateAll(all.mapIndexed { i, c -> c.copy(position = i) })
    }

    suspend fun restoreDefaults() = dao.replaceAll(Defaults.reassuranceCards.map { CardEntity(text = it, position = 0) })
}

@Singleton
class ContactRepository @Inject constructor(private val db: KairosDatabase) {
    private val dao get() = db.contacts()

    val contacts: Flow<List<ContactEntity>> get() = dao.observe()

    suspend fun add(name: String, phone: String) {
        val all = dao.all()
        dao.insert(ContactEntity(name = name.trim(), phone = phone.trim(), position = (all.maxOfOrNull { it.position } ?: -1) + 1))
    }

    suspend fun delete(id: Long) = dao.delete(id)

    suspend fun move(id: Long, by: Int) {
        val all = dao.all().toMutableList()
        val from = all.indexOfFirst { it.id == id }.takeIf { it >= 0 } ?: return
        val to = (from + by).coerceIn(0, all.lastIndex)
        all.add(to, all.removeAt(from))
        dao.updateAll(all.mapIndexed { i, c -> c.copy(position = i) })
    }
}
