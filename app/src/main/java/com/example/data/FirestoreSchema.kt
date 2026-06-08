package com.example.data

/**
 * Firestore Database Schema Mapping
 *
 * This file contains the complete Firestore document schema design for the Datum application.
 * It is structured to allow seamless real-time syncing between the local Room database
 * and Firebase Firestore, keeping user wallets, transactions, limits, and configurations secure.
 * 
 * Using standard JVM primitives (Long millisecond integers) ensures 100% compile-safety,
 * fast performance, and hassle-free serialization across Firebase SDKs.
 */

// ==========================================
// 1. ROOT COLLECTION: /users/{userId}
// ==========================================
data class UserFirestoreDocument(
    val email: String = "",
    val displayName: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val lastSyncTimestamp: Long = System.currentTimeMillis(),
    
    // Quick-access summaries for emergency fund and limits (Read Optimization)
    val emergencyFundBalance: Double = 0.0, // Aggregated balance of lock/secured funds (e.g., FNB Vault)
    val weeklySpendingLimit: Double = 0.0, // Weekly safe budget cap
    val remainingWeeklyAllowance: Double = 0.0 // Dynamic current safe spending remainder
)

// ==========================================
// 2. SUB-COLLECTION: /users/{userId}/wallets/{walletId}
// ==========================================
data class WalletFirestoreDocument(
    val walletId: String = "", // "FNB", "CASH", "AIRTEL", "GLOBAL_CARD"
    val displayName: String = "",
    val balance: Double = 0.0,
    val allocatedAmount: Double = 0.0,
    val isLocked: Boolean = false,
    val description: String = "",
    val lastTouchedTimestamp: Long = System.currentTimeMillis()
) {
    // Helper function to map from Room Entity to Firestore Document
    companion object {
        fun fromRoomEntity(entity: WalletEntity): WalletFirestoreDocument {
            return WalletFirestoreDocument(
                walletId = entity.id,
                displayName = entity.displayName,
                balance = entity.balance,
                allocatedAmount = entity.allocatedAmount,
                isLocked = entity.isLocked,
                description = entity.description,
                lastTouchedTimestamp = entity.lastTouchedTimestamp
            )
        }
    }
    
    // Helper function to map from Firestore Document to Room Entity
    fun toRoomEntity(): WalletEntity {
        return WalletEntity(
            id = walletId,
            displayName = displayName,
            balance = balance,
            allocatedAmount = allocatedAmount,
            isLocked = isLocked,
            description = description,
            lastTouchedTimestamp = lastTouchedTimestamp
        )
    }
}

// ==========================================
// 3. SUB-COLLECTION: /users/{userId}/transactions/{transactionId}
// ==========================================
data class TransactionFirestoreDocument(
    val transactionId: String = "", // Map to local auto ID or UUID
    val walletId: String = "", // Associated wallet e.g., "CASH"
    val amount: Double = 0.0,
    val type: String = "DEBIT", // "CREDIT", "DEBIT", "ALLOCATION"
    val category: String = "Other", // "Salary", "Transport", "Food", "Spotify", "Savings", "Emergency"
    val description: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isEmergency: Boolean = false,
    val emergencyReason: String? = null
) {
    companion object {
        fun fromRoomEntity(entity: LogEntryEntity, customDocId: String): TransactionFirestoreDocument {
            return TransactionFirestoreDocument(
                transactionId = customDocId,
                walletId = entity.walletId,
                amount = entity.amount,
                type = entity.type,
                category = entity.category,
                description = entity.description,
                timestamp = entity.timestamp,
                isEmergency = entity.isEmergency,
                emergencyReason = entity.emergencyReason
            )
        }
    }

    fun toRoomEntity(localId: Int = 0): LogEntryEntity {
        return LogEntryEntity(
            id = localId,
            walletId = walletId,
            amount = amount,
            type = type,
            category = category,
            description = description,
            timestamp = timestamp,
            isEmergency = isEmergency,
            emergencyReason = emergencyReason
        )
    }
}

// ==========================================
// 4. SUB-COLLECTION: /users/{userId}/configs/{configId}
// ==========================================
data class PaydayConfigFirestoreDocument(
    val salaryAmount: Double = 3500.0,
    val fnbAllocation: Double = 1500.0,
    val transportAllocation: Double = 540.0,
    val spotifyAllocation: Double = 100.0,
    val isAllocatedThisMonth: Boolean = false,
    val lastPaydayTimestamp: Long = 0L
) {
    companion object {
        fun fromRoomEntity(entity: PaydayConfigEntity): PaydayConfigFirestoreDocument {
            return PaydayConfigFirestoreDocument(
                salaryAmount = entity.salaryAmount,
                fnbAllocation = entity.fnbAllocation,
                transportAllocation = entity.transportAllocation,
                spotifyAllocation = entity.spotifyAllocation,
                isAllocatedThisMonth = entity.isAllocatedThisMonth,
                lastPaydayTimestamp = entity.lastPaydayTimestamp
            )
        }
    }

    fun toRoomEntity(): PaydayConfigEntity {
        return PaydayConfigEntity(
            id = 1,
            salaryAmount = salaryAmount,
            fnbAllocation = fnbAllocation,
            transportAllocation = transportAllocation,
            spotifyAllocation = spotifyAllocation,
            isAllocatedThisMonth = isAllocatedThisMonth,
            lastPaydayTimestamp = lastPaydayTimestamp
        )
    }
}
