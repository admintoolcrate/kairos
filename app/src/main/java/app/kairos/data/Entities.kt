package app.kairos.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

enum class Mode { Urge, Panic }

/** How an urge session ended. "Still here" is a valid, unjudged answer. */
enum class Outcome { Passed, Eased, StillHere, Skipped }

@Serializable
@Entity(tableName = "sessions", indices = [Index("startedAt")])
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mode: Mode,
    /** Epoch millis. */
    val startedAt: Long,
    val endedAt: Long? = null,
    /** Urge only: 10 or 20 (plus any extensions). */
    val plannedMinutes: Int? = null,
    val outcome: Outcome? = null,
    /** Optional 1–5 check-in; null when skipped. */
    val intensity: Int? = null,
    val note: String? = null,
    val sentToStagira: Boolean = false,
)

@Serializable
@Entity(tableName = "reassurance_cards")
data class CardEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val text: String,
    val position: Int,
)

@Serializable
@Entity(tableName = "contacts")
data class ContactEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String,
    val position: Int,
)
