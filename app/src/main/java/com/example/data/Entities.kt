package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "wallets")
data class WalletEntity(
    @PrimaryKey val id: String, // "FNB", "CASH", "AIRTEL", "GLOBAL_CARD"
    val displayName: String,
    val balance: Double,
    val allocatedAmount: Double,
    val isLocked: Boolean,
    val lastTouchedTimestamp: Long,
    val description: String
)

@Entity(tableName = "log_entries")
data class LogEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val walletId: String,
    val amount: Double,
    val type: String, // "CREDIT", "DEBIT", "ALLOCATION"
    val category: String, // "Salary", "Transport", "Food", "Spotify", "Savings", "Haircut", "Emergency", "Other"
    val description: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isEmergency: Boolean = false,
    val emergencyReason: String? = null
)

@Entity(tableName = "payday_config")
data class PaydayConfigEntity(
    @PrimaryKey val id: Int = 1,
    val salaryAmount: Double = 3500.0,
    val fnbAllocation: Double = 1500.0,
    val transportAllocation: Double = 540.0,
    val spotifyAllocation: Double = 100.0,
    val isAllocatedThisMonth: Boolean = false,
    val lastPaydayTimestamp: Long = 0L
)

@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey val id: Int = 1,
    val title: String = "December Target",
    val targetAmount: Double = 7500.0,
    val deadlineText: String = "ZMW 7,500 by November",
    val startMonthTimestamp: Long = 0L
)

@Entity(tableName = "alert_logs")
data class AlertLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val type: String // "WARNING", "INFO", "SUCCESS"
)
