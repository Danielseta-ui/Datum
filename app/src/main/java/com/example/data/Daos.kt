package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface WalletDao {
    @Query("SELECT * FROM wallets")
    fun getAllWallets(): Flow<List<WalletEntity>>

    @Query("SELECT * FROM wallets WHERE id = :id LIMIT 1")
    suspend fun getWalletById(id: String): WalletEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWallet(wallet: WalletEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWallets(wallets: List<WalletEntity>)

    @Update
    suspend fun updateWallet(wallet: WalletEntity)
}

@Dao
interface LogEntryDao {
    @Query("SELECT * FROM log_entries ORDER BY timestamp DESC")
    fun getAllLogEntries(): Flow<List<LogEntryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLogEntry(entry: LogEntryEntity)

    @Query("DELETE FROM log_entries WHERE id = :id")
    suspend fun deleteLogEntryById(id: Int)

    @Query("DELETE FROM log_entries")
    suspend fun clearAllLogEntries()
}

@Dao
interface PaydayConfigDao {
    @Query("SELECT * FROM payday_config WHERE id = 1 LIMIT 1")
    fun getPaydayConfigFlow(): Flow<PaydayConfigEntity?>

    @Query("SELECT * FROM payday_config WHERE id = 1 LIMIT 1")
    suspend fun getPaydayConfig(): PaydayConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPaydayConfig(config: PaydayConfigEntity)
}

@Dao
interface GoalDao {
    @Query("SELECT * FROM goals WHERE id = 1 LIMIT 1")
    fun getGoalFlow(): Flow<GoalEntity?>

    @Query("SELECT * FROM goals WHERE id = 1 LIMIT 1")
    suspend fun getGoal(): GoalEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: GoalEntity)
}

@Dao
interface AlertLogDao {
    @Query("SELECT * FROM alert_logs ORDER BY timestamp DESC")
    fun getAllAlertLogs(): Flow<List<AlertLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlertLog(alert: AlertLogEntity)

    @Query("DELETE FROM alert_logs")
    suspend fun clearAllAlerts()
}
