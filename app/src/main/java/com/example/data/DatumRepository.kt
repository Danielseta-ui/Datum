package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

class DatumRepository(private val database: DatumDatabase) {
    private val walletDao = database.walletDao()
    private val logEntryDao = database.logEntryDao()
    private val paydayConfigDao = database.paydayConfigDao()
    private val goalDao = database.goalDao()
    private val alertLogDao = database.alertLogDao()

    val allWallets: Flow<List<WalletEntity>> = walletDao.getAllWallets()
    val allLogEntries: Flow<List<LogEntryEntity>> = logEntryDao.getAllLogEntries()
    val paydayConfig: Flow<PaydayConfigEntity?> = paydayConfigDao.getPaydayConfigFlow()
    val goal: Flow<GoalEntity?> = goalDao.getGoalFlow()
    val allAlertLogs: Flow<List<AlertLogEntity>> = alertLogDao.getAllAlertLogs()

    suspend fun getPaydayConfigDirect(): PaydayConfigEntity? = paydayConfigDao.getPaydayConfig()
    suspend fun getWalletById(id: String): WalletEntity? = walletDao.getWalletById(id)
    suspend fun getGoalDirect(): GoalEntity? = goalDao.getGoal()

    suspend fun ensureDefaultDataLoaded() {
        val existingWallets = walletDao.getAllWallets().firstOrNull() ?: emptyList()
        if (existingWallets.isEmpty()) {
            val defaultWallets = listOf(
                WalletEntity(
                    id = "FNB",
                    displayName = "FNB (Emergency)",
                    balance = 0.0,
                    allocatedAmount = 1500.0,
                    isLocked = true,
                    lastTouchedTimestamp = System.currentTimeMillis() - (18L * 24 * 60 * 60 * 1000), // Default FNB untouched for 18 days for alert triggering
                    description = "Locked savings vault. ZMW 1,500 monthly target."
                ),
                WalletEntity(
                    id = "CASH",
                    displayName = "Cash (Daily spend)",
                    balance = 0.0,
                    allocatedAmount = 1360.0,
                    isLocked = false,
                    lastTouchedTimestamp = System.currentTimeMillis(),
                    description = "Includes Transport, Food, and Haircuts."
                ),
                WalletEntity(
                    id = "AIRTEL",
                    displayName = "Airtel Money (Backup)",
                    balance = 0.0,
                    allocatedAmount = 0.0,
                    isLocked = false,
                    lastTouchedTimestamp = System.currentTimeMillis(),
                    description = "Optional buffer. Not counted in daily budgets."
                ),
                WalletEntity(
                    id = "GLOBAL_CARD",
                    displayName = "Global Card (Spotify)",
                    balance = 0.0,
                    allocatedAmount = 100.0,
                    isLocked = false,
                    lastTouchedTimestamp = System.currentTimeMillis(),
                    description = "Auto-deducted digital subscription vault."
                )
            )
            walletDao.insertWallets(defaultWallets)
        }

        val existingConfig = paydayConfigDao.getPaydayConfig()
        if (existingConfig == null) {
            paydayConfigDao.insertPaydayConfig(
                PaydayConfigEntity(
                    id = 1,
                    salaryAmount = 3500.0,
                    fnbAllocation = 1500.0,
                    transportAllocation = 540.0,
                    spotifyAllocation = 100.0,
                    isAllocatedThisMonth = false,
                    lastPaydayTimestamp = 0L
                )
            )
        }

        val existingGoal = goalDao.getGoal()
        if (existingGoal == null) {
            goalDao.insertGoal(
                GoalEntity(
                    id = 1,
                    title = "December Target",
                    targetAmount = 7500.0,
                    deadlineText = "ZMW 7,500 by November",
                    startMonthTimestamp = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun updateWallet(wallet: WalletEntity) {
        walletDao.updateWallet(wallet)
    }

    suspend fun updateWalletBalance(walletId: String, newBalance: Double) {
        val wallet = walletDao.getWalletById(walletId)
        if (wallet != null) {
            walletDao.updateWallet(wallet.copy(balance = newBalance, lastTouchedTimestamp = System.currentTimeMillis()))
        }
    }

    suspend fun insertLogEntry(entry: LogEntryEntity) {
        logEntryDao.insertLogEntry(entry)
    }

    suspend fun deleteLogEntry(id: Int) {
        logEntryDao.deleteLogEntryById(id)
    }

    suspend fun clearAllLogEntries() {
        logEntryDao.clearAllLogEntries()
    }

    suspend fun updatePaydayConfig(config: PaydayConfigEntity) {
        paydayConfigDao.insertPaydayConfig(config)
    }

    suspend fun updateGoal(goal: GoalEntity) {
        goalDao.insertGoal(goal)
    }

    suspend fun insertAlertLog(alert: AlertLogEntity) {
        alertLogDao.insertAlertLog(alert)
    }

    suspend fun clearAllAlerts() {
        alertLogDao.clearAllAlerts()
    }
}
