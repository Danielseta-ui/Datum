package com.example.data

import android.content.Context
import androidx.room.*

@Database(
    entities = [
        WalletEntity::class,
        LogEntryEntity::class,
        PaydayConfigEntity::class,
        GoalEntity::class,
        AlertLogEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class DatumDatabase : RoomDatabase() {
    abstract fun walletDao(): WalletDao
    abstract fun logEntryDao(): LogEntryDao
    abstract fun paydayConfigDao(): PaydayConfigDao
    abstract fun goalDao(): GoalDao
    abstract fun alertLogDao(): AlertLogDao

    companion object {
        @Volatile
        private var INSTANCE: DatumDatabase? = null

        fun getDatabase(context: Context): DatumDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    DatumDatabase::class.java,
                    "datum_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
