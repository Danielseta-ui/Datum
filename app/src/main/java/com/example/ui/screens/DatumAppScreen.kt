package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.*
import com.example.ui.viewmodel.DatumViewModel
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatumAppScreen(viewModel: DatumViewModel) {
    val wallets by viewModel.wallets.collectAsState()
    val logEntries by viewModel.logEntries.collectAsState()
    val paydayConfig by viewModel.paydayConfig.collectAsState()
    val goal by viewModel.goal.collectAsState()
    val alerts by viewModel.alerts.collectAsState()

    val currentTab by viewModel.currentTab.collectAsState()
    val paydayStep by viewModel.paydayStep.collectAsState()
    val pendingEmergencyTransaction by viewModel.pendingEmergencyTransaction.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // Dialog state for adding manual spend and custom goal target overrides
    var showAddSpendDialog by remember { mutableStateOf(false) }
    var selectedWalletIdForSpend by remember { mutableStateOf("CASH") }

    // Emergency rule reason dialog components
    var selectedEmergencyReasonOption by remember { mutableStateOf("") }
    var customEmergencyReasonText by remember { mutableStateOf("") }

    // Collect showing toast messages
    LaunchedEffect(key1 = true) {
        viewModel.toastMessage.collect { message ->
            snackbarHostState.showSnackbar(message, duration = SnackbarDuration.Short)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "DATUM",
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.SansSerif,
                            fontSize = 24.sp,
                            letterSpacing = (-1).sp,
                            color = M3Primary
                        )
                        Text(
                            text = "SYSTEM ACTIVE • JULY CYCLE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = FontFamily.SansSerif,
                            color = M3Secondary,
                            letterSpacing = 1.5.sp
                        )
                    }
                },
                actions = {
                    Box(
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .size(38.dp)
                            .background(M3PillActiveBg, RoundedCornerShape(19.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "JD",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = M3PillActiveText
                        )
                    }
                    IconButton(
                        onClick = { viewModel.factoryReset() },
                        modifier = Modifier.testTag("factory_reset_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset Database",
                            tint = Color.Gray.copy(alpha = 0.6f)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = M3Background
                )
            )
        },
        bottomBar = {
            Column {
                HorizontalDivider(color = M3Outline, thickness = 1.dp)
                NavigationBar(
                    containerColor = M3BottomNavBg,
                    tonalElevation = 0.dp,
                    modifier = Modifier.navigationBarsPadding()
                ) {
                    listOf(
                        NavigationItem("dashboard", Icons.Default.Dashboard, "Home", "nav_dashboard"),
                        NavigationItem("payday", Icons.Default.Payments, "Payday", "nav_payday"),
                        NavigationItem("weekly", Icons.Default.TrendingUp, "Weekly", "nav_weekly"),
                        NavigationItem("goals", Icons.Default.Star, "Goals", "nav_goals"),
                        NavigationItem("rules", Icons.Default.Security, "Rules", "nav_rules")
                    ).forEach { item ->
                        val isSelected = currentTab == item.id
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { viewModel.setTab(item.id) },
                            icon = { 
                                DuoToneIcon(
                                    imageVector = item.icon, 
                                    contentDescription = item.label,
                                    primaryColor = if (isSelected) M3PillActiveText else M3Secondary,
                                    secondaryColor = if (isSelected) M3Primary.copy(alpha = 0.25f) else M3Outline.copy(alpha = 0.4f),
                                    size = 20.dp
                                ) 
                            },
                            label = { 
                                Text(
                                    text = item.label, 
                                    fontSize = 10.sp, 
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) M3PillActiveText else M3Secondary
                                ) 
                            },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = M3PillActiveBg
                            ),
                            modifier = Modifier.testTag(item.testTag)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
        ) {
            when (currentTab) {
                "dashboard" -> DashboardScreen(
                    wallets = wallets,
                    logs = logEntries,
                    alerts = alerts,
                    viewModel = viewModel,
                    onOpenAddSpend = { walletId ->
                        selectedWalletIdForSpend = walletId
                        showAddSpendDialog = true
                    }
                )
                "payday" -> PaydayPlannerScreen(
                    config = paydayConfig,
                    viewModel = viewModel,
                    step = paydayStep
                )
                "weekly" -> WeeklyBudgetScreen(
                    wallets = wallets,
                    logs = logEntries,
                    viewModel = viewModel,
                    onAddExpense = {
                        selectedWalletIdForSpend = "CASH"
                        showAddSpendDialog = true
                    }
                )
                "goals" -> GoalsScreen(
                    wallets = wallets,
                    goal = goal,
                    viewModel = viewModel
                )
                "rules" -> RulesScreen(
                    wallets = wallets,
                    logs = logEntries,
                    viewModel = viewModel
                )
            }

            // MODAL 1: EMERGENCY RULES ENGINE CONFIRMATION DIALOG (Interpreting FNB)
            pendingEmergencyTransaction?.let { pending ->
                Dialog(onDismissRequest = { viewModel.dismissEmergencyDialog() }) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .testTag("emergency_rule_dialog")
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(28.dp))
                                    .background(MaterialTheme.colorScheme.error.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Blocked Savings",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(32.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "EMERGENCY RULE VIOLATION",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 16.sp,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "You are attempting to withdraw ZMW ${String.format("%.2f", pending.amount)} from FNB Locked Savings.",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )

                            Spacer(modifier = Modifier.height(16.dp))
                            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "Select Emergency Justification Profile:",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.align(Alignment.Start)
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Verification Options
                            val justifications = listOf(
                                "Critical Medical Necessity",
                                "Emergency Commute & Survival Transport",
                                "Survival Food (Exhausted Cash Balance)"
                            )

                            justifications.forEach { option ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (selectedEmergencyReasonOption == option)
                                                MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                                            else Color.Transparent
                                        )
                                        .clickable {
                                            selectedEmergencyReasonOption = option
                                            customEmergencyReasonText = option
                                        }
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = selectedEmergencyReasonOption == option,
                                        onClick = {
                                            selectedEmergencyReasonOption = option
                                            customEmergencyReasonText = option
                                        },
                                        colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.error)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = option,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        viewModel.confirmEmergencyAction(approved = false)
                                        selectedEmergencyReasonOption = ""
                                        customEmergencyReasonText = ""
                                    },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("emergency_rule_deny_button")
                                ) {
                                    Text("NO (Safe FNB)", fontSize = 11.sp, maxLines = 1)
                                }

                                Button(
                                    onClick = {
                                        if (customEmergencyReasonText.isNotEmpty()) {
                                            viewModel.confirmEmergencyAction(approved = true, reason = customEmergencyReasonText)
                                            selectedEmergencyReasonOption = ""
                                            customEmergencyReasonText = ""
                                        }
                                    },
                                    enabled = customEmergencyReasonText.isNotEmpty(),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("emergency_rule_approve_button")
                                ) {
                                    Text("YES (Withdraw)", fontSize = 11.sp, color = Color.White, maxLines = 1)
                                }
                            }
                        }
                    }
                }
            }

            // MODAL 2: STANDARD TRANSACTION INPUT DIALOG
            if (showAddSpendDialog) {
                var expenseAmount by remember { mutableStateOf("") }
                var expenseCategory by remember { mutableStateOf("Food") }
                var expenseDesc by remember { mutableStateOf("") }
                var isExpandCat by remember { mutableStateOf(false) }

                Dialog(onDismissRequest = { showAddSpendDialog = false }) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .testTag("add_spend_dialog")
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (selectedWalletIdForSpend == "FNB") "FNB Emergency Claim" else "Record Spend Transaction",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = if (selectedWalletIdForSpend == "FNB") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            )

                            Text(
                                text = "Wallet: $selectedWalletIdForSpend",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            OutlinedTextField(
                                value = expenseAmount,
                                onValueChange = { expenseAmount = it },
                                label = { Text("Amount (ZMW)") },
                                textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center, fontWeight = FontWeight.Bold),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("amount_input")
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = expenseDesc,
                                onValueChange = { expenseDesc = it },
                                label = { Text("Description") },
                                placeholder = { Text("e.g. Weekly Groceries, Bus fare") },
                                colors = OutlinedTextFieldDefaults.colors(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("desc_input")
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Categorizations
                            val categoryList = when (selectedWalletIdForSpend) {
                                "FNB" -> listOf("Emergency")
                                "GLOBAL_CARD" -> listOf("Spotify", "Subscriptions")
                                else -> listOf("Food", "Transport", "Haircut", "Subscriptions", "Other")
                            }

                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedButton(
                                    onClick = { isExpandCat = !isExpandCat },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("category_dropdown")
                                ) {
                                    Text("Category: $expenseCategory")
                                }
                                DropdownMenu(
                                    expanded = isExpandCat,
                                    onDismissRequest = { isExpandCat = false }
                                ) {
                                    categoryList.forEach { category ->
                                        DropdownMenuItem(
                                            text = { Text(category) },
                                            onClick = {
                                                expenseCategory = category
                                                isExpandCat = false
                                            }
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { showAddSpendDialog = false },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Cancel")
                                }

                                Button(
                                    onClick = {
                                        val amt = expenseAmount.toDoubleOrNull()
                                        if (amt != null && amt > 0.0) {
                                            viewModel.processTransaction(
                                                walletId = selectedWalletIdForSpend,
                                                amount = amt,
                                                type = "DEBIT",
                                                category = expenseCategory,
                                                description = expenseDesc.ifEmpty { "Manual $expenseCategory expense" }
                                            )
                                            showAddSpendDialog = false
                                        }
                                    },
                                    enabled = expenseAmount.isNotEmpty(),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("save_spend_button")
                                ) {
                                    Text("Submit")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// NAVIGATION ITEM MODEL
data class NavigationItem(val id: String, val icon: ImageVector, val label: String, val testTag: String)

// -------------------------------------------------------------
// SCREEN 1: THE MAIN METRIC DASHBOARD
// -------------------------------------------------------------
@Composable
fun DashboardScreen(
    wallets: List<WalletEntity>,
    logs: List<LogEntryEntity>,
    alerts: List<AlertLogEntity>,
    viewModel: DatumViewModel,
    onOpenAddSpend: (String) -> Unit
) {
    val weeklyAllowance = viewModel.calculateWeeklyAllowance()
    val spentThisWeek = viewModel.calculateSpentThisWeek()
    val weeklyRemaining = (weeklyAllowance - spentThisWeek).coerceAtLeast(0.0)

    val formatter = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("dashboard_scroll_view"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Core System Balance Banner (Modern Radial gradient look)
        item {
            val totalAssets = wallets.sumOf { it.balance }
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .testTag("total_balance_card")
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f),
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                                )
                            )
                        )
                        .padding(24.dp)
                ) {
                    Column {
                        Text(
                            text = "TOTAL SYSTEM POWERED ASSETS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "ZMW ${String.format("%,.2f", totalAssets)}",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Active Weekly Spend Budget",
                                    fontSize = 10.sp,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                                Text(
                                    text = "ZMW ${String.format("%.2f", weeklyAllowance)} / week",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White.copy(alpha = 0.15f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Lock, "Lock State", tint = Color.White, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Locked-In Plan", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4 Wallets Segment with custom actions
        item {
            Text(
                text = "YOUR SYSTEM WALLETS",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = M3Secondary,
                letterSpacing = 1.5.sp
            )
        }

        item {
            val fnb = wallets.find { it.id == "FNB" }
            val cash = wallets.find { it.id == "CASH" }
            val airtel = wallets.find { it.id == "AIRTEL" }
            val global = wallets.find { it.id == "GLOBAL_CARD" }

            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        if (fnb != null) {
                            WalletGridCard(wallet = fnb, onOpenAddSpend = onOpenAddSpend)
                        }
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        if (cash != null) {
                            WalletGridCard(wallet = cash, onOpenAddSpend = onOpenAddSpend)
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        if (airtel != null) {
                            WalletGridCard(wallet = airtel, onOpenAddSpend = onOpenAddSpend)
                        }
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        if (global != null) {
                            WalletGridCard(wallet = global, onOpenAddSpend = onOpenAddSpend)
                        }
                    }
                }
            }
        }

        // Live alert notification segment
        item {
            Text(
                text = "ACTIVE BEHAVIORAL FEED",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                letterSpacing = 1.sp
            )
        }

        if (alerts.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        DuoToneIcon(
                            imageVector = Icons.Default.VerifiedUser,
                            contentDescription = "OK",
                            primaryColor = M3Primary,
                            secondaryColor = M3Secondary.copy(alpha = 0.35f),
                            size = 24.dp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("All systems nominal. Discipline level: High.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        } else {
            items(alerts.take(4)) { alert ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("alert_item_${alert.id}")
                ) {
                    Row(
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    when (alert.type) {
                                        "WARNING" -> MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                                        "SUCCESS" -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                        else -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            val alertColor = when (alert.type) {
                                "WARNING" -> MaterialTheme.colorScheme.error
                                "SUCCESS" -> M3Primary
                                else -> M3Secondary
                            }
                            DuoToneIcon(
                                imageVector = when (alert.type) {
                                    "WARNING" -> Icons.Default.Warning
                                    "SUCCESS" -> Icons.Default.CheckCircle
                                    else -> Icons.Default.Info
                                },
                                contentDescription = alert.type,
                                primaryColor = alertColor,
                                secondaryColor = alertColor.copy(alpha = 0.35f),
                                size = 18.dp
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(alert.title, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text(alert.message, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        Text(
                            text = formatter.format(Date(alert.timestamp)),
                            fontSize = 8.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Weekly progress Bar (Dynamic Tracker)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("weekly_tracker_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("WEEKLY SPENDING RADAR", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                            Text("Dynamic balance relative to weekly limit.", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    if (weeklyRemaining > (weeklyAllowance * 0.2))
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                    else MaterialTheme.colorScheme.error.copy(alpha = 0.2f)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                if (weeklyRemaining > (weeklyAllowance * 0.2)) "ON TRACK" else "DANGER BREACH",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (weeklyRemaining > (weeklyAllowance * 0.2)) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    val percent = if (weeklyAllowance > 0) (spentThisWeek / weeklyAllowance).toFloat().coerceIn(0f, 1f) else 1f
                    LinearProgressIndicator(
                        progress = percent,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = if (percent > 0.8f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("SPENT THIS WEEK", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("ZMW ${String.format("%.2f", spentThisWeek)}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("REMAINING ALLOWANCE", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("ZMW ${String.format("%.2f", weeklyRemaining)}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// SCREEN 2: PAYDAY PLANNER SEQUENCE
// -------------------------------------------------------------
@Composable
fun PaydayPlannerScreen(
    config: PaydayConfigEntity?,
    viewModel: DatumViewModel,
    step: Int
) {
    var salaryInput by remember { mutableStateOf("3500") }
    var fnbAllocateInput by remember { mutableStateOf("1500") }
    var transportAllocateInput by remember { mutableStateOf("540") }
    var spotifyAllocateInput by remember { mutableStateOf("100") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
            .testTag("payday_planner_scroll"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
                .testTag("payday_workflow_card")
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                // Stepper Visualizer
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StepBubble(number = 1, text = "Salary", active = step == 1, completed = step > 1)
                    StepDivider()
                    StepBubble(number = 2, text = "Enforce", active = step == 2, completed = step > 2)
                    StepDivider()
                    StepBubble(number = 3, text = "Locked", active = step == 3, completed = step == 3)
                }

                Spacer(modifier = Modifier.height(24.dp))

                AnimatedContent(
                    targetState = step,
                    transitionSpec = { fadeIn() togetherWith fadeOut() }
                ) { currentStep ->
                    when (currentStep) {
                        1 -> {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    "STEP 1: SALARY INPUT",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    "Enter the monthly paycheck to be partitioned. The behavior engine will lock category boundaries once saved.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(modifier = Modifier.height(20.dp))

                                OutlinedTextField(
                                    value = salaryInput,
                                    onValueChange = { salaryInput = it },
                                    label = { Text("Base Salary (ZMW)") },
                                    leadingIcon = { Icon(Icons.Default.Payments, "K") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    textStyle = LocalTextStyle.current.copy(fontWeight = FontWeight.Bold, fontSize = 20.sp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("payday_salary_input")
                                )

                                Spacer(modifier = Modifier.height(24.dp))

                                Button(
                                    onClick = { viewModel.setPaydayStep(2) },
                                    enabled = salaryInput.toDoubleOrNull() ?: 0.0 > 0.0,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("payday_next_step_button")
                                ) {
                                    Text("Next: Split & Allocation", fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        2 -> {
                            val sal = salaryInput.toDoubleOrNull() ?: 3500.0
                            val f = fnbAllocateInput.toDoubleOrNull() ?: 1500.0
                            val t = transportAllocateInput.toDoubleOrNull() ?: 540.0
                            val s = spotifyAllocateInput.toDoubleOrNull() ?: 100.0
                            val remainingCashVal = sal - f - t - s

                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    "STEP 2: ENFORCE AUTO-ALLOCATIONS",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    "Enforce rules immediately. Locked categories are strictly reserved. Remaining balance automatically channels to Daily Cash Wallet.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                AllocationRow(label = "FNB Locked Vault (Savings Target)", value = f) { fnbAllocateInput = it }
                                AllocationRow(label = "Transport Reserve Fund", value = t) { transportAllocateInput = it }
                                AllocationRow(label = "Spotify Subscription Buffer", value = s) { spotifyAllocateInput = it }

                                Spacer(modifier = Modifier.height(12.dp))
                                Divider()
                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("DAILY life (Cash Wallet remainder):", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Text(
                                        "ZMW ${String.format("%.2f", remainingCashVal)}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = if (remainingCashVal >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                    )
                                }

                                if (remainingCashVal < 0) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        "⚠️ Allocation overdraft! Your items exceed ZMW $sal.",
                                        color = MaterialTheme.colorScheme.error,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Spacer(modifier = Modifier.height(24.dp))

                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    OutlinedButton(
                                        onClick = { viewModel.setPaydayStep(1) },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Back")
                                    }

                                    Button(
                                        onClick = {
                                            viewModel.runPaydayAllocation(
                                                salaryAmount = sal,
                                                fnb = f,
                                                transport = t,
                                                spotify = s
                                            )
                                        },
                                        enabled = remainingCashVal >= 0,
                                        modifier = Modifier
                                            .weight(1.5f)
                                            .testTag("payday_enforce_submit_button")
                                    ) {
                                        Text("Enforce System Plan", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        3 -> {
                            val sal = config?.salaryAmount ?: 3500.0
                            val f = config?.fnbAllocation ?: 1500.0

                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(RoundedCornerShape(32.dp))
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.VerifiedUser,
                                        contentDescription = "Success Shield",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Text(
                                    "BEHAVIOR ENFORCEMENT KEYED IN",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 16.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.primary,
                                    textAlign = TextAlign.Center
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    "Payday allocation completed on the first. Emergency vault enriched with ZMW ${String.format("%.2f", f)}. Your Daily cash weekly schedule is active.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )

                                Spacer(modifier = Modifier.height(24.dp))

                                Button(
                                    onClick = { viewModel.resetPaydayState() },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                    modifier = Modifier
                                        .wrapContentWidth()
                                        .testTag("unlock_payday_button")
                                ) {
                                    Icon(Icons.Default.LockOpen, "Lock", modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Unlock Planner (Test/Reset)", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StepBubble(number: Int, text: String, active: Boolean, completed: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(
                    when {
                        completed -> MaterialTheme.colorScheme.primary
                        active -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (completed) {
                Icon(Icons.Default.Check, "Done", tint = Color.Black, modifier = Modifier.size(14.dp))
            } else {
                Text(
                    text = number.toString(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (active) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = text,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = if (active || completed) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun RowScope.StepDivider() {
    Divider(
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
        modifier = Modifier
            .weight(1f)
            .padding(horizontal = 4.dp)
            .align(Alignment.CenterVertically)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllocationRow(label: String, value: Double, onValueChange: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
            Text("Auto-transfers each payday cycle.", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(
            "ZMW $value",
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 12.dp)
        )
    }
}

// -------------------------------------------------------------
// SCREEN 3: WEEKLY BUDGET MANAGER
// -------------------------------------------------------------
@Composable
fun WeeklyBudgetScreen(
    wallets: List<WalletEntity>,
    logs: List<LogEntryEntity>,
    viewModel: DatumViewModel,
    onAddExpense: () -> Unit
) {
    val weeklyLimit = viewModel.calculateWeeklyAllowance()
    val spentThisWeek = viewModel.calculateSpentThisWeek()
    val remaining = (weeklyLimit - spentThisWeek).coerceAtLeast(0.0)

    val formatter = remember { SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault()) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
            .testTag("weekly_budget_scroll"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // High Contrast Budget Wheel State styled as Weekly Command Center
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("weekly_hud_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Top Dash border
                    Canvas(modifier = Modifier.fillMaxWidth().height(1.dp)) {
                        val strokeWidth = 1.dp.toPx()
                        val intervals = floatArrayOf(12f, 12f)
                        val pathEffect = PathEffect.dashPathEffect(intervals, 0f)
                        drawLine(
                            color = M3Outline,
                            start = Offset(0f, 0f),
                            end = Offset(size.width, 0f),
                            strokeWidth = strokeWidth,
                            pathEffect = pathEffect
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        "WEEKLY SAFE SPENDING",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = M3Primary,
                        letterSpacing = 2.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "ZMW",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = M3OnBackground,
                            modifier = Modifier.padding(top = 10.dp, end = 6.dp)
                        )
                        Text(
                            text = String.format("%.0f", remaining),
                            fontSize = 68.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.SansSerif,
                            color = M3OnBackground,
                            letterSpacing = (-1.5).sp
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Box(
                        modifier = Modifier
                            .background(M3AlertPillBg, RoundedCornerShape(50.dp))
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "SYSTEM RESET IN 3 DAYS",
                            color = M3AlertPillText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Bottom Dash border
                    Canvas(modifier = Modifier.fillMaxWidth().height(1.dp)) {
                        val strokeWidth = 1.dp.toPx()
                        val intervals = floatArrayOf(12f, 12f)
                        val pathEffect = PathEffect.dashPathEffect(intervals, 0f)
                        drawLine(
                            color = M3Outline,
                            start = Offset(0f, 0f),
                            end = Offset(size.width, 0f),
                            strokeWidth = strokeWidth,
                            pathEffect = pathEffect
                        )
                    }
                }
            }
        }

        // Action Trigger
        item {
            Button(
                onClick = onAddExpense,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("trigger_cash_expense_button")
            ) {
                DuoToneIcon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Log Exp",
                    primaryColor = Color.White,
                    secondaryColor = Color.White.copy(alpha = 0.35f),
                    size = 18.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("RECORD MANUAL CASH SPEND", fontWeight = FontWeight.Bold)
            }
        }

        // Ledgers specific to current week
        item {
            Text(
                "DISCIPLINARY EXPENDITURE TRAIL",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                letterSpacing = 1.sp
            )
        }

        val weeklyLogs = logs.filter { it.walletId == "CASH" }
        if (weeklyLogs.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "No manual transactions recorded on Cash Wallet. Budget fully intact.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            items(weeklyLogs) { log ->
                val isDebit = log.type == "DEBIT"
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = when (log.category) {
                                        "Transport" -> Icons.Default.DirectionsCar
                                        "Food" -> Icons.Default.Store
                                        else -> Icons.Default.LocalAtm
                                    },
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    contentDescription = null
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Text(log.description, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text(
                                    text = "${log.category} • ${formatter.format(Date(log.timestamp))}",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Text(
                            text = "${if (isDebit) "-" else "+"} ZMW ${String.format("%.2f", log.amount)}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = if (isDebit) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// SCREEN 4: GOAL TRACKING (DECEMBER TARGET)
// -------------------------------------------------------------
@Composable
fun GoalsScreen(
    wallets: List<WalletEntity>,
    goal: GoalEntity?,
    viewModel: DatumViewModel
) {
    if (goal == null) return

    val fnbWallet = wallets.find { it.id == "FNB" }
    val currentSaved = fnbWallet?.balance ?: 0.0
    val target = goal.targetAmount
    // Calculating progress percent
    val progressPercent = if (target > 0) (currentSaved / target).toFloat().coerceIn(0f, 1f) else 1f

    // July: +1500, August: +1500, etc, monthly target checks
    val monthlyTargetSavings = 1500.0

    var customTargetInput by remember { mutableStateOf(target.toString()) }
    var customDeadlineInput by remember { mutableStateOf(goal.deadlineText) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
            .testTag("goals_scroll"),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Text(
                "DECEMBER MILESTONE RADAR",
                fontWeight = FontWeight.Black,
                fontSize = 14.sp,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Tracking cumulative locked FNB deposits.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = M3GoalCardBg),
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("milestone_card")
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(
                            text = goal.deadlineText.uppercase(),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = M3GoalCardText,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "ZMW ${String.format("%,.0f", currentSaved)} / ${String.format("%,.0f", target)}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = M3GoalCardText.copy(alpha = 0.7f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    LinearProgressIndicator(
                        progress = { progressPercent },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(12.dp)
                            .clip(RoundedCornerShape(6.dp)),
                        color = M3Primary,
                        trackColor = M3Surface
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("ACCUMULATED FNB VAULT", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = M3GoalCardText.copy(alpha = 0.61f))
                            Text("ZMW ${String.format("%.2f", currentSaved)}", fontWeight = FontWeight.Black, fontSize = 16.sp, color = M3GoalCardText)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("TARGET GOAL LIMIT", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = M3GoalCardText.copy(alpha = 0.61f))
                            Text("ZMW ${String.format("%.2f", target)}", fontWeight = FontWeight.Black, fontSize = 16.sp, color = M3GoalCardText)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    HorizontalDivider(color = M3GoalCardText.copy(alpha = 0.15f))

                    Spacer(modifier = Modifier.height(12.dp))

                    if (currentSaved >= target) {
                        Text(
                            "🎉 CONGRATULATIONS: System disciplined target reached. Vault secured.",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = M3GoalCardText
                        )
                    } else {
                        // Progress months projection logic (assuming 1500 monthly)
                        val needed = target - currentSaved
                        val months = Math.ceil(needed / monthlyTargetSavings).toInt()
                        Text(
                            "STATUS: YOU ARE ON TRACK\nNeed $months more payday splits (+ZMW 1,500/mo) to reinforce.",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.tertiary,
                            lineHeight = 14.sp
                        )
                    }
                }
            }
        }

        // Monthly Progression Breakdown Tracker (July -> November Target)
        item {
            Text(
                "AUTO MONTHLY ENFORCEMENT BREAKDOWN",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                letterSpacing = 1.sp
            )
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    listOf(
                        MonthGoalItem("July Target Increment", 1500.0, currentSaved >= 1500.0),
                        MonthGoalItem("August Target Increment", 3000.0, currentSaved >= 3000.0),
                        MonthGoalItem("September Target Increment", 4500.0, currentSaved >= 4500.0),
                        MonthGoalItem("October Target Increment", 6000.0, currentSaved >= 6000.0),
                        MonthGoalItem("November final reinforce", 7500.0, currentSaved >= 7500.0)
                    ).forEach { month ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                DuoToneIcon(
                                    imageVector = if (month.completed) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                    primaryColor = if (month.completed) M3Primary else M3Outline,
                                    secondaryColor = if (month.completed) M3Primary.copy(alpha = 0.35f) else M3Outline.copy(alpha = 0.35f),
                                    contentDescription = null,
                                    size = 16.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(month.name, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Text(
                                "ZMW ${String.format("%.0f", month.criticalAmount)}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (month.completed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Goal Editing panel
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("CUSTOMIZE ARCHITECT TARGET", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = customTargetInput,
                        onValueChange = { customTargetInput = it },
                        label = { Text("Target Amount (ZMW)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = customDeadlineInput,
                        onValueChange = { customDeadlineInput = it },
                        label = { Text("Goal Title Timeline") },
                        placeholder = { Text("e.g. ZMW 7,500 by November") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            val tVal = customTargetInput.toDoubleOrNull()
                            if (tVal != null && tVal > 0.0) {
                                viewModel.updateGoalTarget(tVal, customDeadlineInput.ifEmpty { "by Deadline" })
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("apply_goal_changes")
                    ) {
                        Text("Apply Goal Configuration")
                    }
                }
            }
        }
    }
}

data class MonthGoalItem(val name: String, val criticalAmount: Double, val completed: Boolean)

// -------------------------------------------------------------
// SCREEN 5: EMERGENCY RULES ENGINE
// -------------------------------------------------------------
@Composable
fun RulesScreen(
    wallets: List<WalletEntity>,
    logs: List<LogEntryEntity>,
    viewModel: DatumViewModel
) {
    val formatter = remember { SimpleDateFormat("EEE, dd MMM yyyy • HH:mm", Locale.getDefault()) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
            .testTag("rules_view_scroll"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                "SYSTEM ENFORCEMENT ENGINE",
                fontWeight = FontWeight.Black,
                fontSize = 14.sp,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "These behavior-control rules are hard-coded to secure asset longevity.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Display 4 Rules card set
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                RuleDetailCard(
                    title = "RULE 1: Locked Vault Gateway (FNB)",
                    description = "FNB balance is completely locked. Outflows can only occur if the user selects Medical, Transport, or Survival Food override justifications. Non-emergency attempts are hard-blocked.",
                    icon = Icons.Default.Lock,
                    active = true,
                    color = MaterialTheme.colorScheme.error
                )
                RuleDetailCard(
                    title = "RULE 2: Payday Allocation Enforcer",
                    description = "On the 1st of every month, salaries (ZMW 3,500) are mapped immediately. 1,500 FNB, 540 Transport, 100 Spotify and remaining CASH. Transfer rates boundaries are strictly enforced.",
                    icon = Icons.Default.Check,
                    active = true,
                    color = MaterialTheme.colorScheme.primary
                )
                RuleDetailCard(
                    title = "RULE 3: Double-Buffer Subscription Lock",
                    description = "Global card balance is strictly limited to Spotify deductions (ZMW 100). This prevents subscription spill-over into grocery or emergency cash.",
                    icon = Icons.Default.CreditCard,
                    active = true,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }

        // Audit Trail of Emergency Withdrawals
        item {
            Text(
                "FNB EMERGENCY AUDIT LOG",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                letterSpacing = 1.sp
            )
        }

        // Collect FNB DEBIT logs
        val emergencyLogs = logs.filter { it.walletId == "FNB" && it.type == "DEBIT" }
        if (emergencyLogs.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "No emergency withdrawals found. FNB compliance status is 100% disciplined.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            items(emergencyLogs) { log ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(14.dp),
                                    contentDescription = null
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Emergency Breach Approved", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                            }
                            Text(
                                "ZMW ${String.format("%.2f", log.amount)}",
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.error
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Item: ${log.description}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        Text(
                            text = "Reason Claimed: ${log.emergencyReason ?: "N/A"}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = formatter.format(Date(log.timestamp)),
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RuleDetailCard(
    title: String,
    description: String,
    icon: ImageVector,
    active: Boolean,
    color: Color
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(color.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        DuoToneIcon(
                            imageVector = icon,
                            contentDescription = null,
                            primaryColor = color,
                            secondaryColor = color.copy(alpha = 0.35f),
                            size = 16.dp
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(title, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(color.copy(alpha = 0.2f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text("ENFORCED", fontSize = 8.sp, fontWeight = FontWeight.Black, color = color)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                description,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 15.sp
            )
        }
    }
}

@Composable
fun WalletGridCard(wallet: WalletEntity, onOpenAddSpend: (String) -> Unit) {
    val isFnb = wallet.id == "FNB"
    val isCash = wallet.id == "CASH"

    // Container and content styling consistent with Bold Typography specs
    val bg = when {
        isFnb -> M3FnbVaultBg
        isCash -> M3CashWalletBg
        else -> M3AirtelGlobalBg
    }
    val borderStroke = when {
        isCash -> BorderStroke(2.dp, M3Primary)
        isFnb -> null
        else -> BorderStroke(1.dp, M3Outline)
    }
    val tc = if (isFnb) Color.White else M3OnBackground
    val subTc = if (isFnb) Color.White.copy(alpha = 0.7f) else M3OnBackground.copy(alpha = 0.7f)

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = bg),
        border = borderStroke,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 115.dp)
            .testTag("wallet_card_${wallet.id}")
            .clickable { onOpenAddSpend(wallet.id) }
    ) {
        Column(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon representation
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            when {
                                isFnb -> Color.White.copy(alpha = 0.15f)
                                isCash -> M3PillActiveBg
                                else -> Color(0xFFF4EFF4)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    DuoToneIcon(
                        imageVector = when (wallet.id) {
                            "FNB" -> Icons.Default.Lock
                            "CASH" -> Icons.Default.LocalAtm
                            "AIRTEL" -> Icons.Default.CompassCalibration
                            else -> Icons.Default.CreditCard
                        },
                        contentDescription = null,
                        primaryColor = if (isFnb) Color.White else M3Primary,
                        secondaryColor = if (isFnb) Color.White.copy(alpha = 0.35f) else M3Secondary.copy(alpha = 0.35f),
                        size = 18.dp
                    )
                }

                // Label tag
                Text(
                    text = when (wallet.id) {
                        "FNB" -> "Vault"
                        "CASH" -> "Daily"
                        "AIRTEL" -> "Buffer"
                        else -> "Subs"
                    }.uppercase(),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    color = when {
                        isFnb -> Color.White.copy(alpha = 0.6f)
                        isCash -> M3Primary
                        else -> M3Secondary
                    },
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Column {
                Text(
                    text = when (wallet.id) {
                        "FNB" -> "FNB Account"
                        "CASH" -> "Cash Fund"
                        "AIRTEL" -> "Airtel"
                        else -> "Spotify Card"
                    },
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = subTc
                )
                Text(
                    text = "ZMW ${String.format("%.0f", wallet.balance)}",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = tc,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
fun DuoToneIcon(
    imageVector: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    primaryColor: Color = MaterialTheme.colorScheme.primary,
    secondaryColor: Color = primaryColor.copy(alpha = 0.35f),
    size: androidx.compose.ui.unit.Dp = 24.dp
) {
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        // Shadow offset layer for 3D retro printed look (secondary color)
        Icon(
            imageVector = imageVector,
            contentDescription = null,
            tint = secondaryColor,
            modifier = Modifier
                .fillMaxSize()
                .offset(x = 1.dp, y = 1.dp)
        )
        // Solid foreground accent layer (primary color)
        Icon(
            imageVector = imageVector,
            contentDescription = contentDescription,
            tint = primaryColor,
            modifier = Modifier.fillMaxSize()
        )
    }
}


