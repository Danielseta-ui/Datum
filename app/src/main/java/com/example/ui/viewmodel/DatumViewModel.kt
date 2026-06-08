package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

class DatumViewModel(application: Application, private val repository: DatumRepository) : AndroidViewModel(application) {

    // Main UI Flows
    val wallets: StateFlow<List<WalletEntity>> = repository.allWallets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val logEntries: StateFlow<List<LogEntryEntity>> = repository.allLogEntries
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val paydayConfig: StateFlow<PaydayConfigEntity?> = repository.paydayConfig
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val goal: StateFlow<GoalEntity?> = repository.goal
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val alerts: StateFlow<List<AlertLogEntity>> = repository.allAlertLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active Tab Navigation
    private val _currentTab = MutableStateFlow("dashboard")
    val currentTab: StateFlow<String> = _currentTab.asStateFlow()

    // Payday Step Manager (for Payday Planner)
    private val _paydayStep = MutableStateFlow(1) // 1: Input Salary, 2: Allocation Confirm, 3: Success
    val paydayStep: StateFlow<Int> = _paydayStep.asStateFlow()

    // Pending Emergency Transaction Hold State
    private val _pendingEmergencyTransaction = MutableStateFlow<PendingTransaction?>(null)
    val pendingEmergencyTransaction: StateFlow<PendingTransaction?> = _pendingEmergencyTransaction.asStateFlow()

    // System Warning Messages for Rules Screen or Snackbars
    private val _toastMessage = MutableSharedFlow<String>()
    val toastMessage = _toastMessage.asSharedFlow()

    data class PendingTransaction(
        val walletId: String,
        val amount: Double,
        val category: String,
        val description: String
    )

    init {
        viewModelScope.launch {
            repository.ensureDefaultDataLoaded()
            triggerRuleInference()
        }
    }

    fun setTab(tab: String) {
        _currentTab.value = tab
    }

    fun setPaydayStep(step: Int) {
        _paydayStep.value = step
    }

    // Spend / Deposit Action (Manual spending with Rules Enforcement)
    fun processTransaction(
        walletId: String,
        amount: Double,
        type: String, // "CREDIT", "DEBIT"
        category: String,
        description: String
    ) {
        viewModelScope.launch {
            val wallet = wallets.value.find { it.id == walletId } ?: return@launch

            if (type == "DEBIT") {
                // RULE 1: FNB Locked Savings Gatekeeper
                if (walletId == "FNB") {
                    // Open the emergency rule confirmation dialog
                    _pendingEmergencyTransaction.value = PendingTransaction(
                        walletId = walletId,
                        amount = amount,
                        category = category,
                        description = description
                    )
                    return@launch
                }

                // Balance depletion checker for non-FNB
                if (wallet.balance < amount) {
                    _toastMessage.emit("Declined: Insufficient funds in ${wallet.displayName}!")
                    // Insert alert log for overspending
                    repository.insertAlertLog(
                        AlertLogEntity(
                            title = "Funds Exhausted",
                            message = "Tried to spend ZMW $amount in ${wallet.id} but balance is code-red low (ZMW ${wallet.balance}).",
                            type = "WARNING"
                        )
                    )
                    return@launch
                }
            }

            // Normal Execution
            executeTransaction(walletId, amount, type, category, description, isEmergency = false)
        }
    }

    // Executes verified or bypassed transactions
    private suspend fun executeTransaction(
        walletId: String,
        amount: Double,
        type: String,
        category: String,
        description: String,
        isEmergency: Boolean,
        emergencyReason: String? = null
    ) {
        val wallet = wallets.value.find { it.id == walletId } ?: return
        val newBalance = if (type == "CREDIT") wallet.balance + amount else wallet.balance - amount

        // Update Wallet Balance
        repository.updateWallet(wallet.copy(
            balance = newBalance,
            lastTouchedTimestamp = System.currentTimeMillis()
        ))

        // Create log entry
        repository.insertLogEntry(
            LogEntryEntity(
                walletId = walletId,
                amount = amount,
                type = type,
                category = category,
                description = description,
                timestamp = System.currentTimeMillis(),
                isEmergency = isEmergency,
                emergencyReason = emergencyReason
            )
        )

        // Clear pending dialogs
        _pendingEmergencyTransaction.value = null

        // Refresh alerts and rules
        triggerRuleInference()

        if (isEmergency) {
            _toastMessage.emit("Emergency Override Approved. ZMW $amount withdrawn.")
        } else {
            _toastMessage.emit("Transaction recorded successfully.")
        }
    }

