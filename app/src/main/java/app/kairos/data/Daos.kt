package app.kairos.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Insert suspend fun insert(session: SessionEntity): Long
    @Update suspend fun update(session: SessionEntity)
    @Query("SELECT * FROM sessions WHERE id = :id") suspend fun get(id: Long): SessionEntity?
    @Query("SELECT * FROM sessions ORDER BY startedAt DESC") fun observeAll(): Flow<List<SessionEntity>>
    @Query("SELECT * FROM sessions ORDER BY startedAt") suspend fun all(): List<SessionEntity>
    @Query("DELETE FROM sessions WHERE id = :id") suspend fun delete(id: Long)
    @Query("DELETE FROM sessions") suspend fun clear()
    @Insert suspend fun insertAll(sessions: List<SessionEntity>)
}

@Dao
interface CardDao {
    @Query("SELECT * FROM reassurance_cards ORDER BY position, id") fun observe(): Flow<List<CardEntity>>
    @Query("SELECT * FROM reassurance_cards ORDER BY position, id") suspend fun all(): List<CardEntity>
    @Insert suspend fun insert(card: CardEntity): Long
    @Insert suspend fun insertAll(cards: List<CardEntity>)
    @Update suspend fun update(card: CardEntity)
    @Update suspend fun updateAll(cards: List<CardEntity>)
    @Query("DELETE FROM reassurance_cards WHERE id = :id") suspend fun delete(id: Long)
    @Query("DELETE FROM reassurance_cards") suspend fun clear()

    @Transaction
    suspend fun replaceAll(cards: List<CardEntity>) {
        clear()
        insertAll(cards.mapIndexed { i, c -> c.copy(id = 0, position = i) })
    }
}

@Dao
interface ContactDao {
    @Query("SELECT * FROM contacts ORDER BY position, id") fun observe(): Flow<List<ContactEntity>>
    @Query("SELECT * FROM contacts ORDER BY position, id") suspend fun all(): List<ContactEntity>
    @Insert suspend fun insert(contact: ContactEntity): Long
    @Insert suspend fun insertAll(contacts: List<ContactEntity>)
    @Update suspend fun updateAll(contacts: List<ContactEntity>)
    @Query("DELETE FROM contacts WHERE id = :id") suspend fun delete(id: Long)
    @Query("DELETE FROM contacts") suspend fun clear()

    @Transaction
    suspend fun replaceAll(contacts: List<ContactEntity>) {
        clear()
        insertAll(contacts.mapIndexed { i, c -> c.copy(id = 0, position = i) })
    }
}
