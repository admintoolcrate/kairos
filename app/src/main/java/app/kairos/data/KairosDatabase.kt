package app.kairos.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters

class Converters {
    @TypeConverter fun modeToString(m: Mode): String = m.name
    @TypeConverter fun stringToMode(s: String): Mode = Mode.valueOf(s)
    @TypeConverter fun outcomeToString(o: Outcome?): String? = o?.name
    @TypeConverter fun stringToOutcome(s: String?): Outcome? = s?.let { runCatching { Outcome.valueOf(it) }.getOrNull() }
}

@Database(
    entities = [SessionEntity::class, CardEntity::class, ContactEntity::class],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class KairosDatabase : RoomDatabase() {
    abstract fun sessions(): SessionDao
    abstract fun cards(): CardDao
    abstract fun contacts(): ContactDao

    companion object {
        const val NAME = "kairos.db"
    }
}