    // Handles user response to FNB Emergency rules dialog
    fun confirmEmergencyAction(approved: Boolean, reason: String? = null) {
        viewModelScope.launch {
            val pending = _pendingEmergencyTransaction.value ?: return@launch
            if (approved && reason != null) {
                val fnbWallet = wallets.value.find { it.id == "FNB" } ?: return@launch
                if (fnbWallet.balance < pending.amount) {
                    _toastMessage.emit("Declined: Even emergency funds are insufficient!")
                    _pendingEmergencyTransaction.value = null
                    return@launch
                }

                // Log Emergency Withdrawal
                executeTransaction(
                    walletId = pending.walletId,
                    amount = pending.amount,
                    type = "DEBIT",
                    category = "Emergency",
                    description = pending.description,
                    isEmergency = true,
                    emergencyReason = reason
                )

                // Log immediate safety intervention alert
                repository.insertAlertLog(
                    AlertLogEntity(
                        title = "Emergency Withdrawal Logged",
                        message = "FNB Vault tapped for ZMW ${pending.amount}. Reason: $reason",
                        type = "WARNING"
                    )
                )
            } else {
                // Denied or User canceled
                _pendingEmergencyTransaction.value = null
                _toastMessage.emit("Discipline reinforced. Transaction canceled.")
                repository.insertAlertLog(
                    AlertLogEntity(
                        title = "Vault Locked",
                        message = "FNB withdrawal canceled. Lockdown enforced. Your savings remain safe.",
                        type = "SUCCESS"
                    )
                )
            }
        }
    }

    fun dismissEmergencyDialog() {
        _pendingEmergencyTransaction.value = null
    }

    // Payday Allocation Sequence
    fun runPaydayAllocation(salaryAmount: Double, fnb: Double, transport: Double, spotify: Double) {
        viewModelScope.launch {
            // Calculate remaining for cash
            val remainingCash = salaryAmount - fnb - transport - spotify
            if (remainingCash < 0) {
                _toastMessage.emit("Error: Allocations cannot exceed custom salary!")
                return@launch
            }

            // Update Payday State config
            val config = paydayConfig.value?.copy(
                salaryAmount = salaryAmount,
                fnbAllocation = fnb,
                transportAllocation = transport,
                spotifyAllocation = spotify,
                isAllocatedThisMonth = true,
                lastPaydayTimestamp = System.currentTimeMillis()
            ) ?: PaydayConfigEntity(
                id = 1,
                salaryAmount = salaryAmount,
                fnbAllocation = fnb,
                transportAllocation = transport,
                spotifyAllocation = spotify,
                isAllocatedThisMonth = true,
                lastPaydayTimestamp = System.currentTimeMillis()
            )
            repository.updatePaydayConfig(config)

            // Inject Money into and lock categories in Wallets
            val currentWallets = wallets.value
            currentWallets.forEach { wallet ->
                val addition = when (wallet.id) {
                    "FNB" -> fnb
                    "CASH" -> remainingCash + transport // transport enters Cash wallet but designated as transport log
                    "GLOBAL_CARD" -> spotify
                    else -> 0.0
                }
                repository.updateWallet(wallet.copy(
                    balance = wallet.balance + addition,
                    allocatedAmount = addition,
                    lastTouchedTimestamp = System.currentTimeMillis()
                ))
            }

            // Create ledger logs for payday auto-allocation
            repository.insertLogEntry(
                LogEntryEntity(
                    walletId = "CASH",
                    amount = salaryAmount,
                    type = "CREDIT",
                    category = "Salary",
                    description = "Monthly Payday Salary Deposit (Auto-allocated)"
                )
            )

            // Log ledger allocations
            repository.insertLogEntry(
                LogEntryEntity(
                    walletId = "FNB",
                    amount = fnb,
                    type = "ALLOCATION",
                    category = "Savings",
                    description = "Allocated to Emergency Fund"
                )
            )
            repository.insertLogEntry(
                LogEntryEntity(
                    walletId = "GLOBAL_CARD",
                    amount = spotify,
                    type = "ALLOCATION",
                    category = "Spotify",
                    description = "Allocated to Subscriptions"
                )
            )

            // Log system alert
            repository.insertAlertLog(
                AlertLogEntity(
                    title = "Payday Enforced",
                    message = "ZMW $salaryAmount successfully channeled. FNB +$fnb, Cash +${remainingCash + transport}, Spotify +$spotify. Wallets locked.",
                    type = "SUCCESS"
                )
            )

            // Set Step to Success
            _paydayStep.value = 3
            _toastMessage.emit("Payday allocation completed!")
            triggerRuleInference()
        }
    }

    // Reset payday states for testing/re-running payday sequence
    fun resetPaydayState() {
        viewModelScope.launch {
            val currentConfig = paydayConfig.value ?: return@launch
            repository.updatePaydayConfig(currentConfig.copy(isAllocatedThisMonth = false))
            _paydayStep.value = 1
            _toastMessage.emit("Payday Planner unlocked.")
        }
    }

