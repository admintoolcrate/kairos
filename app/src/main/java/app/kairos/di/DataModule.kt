package app.kairos.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import app.kairos.data.Defaults
import app.kairos.data.KairosDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {
    /** Created once per process by Hilt, on first use, never at startup. */
    @Provides
    @Singleton
    fun dataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create { context.preferencesDataStoreFile("kairos") }

    /** Building is cheap; the file is only opened on the first query, off the main thread. */
    @Provides
    @Singleton
    fun database(@ApplicationContext context: Context): KairosDatabase =
        Room.databaseBuilder(context, KairosDatabase::class.java, KairosDatabase.NAME)
            .addCallback(object : RoomDatabase.Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    Defaults.reassuranceCards.forEachIndexed { i, text ->
                        db.execSQL("INSERT INTO reassurance_cards (text, position) VALUES (?, ?)", arrayOf<Any>(text, i))
                    }
                }
            })
            .build()
}