    // Clear complete ledger & reset wallets (Factory Reset)
    fun factoryReset() {
        viewModelScope.launch {
            repository.clearAllLogEntries()
            repository.clearAllAlerts()
            
            // Re-initialize default wallets to 0
            val initialWallets = listOf(
                WalletEntity("FNB", "FNB (Emergency)", 0.0, 1500.0, true, System.currentTimeMillis() - (18L * 24 * 60 * 60 * 1000), "Locked savings vault. ZMW 1,500 monthly target."),
                WalletEntity("CASH", "Cash (Daily spend)", 0.0, 1360.0, false, System.currentTimeMillis(), "Includes Transport, Food, and Haircuts."),
                WalletEntity("AIRTEL", "Airtel Money (Backup)", 0.0, 0.0, false, System.currentTimeMillis(), "Optional buffer. Not counted in daily budgets."),
                WalletEntity("GLOBAL_CARD", "Global Card (Spotify)", 0.0, 100.0, false, System.currentTimeMillis(), "Auto-deducted digital subscription vault.")
            )
            initialWallets.forEach { repository.updateWallet(it) }

            repository.updatePaydayConfig(PaydayConfigEntity(id = 1, isAllocatedThisMonth = false))
            
            repository.insertAlertLog(
                AlertLogEntity(
                    title = "Database Cleaned",
                    message = "System reset completed. Datum system is online & ready.",
                    type = "SUCCESS"
                )
            )
            _paydayStep.value = 1
            _toastMessage.emit("Database values reset successfully.")
            triggerRuleInference()
        }
    }

    // Goal Updates
    fun updateGoalTarget(newTarget: Double, text: String) {
        viewModelScope.launch {
            val current = goal.value ?: GoalEntity()
            repository.updateGoal(current.copy(targetAmount = newTarget, deadlineText = text))
            _toastMessage.emit("Goal updated to ZMW $newTarget.")
            triggerRuleInference()
        }
    }

    // Infer rules instantly to update Alerts Log
    private suspend fun triggerRuleInference() {
        val currentWallets = wallets.value
        val logs = logEntries.value

        val cashWallet = currentWallets.find { it.id == "CASH" }
        val fnbWallet = currentWallets.find { it.id == "FNB" }
        val globalCard = currentWallets.find { it.id == "GLOBAL_CARD" }

        // Determine if alert logs need insertion
        if (cashWallet != null) {
            // 1. Weekly spending limit warning
            val weeklyAllowance = calculateWeeklyAllowance()
            val spentThisWeek = calculateSpentThisWeek()
            if (weeklyAllowance > 0 && spentThisWeek > (weeklyAllowance * 0.8)) {
                repository.insertAlertLog(
                    AlertLogEntity(
                        title = "Approaching Weekly Limit",
                        message = "You have spent ZMW ${String.format("%.2f", spentThisWeek)} of your ZMW ${String.format("%.2f", weeklyAllowance)} weekly allowance.",
                        type = "WARNING"
                    )
                )
            }

            // 2. Transport budget warning
            val currentTransportSpent = logs.filter { it.walletId == "CASH" && it.category == "Transport" && it.type == "DEBIT" }.sumOf { it.amount }
            if (currentTransportSpent > 450.0) {
                repository.insertAlertLog(
                    AlertLogEntity(
                        title = "Transport Budget High Alert",
                        message = "Transport expenses have consumed ZMW ${String.format("%.2f", currentTransportSpent)} out of ZMW 540 allocation.",
                        type = "WARNING"
                    )
                )
            }
        }

        // 3. FNB savings untouched 18 days notice
        if (fnbWallet != null) {
            val millisSinceTouch = System.currentTimeMillis() - fnbWallet.lastTouchedTimestamp
            val daysSinceTouch = millisSinceTouch / (24 * 60 * 60 * 1000)
            if (daysSinceTouch >= 18) {
                repository.insertAlertLog(
                    AlertLogEntity(
                        title = "Good Savings Discipline",
                        message = "FNB emergency vault remains solid and untouched for $daysSinceTouch consecutive days! Keep it up.",
                        type = "SUCCESS"
                    )
                )
            }
        }

        // 4. Spotify subscription upcoming alert
        if (globalCard != null && globalCard.balance < 100.0) {
            repository.insertAlertLog(
                AlertLogEntity(
                    title = "Spotify Sub Alert Due Soon",
                    message = "Global Card balance (ZMW ${globalCard.balance}) is lower than the ZMW 100.0 monthly Spotify sub.",
                    type = "WARNING"
                )
            )
        }
    }

    // Helpers to compute weekly tracker
    fun calculateWeeklyAllowance(): Double {
        val wallet = wallets.value.find { it.id == "CASH" } ?: return 0.0
        // Standard Weekly allowance is cash balance / 4 (or dynamic between 290 and 325)
        // If they have standard ZMW 1360 allocations, splitting by 4.4 gives exactly 309 ZMW.
        // Let's implement dynamic calculation: Cash balance / remaining weeks (default 4.0 if payday config is empty)
        val balance = wallet.balance
        return if (balance <= 0.0) 0.0 else (balance / 4.4).coerceIn(280.0, 350.0)
    }

    fun calculateSpentThisWeek(): Double {
        // Collect transactions for CASH wallet that are DEBIT and happened in the last 7 days
        val offset = 7L * 24 * 60 * 60 * 1000
        val startTime = System.currentTimeMillis() - offset
        return logEntries.value
            .filter { it.walletId == "CASH" && it.type == "DEBIT" && it.timestamp >= startTime }
            .sumOf { it.amount }
    }
}

// ViewModelFactory
class DatumViewModelFactory(private val application: Application, private val repository: DatumRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DatumViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DatumViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
