package com.dhandy.smartbudget.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dhandy.smartbudget.data.local.entity.TargetEntity
import com.dhandy.smartbudget.data.local.model.TransactionWithDetails
import com.dhandy.smartbudget.ui.viewmodel.TransactionViewModel
import com.dhandy.smartbudget.ui.utils.ThousandsSeparatorVisualTransformation
import com.dhandy.smartbudget.ui.theme.*
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.hypot
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: TransactionViewModel,
    onAddTransactionClick: () -> Unit,
    onAllTransactionsClick: () -> Unit,
    onAnalyticsClick: () -> Unit,
    onCalculatorClick: () -> Unit
) {
    // Mengumpulkan state secara reaktif dari ViewModel
    val transactions by viewModel.transactions.collectAsState()
    val wallets by viewModel.wallets.collectAsState()
    val targets by viewModel.targets.collectAsState()
    val isDarkTheme by viewModel.isDarkTheme.collectAsState()
 
    var activeTab by remember { mutableStateOf(0) } // 0 = Dompet Saya, 1 = Target Impian
    var showAddWalletDialog by remember { mutableStateOf(false) }
    var walletToManage by remember { mutableStateOf<com.dhandy.smartbudget.data.local.entity.WalletEntity?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showTransferDialog by remember { mutableStateOf(false) }

    var showAddTargetDialog by remember { mutableStateOf(false) }
    var targetToManage by remember { mutableStateOf<TargetEntity?>(null) }
    var showDeleteTargetConfirmDialog by remember { mutableStateOf(false) }
    var showSavingDialog by remember { mutableStateOf(false) }

    // Menghitung ringkasan dari list (untuk MVP)
    val totalBalance = wallets.sumOf { it.currentBalance }
    val totalIncome = transactions.filter { it.category.type == "INCOME" }.sumOf { it.transaction.amount }
    val totalExpense = transactions.filter { it.category.type == "EXPENSE" }.sumOf { it.transaction.amount }

    var buttonCenterInRoot by remember { mutableStateOf(Offset.Zero) }
    var isCircleRevealing by remember { mutableStateOf(false) }
    var revealTargetIsDark by remember { mutableStateOf(isDarkTheme) }
    val revealRadiusProgress = remember { Animatable(0f) }
    val revealAlpha = remember { Animatable(1f) }
    val coinRotation = remember { Animatable(if (isDarkTheme) 180f else 0f) }
    val coroutineScope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets(0, 0, 0, 0)
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .statusBarsPadding()
                    .background(MaterialTheme.colorScheme.background),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
            // 0. Custom Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Yo, Smart Spender! 💸",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "SmartBudget",
                            style = TextStyle(
                                color = MaterialTheme.colorScheme.onBackground,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = (-1).sp
                            )
                        )
                    }
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ThemeCoinButton(
                            coinRotation = coinRotation.value,
                            isAnimating = isCircleRevealing,
                            onPositioned = { center -> buttonCenterInRoot = center },
                            onToggle = {
                                if (!isCircleRevealing) {
                                    val targetDark = !isDarkTheme
                                    revealTargetIsDark = targetDark
                                    isCircleRevealing = true
                                    coroutineScope.launch {
                                        launch {
                                            coinRotation.animateTo(
                                                targetValue = coinRotation.value + 180f,
                                                animationSpec = tween(durationMillis = 520, easing = FastOutSlowInEasing)
                                            )
                                        }
                                        launch {
                                            revealRadiusProgress.snapTo(0f)
                                            revealRadiusProgress.animateTo(
                                                targetValue = 1f,
                                                animationSpec = tween(durationMillis = 460, easing = FastOutSlowInEasing)
                                            )
                                            viewModel.setDarkTheme(targetDark)
                                            kotlinx.coroutines.delay(40)
                                            revealAlpha.animateTo(0f, animationSpec = tween(120))
                                            isCircleRevealing = false
                                            revealAlpha.snapTo(1f)
                                        }
                                    }
                                }
                            }
                        )
                    }
                }
            }

            // 1. Ringkasan Saldo (Kartu Visual Utama)
            item {
                SummaryCard(
                    totalBalance = totalBalance,
                    totalIncome = totalIncome,
                    totalExpense = totalExpense
                )
            }

            // 2. Tab Selector Pill
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            shape = RoundedCornerShape(26.dp)
                        )
                        .padding(4.dp)
                ) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        // Tab 0: Dompetku
                        val isTab0Active = activeTab == 0
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .background(
                                    color = if (isTab0Active) AccentOrange else Color.Transparent,
                                    shape = RoundedCornerShape(22.dp)
                                )
                                .clickable { activeTab = 0 },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Dompetku 💳",
                                fontWeight = if (isTab0Active) FontWeight.Bold else FontWeight.Medium,
                                color = if (isTab0Active) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                fontSize = 13.sp
                            )
                        }
                        
                        // Tab 1: Targetku
                        val isTab1Active = activeTab == 1
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .background(
                                    color = if (isTab1Active) AccentOrange else Color.Transparent,
                                    shape = RoundedCornerShape(22.dp)
                                )
                                .clickable { activeTab = 1 },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Targetku 🎯",
                                fontWeight = if (isTab1Active) FontWeight.Bold else FontWeight.Medium,
                                color = if (isTab1Active) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            // 3. Konten berdasarkan Tab Aktif
            item {
                if (activeTab == 0) {
                    // Konten Dompet Saya
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Kelola Akun",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                TextButton(onClick = { showTransferDialog = true }) {
                                    Text("⇅ Transfer")
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                TextButton(onClick = { showAddWalletDialog = true }) {
                                    Text("+ Tambah")
                                }
                            }
                        }
                        
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(wallets, key = { it.id }) { wallet ->
                                WalletCard(
                                    wallet = wallet,
                                    onClick = { walletToManage = wallet }
                                )
                            }
                        }
                    }
                } else {
                    // Konten Target Impian
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Rencana Belanja",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                            )
                            TextButton(onClick = { showAddTargetDialog = true }) {
                                Text("+ Target")
                            }
                        }
                        
                        if (targets.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(100.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Belum ada target impian.", color = Color.Gray)
                            }
                        } else {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(targets, key = { it.id }) { target ->
                                    TargetCard(
                                        target = target,
                                        onClick = { targetToManage = target }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Judul Daftar Transaksi
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Transaksi Terbaru",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    TextButton(onClick = onAllTransactionsClick) {
                        Text(
                            text = "Lihat Semua ➜", 
                            fontWeight = FontWeight.Bold, 
                            color = AccentOrange
                        )
                    }
                }
            }

            // 2. Daftar Transaksi Terbaru
            if (transactions.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Belum ada transaksi.", color = Color.Gray)
                    }
                }
            } else {
                items(transactions, key = { it.transaction.id }) { transactionWithDetails ->
                    TransactionItem(transactionWithDetails)
                }
            }
        }
    }

    // Dialog Tambah Dompet Baru
    if (showAddWalletDialog) {
        var walletName by remember { mutableStateOf("") }
        var initialBalanceStr by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddWalletDialog = false },
            title = { Text("Tambah Dompet Baru") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = walletName,
                        onValueChange = { walletName = it },
                        label = { Text("Nama Dompet") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = initialBalanceStr,
                        onValueChange = { input ->
                            initialBalanceStr = input.replace(Regex("[^0-9]"), "")
                        },
                        label = { Text("Saldo Awal") },
                        prefix = { Text("Rp ") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        visualTransformation = ThousandsSeparatorVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val balance = initialBalanceStr.toDoubleOrNull() ?: 0.0
                        if (walletName.isNotBlank()) {
                            viewModel.addWallet(walletName, balance)
                            showAddWalletDialog = false
                        }
                    },
                    enabled = walletName.isNotBlank() && initialBalanceStr.isNotBlank()
                ) {
                    Text("Simpan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddWalletDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    // Dialog Kelola Dompet (Ubah Nama, Saldo, Hapus)
    walletToManage?.let { wallet ->
        var editName by remember(wallet) { mutableStateOf(wallet.name) }
        var editBalanceStr by remember(wallet) {
            mutableStateOf(wallet.currentBalance.toLong().toString())
        }

        AlertDialog(
            onDismissRequest = { walletToManage = null },
            title = { Text("Kelola Dompet") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Nama Dompet") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = editBalanceStr,
                        onValueChange = { input ->
                            editBalanceStr = input.replace(Regex("[^0-9]"), "")
                        },
                        label = { Text("Saldo") },
                        prefix = { Text("Rp ") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        visualTransformation = ThousandsSeparatorVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { showDeleteConfirmDialog = true },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Hapus")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val balance = editBalanceStr.toDoubleOrNull() ?: 0.0
                            if (editName.isNotBlank() && editBalanceStr.isNotBlank()) {
                                viewModel.updateWallet(
                                    wallet.copy(
                                        name = editName,
                                        currentBalance = balance,
                                        initialBalance = balance
                                    )
                                )
                                walletToManage = null
                            }
                        },
                        enabled = editName.isNotBlank() && editBalanceStr.isNotBlank()
                    ) {
                        Text("Simpan")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { walletToManage = null }) {
                    Text("Batal")
                }
            }
        )
    }

    // Dialog Konfirmasi Hapus
    if (showDeleteConfirmDialog && walletToManage != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Hapus Dompet?") },
            text = {
                Text("Menghapus dompet \"${walletToManage?.name}\" akan menghapus semua riwayat transaksi yang terkait dengan dompet ini secara permanen. Apakah Anda yakin?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        walletToManage?.let { wallet ->
                            viewModel.deleteWallet(wallet)
                        }
                        showDeleteConfirmDialog = false
                        walletToManage = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Hapus")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    // Dialog Transfer Antar Dompet
    if (showTransferDialog) {
        var fromWallet by remember { mutableStateOf<com.dhandy.smartbudget.data.local.entity.WalletEntity?>(null) }
        var toWallet by remember { mutableStateOf<com.dhandy.smartbudget.data.local.entity.WalletEntity?>(null) }
        var amountStr by remember { mutableStateOf("") }
        var note by remember { mutableStateOf("") }

        var fromExpanded by remember { mutableStateOf(false) }
        var toExpanded by remember { mutableStateOf(false) }

        // Validasi saldo asal mencukupi
        val amountDouble = amountStr.toDoubleOrNull() ?: 0.0
        val isBalanceSufficient = fromWallet == null || fromWallet!!.currentBalance >= amountDouble
        val isFormValid = fromWallet != null && toWallet != null && fromWallet != toWallet && 
                amountDouble > 0.0 && isBalanceSufficient

        AlertDialog(
            onDismissRequest = { showTransferDialog = false },
            title = { Text("Transfer Antar Dompet") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // 1. Dropdown Dompet Asal
                    ExposedDropdownMenuBox(
                        expanded = fromExpanded,
                        onExpandedChange = { fromExpanded = it },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = fromWallet?.let { "${it.name} (Saldo: ${formatCurrency(it.currentBalance)})" } ?: "Pilih Dompet Asal",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Dari Dompet") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = fromExpanded) },
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = fromExpanded,
                            onDismissRequest = { fromExpanded = false }
                        ) {
                            wallets.forEach { wallet ->
                                DropdownMenuItem(
                                    text = { Text("${wallet.name} (${formatCurrency(wallet.currentBalance)})") },
                                    onClick = {
                                        fromWallet = wallet
                                        fromExpanded = false
                                        if (toWallet?.id == wallet.id) {
                                            toWallet = null
                                        }
                                    }
                                )
                            }
                        }
                    }

                    // 2. Dropdown Dompet Tujuan
                    ExposedDropdownMenuBox(
                        expanded = toExpanded,
                        onExpandedChange = { toExpanded = it },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = toWallet?.name ?: "Pilih Dompet Tujuan",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Ke Dompet") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = toExpanded) },
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = toExpanded,
                            onDismissRequest = { toExpanded = false }
                        ) {
                            wallets.filter { it.id != fromWallet?.id }.forEach { wallet ->
                                DropdownMenuItem(
                                    text = { Text(wallet.name) },
                                    onClick = {
                                        toWallet = wallet
                                        toExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // 3. Input Nominal Transfer
                    OutlinedTextField(
                        value = amountStr,
                        onValueChange = { input ->
                            amountStr = input.replace(Regex("[^0-9]"), "")
                        },
                        label = { Text("Nominal Transfer") },
                        prefix = { Text("Rp ") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        visualTransformation = ThousandsSeparatorVisualTransformation(),
                        isError = !isBalanceSufficient,
                        supportingText = {
                            if (!isBalanceSufficient) {
                                Text("Saldo tidak mencukupi!", color = MaterialTheme.colorScheme.error)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // 4. Catatan Opsional
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text("Catatan (Opsional)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (isFormValid) {
                            viewModel.transferFunds(
                                fromWalletId = fromWallet!!.id,
                                toWalletId = toWallet!!.id,
                                amount = amountDouble,
                                note = note.takeIf { it.isNotBlank() }
                            )
                            showTransferDialog = false
                        }
                    },
                    enabled = isFormValid
                ) {
                    Text("Transfer")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTransferDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    // Dialog Tambah Target Baru
    if (showAddTargetDialog) {
        var targetName by remember { mutableStateOf("") }
        var targetAmountStr by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddTargetDialog = false },
            title = { Text("Tambah Target Baru") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = targetName,
                        onValueChange = { targetName = it },
                        label = { Text("Nama Barang") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = targetAmountStr,
                        onValueChange = { input ->
                            targetAmountStr = input.replace(Regex("[^0-9]"), "")
                        },
                        label = { Text("Target Harga") },
                        prefix = { Text("Rp ") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        visualTransformation = ThousandsSeparatorVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = targetAmountStr.toDoubleOrNull() ?: 0.0
                        if (targetName.isNotBlank() && targetAmountStr.isNotBlank()) {
                            viewModel.addTarget(targetName, amount)
                            showAddTargetDialog = false
                        }
                    },
                    enabled = targetName.isNotBlank() && targetAmountStr.isNotBlank()
                ) {
                    Text("Simpan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddTargetDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    // Dialog Kelola Target (Tabung / Hapus)
    targetToManage?.let { target ->
        if (!showSavingDialog) {
            AlertDialog(
                onDismissRequest = { targetToManage = null },
                title = { Text("Kelola Target Impian") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Target: ${target.name}", fontWeight = FontWeight.Bold)
                        Text("Target Harga: ${formatCurrency(target.targetAmount)}")
                        Text("Saldo Terkumpul: ${formatCurrency(target.currentAmount)}")
                    }
                },
                confirmButton = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = { showDeleteTargetConfirmDialog = true },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Hapus")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                showSavingDialog = true
                            }
                        ) {
                            Text("Tabung")
                        }
                    }
                },
                dismissButton = {
                    TextButton(onClick = { targetToManage = null }) {
                        Text("Batal")
                    }
                }
            )
        }
    }

    // Dialog Konfirmasi Hapus Target
    if (showDeleteTargetConfirmDialog && targetToManage != null) {
        AlertDialog(
            onDismissRequest = { showDeleteTargetConfirmDialog = false },
            title = { Text("Hapus Target?") },
            text = {
                Text("Apakah Anda yakin ingin menghapus target \"${targetToManage?.name}\"? Saldo yang terkumpul tidak akan ditransfer kembali secara otomatis.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        targetToManage?.let { target ->
                            viewModel.deleteTarget(target)
                        }
                        showDeleteTargetConfirmDialog = false
                        targetToManage = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Hapus")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteTargetConfirmDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    // Dialog Tabung/Transfer ke Target
    if (showSavingDialog && targetToManage != null) {
        val target = targetToManage!!
        var selectedWalletForSaving by remember { mutableStateOf<com.dhandy.smartbudget.data.local.entity.WalletEntity?>(null) }
        var savingAmountStr by remember { mutableStateOf("") }
        var savingNote by remember { mutableStateOf("") }

        var walletDropdownExpanded by remember { mutableStateOf(false) }

        // Validasi saldo
        val savingAmountDouble = savingAmountStr.toDoubleOrNull() ?: 0.0
        val isBalanceSufficient = selectedWalletForSaving == null || selectedWalletForSaving!!.currentBalance >= savingAmountDouble
        val isFormValid = selectedWalletForSaving != null && savingAmountDouble > 0.0 && isBalanceSufficient

        AlertDialog(
            onDismissRequest = { 
                showSavingDialog = false 
                targetToManage = null
            },
            title = { Text("Tabung ke Target") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Menabung untuk: ${target.name}", fontWeight = FontWeight.Bold)

                    // 1. Dropdown Dompet Asal
                    ExposedDropdownMenuBox(
                        expanded = walletDropdownExpanded,
                        onExpandedChange = { walletDropdownExpanded = it },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = selectedWalletForSaving?.let { "${it.name} (Saldo: ${formatCurrency(it.currentBalance)})" } ?: "Pilih Dompet Asal",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Dari Dompet") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = walletDropdownExpanded) },
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = walletDropdownExpanded,
                            onDismissRequest = { walletDropdownExpanded = false }
                        ) {
                            wallets.forEach { wallet ->
                                DropdownMenuItem(
                                    text = { Text("${wallet.name} (${formatCurrency(wallet.currentBalance)})") },
                                    onClick = {
                                        selectedWalletForSaving = wallet
                                        walletDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // 2. Input Nominal Tabungan
                    OutlinedTextField(
                        value = savingAmountStr,
                        onValueChange = { input ->
                            savingAmountStr = input.replace(Regex("[^0-9]"), "")
                        },
                        label = { Text("Nominal Tabungan") },
                        prefix = { Text("Rp ") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        visualTransformation = ThousandsSeparatorVisualTransformation(),
                        isError = !isBalanceSufficient,
                        supportingText = {
                            if (!isBalanceSufficient) {
                                Text("Saldo tidak mencukupi!", color = MaterialTheme.colorScheme.error)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // 3. Catatan Opsional
                    OutlinedTextField(
                        value = savingNote,
                        onValueChange = { savingNote = it },
                        label = { Text("Catatan (Opsional)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (isFormValid) {
                            viewModel.savingToTarget(
                                walletId = selectedWalletForSaving!!.id,
                                targetId = target.id,
                                amount = savingAmountDouble,
                                note = savingNote.takeIf { it.isNotBlank() }
                            )
                            showSavingDialog = false
                            targetToManage = null // Tutup kedua dialog
                        }
                    },
                    enabled = isFormValid
                ) {
                    Text("Tabung")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showSavingDialog = false 
                    targetToManage = null
                }) {
                    Text("Batal")
                }
            }
        )
    }

    // Circular Reveal Animation Overlay
    if (isCircleRevealing && buttonCenterInRoot != Offset.Zero) {
        val targetBgColor = if (revealTargetIsDark) Color.Black else Color.White
        val ringGlowColor = if (revealTargetIsDark) Color.White else Color.Black

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {}
        ) {
            val center = buttonCenterInRoot
            val maxRadius = maxOf(
                hypot(center.x, center.y),
                hypot(size.width - center.x, center.y),
                hypot(center.x, size.height - center.y),
                hypot(size.width - center.x, size.height - center.y)
            )
            val currentRadius = maxRadius * revealRadiusProgress.value
            val alpha = revealAlpha.value

            if (currentRadius > 0f) {
                val coinHoleRadius = 24.dp.toPx()
                if (currentRadius <= coinHoleRadius) {
                    drawCircle(
                        color = targetBgColor.copy(alpha = alpha),
                        radius = currentRadius,
                        center = center
                    )
                } else {
                    val circlePath = Path().apply {
                        addOval(Rect(center = center, radius = currentRadius))
                    }
                    val holePath = Path().apply {
                        addOval(Rect(center = center, radius = coinHoleRadius))
                    }
                    val revealPath = Path().apply {
                        op(circlePath, holePath, PathOperation.Difference)
                    }
                    drawPath(
                        path = revealPath,
                        color = targetBgColor.copy(alpha = alpha)
                    )
                }

                // Radiant glowing wavefront at the perimeter of the circle
                if (revealRadiusProgress.value in 0.02f..0.98f) {
                    drawCircle(
                        color = ringGlowColor.copy(alpha = alpha * (1f - revealRadiusProgress.value * 0.35f)),
                        radius = currentRadius,
                        center = center,
                        style = Stroke(width = 5.dp.toPx())
                    )
                }
            }
        }
    }
}
}

@Composable
fun SummaryCard(totalBalance: Double, totalIncome: Double, totalExpense: Double) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = WarmYellowCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Today Balance",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = DarkNavBrown.copy(alpha = 0.75f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                formatCurrency(totalBalance),
                style = MaterialTheme.typography.headlineLarge.copy(fontSize = 32.sp),
                fontWeight = FontWeight.Black,
                color = DarkNavBrown
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Speedometer Arc Gauge
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                val budgetLimit = (totalIncome.takeIf { it > 0 } ?: (totalBalance + totalExpense)).coerceAtLeast(1.0)
                val expenseRatio = (totalExpense / budgetLimit).toFloat().coerceIn(0f, 1f)
                val remainingAmount = (budgetLimit - totalExpense).coerceAtLeast(0.0)

                androidx.compose.foundation.Canvas(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .fillMaxHeight()
                ) {
                    val strokeW = 14.dp.toPx()
                    val arcSize = size.width - strokeW
                    val topLeftOffset = androidx.compose.ui.geometry.Offset(strokeW / 2, size.height - (arcSize / 2))

                    // Background track (Light Arc)
                    drawArc(
                        color = Color.White.copy(alpha = 0.45f),
                        startAngle = 180f,
                        sweepAngle = 180f,
                        useCenter = false,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeW, cap = androidx.compose.ui.graphics.StrokeCap.Round),
                        size = androidx.compose.ui.geometry.Size(arcSize, arcSize),
                        topLeft = topLeftOffset
                    )

                    // Active progress track (Gradient / Orange Arc)
                    drawArc(
                        color = AccentOrange,
                        startAngle = 180f,
                        sweepAngle = 180f * expenseRatio,
                        useCenter = false,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeW, cap = androidx.compose.ui.graphics.StrokeCap.Round),
                        size = androidx.compose.ui.geometry.Size(arcSize, arcSize),
                        topLeft = topLeftOffset
                    )
                }

                // Center Left Remaining Badge
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(bottom = 6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(AccentOrange, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("💳", fontSize = 18.sp)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Sisa: " + formatCurrency(remainingAmount),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = DarkNavBrown
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Sub-metrics (Masuk vs Keluar)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(horizontalAlignment = Alignment.Start) {
                    Text("Pemasukan", fontSize = 11.sp, color = DarkNavBrown.copy(alpha = 0.7f), fontWeight = FontWeight.Medium)
                    Text(formatCurrency(totalIncome), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF047857))
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Pengeluaran", fontSize = 11.sp, color = DarkNavBrown.copy(alpha = 0.7f), fontWeight = FontWeight.Medium)
                    Text(formatCurrency(totalExpense), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AccentOrange)
                }
            }
        }
    }
}

@Composable
fun TransactionItem(item: TransactionWithDetails) {
    val isIncome = item.category.type == "INCOME"
    val (emoji, categoryColor) = getCategoryEmojiAndColor(item.category.name, isIncome)
    val amountColor = if (isIncome) Color(0xFF10B981) else Color(0xFFF43F5E)
    val amountPrefix = if (isIncome) "+" else "-"

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Circular Badge dengan emoji
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .background(
                        color = categoryColor.copy(alpha = 0.15f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(text = emoji, fontSize = 20.sp)
            }
            
            Spacer(modifier = Modifier.width(14.dp))
            
            // Detail Kategori, Dompet, dan Tanggal
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.category.name, 
                    fontWeight = FontWeight.ExtraBold, 
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = item.wallet.name, 
                    style = MaterialTheme.typography.bodySmall, 
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = formatDate(item.transaction.date), 
                    style = MaterialTheme.typography.bodySmall, 
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    maxLines = 1
                )
                if (!item.transaction.note.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = item.transaction.note, 
                        style = MaterialTheme.typography.bodySmall, 
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // Nominal Transaksi
            Text(
                text = "$amountPrefix ${formatCurrency(item.transaction.amount)}",
                fontWeight = FontWeight.Black,
                color = amountColor,
                fontSize = 16.sp
            )
        }
    }
}

// Fungsi Bantuan (Helper)
fun formatCurrency(amount: Double): String {
    val format = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
    if (amount % 1.0 == 0.0) {
        format.maximumFractionDigits = 0
    } else {
        format.maximumFractionDigits = 2
    }
    return format.format(amount).replace("Rp", "Rp ")
}

fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID"))
    return sdf.format(Date(timestamp))
}

@Composable
fun WalletCard(
    wallet: com.dhandy.smartbudget.data.local.entity.WalletEntity,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(170.dp)
            .height(105.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = WarmYellowCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = wallet.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = DarkNavBrown,
                        maxLines = 1
                    )
                    Text(
                        text = "💳", 
                        fontSize = 16.sp
                    )
                }
                
                Column {
                    Text(
                        text = "SALDO",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = DarkNavBrown.copy(alpha = 0.6f),
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = formatCurrency(wallet.currentBalance),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = DarkNavBrown
                    )
                }
            }
        }
    }
}

@Composable
fun TargetCard(
    target: com.dhandy.smartbudget.data.local.entity.TargetEntity,
    onClick: () -> Unit
) {
    val progress = if (target.targetAmount > 0) (target.currentAmount / target.targetAmount).toFloat().coerceIn(0f, 1f) else 0f
    val percentage = (progress * 100).toInt()

    Card(
        modifier = Modifier
            .width(190.dp)
            .height(115.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        border = BorderStroke(1.dp, WarmCardSubtle),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = target.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = DarkNavBrown,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                    Text("🎯", fontSize = 14.sp)
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${formatCurrency(target.currentAmount)} / ${formatCurrency(target.targetAmount)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MutedTextBrown,
                    maxLines = 1
                )
            }
            
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .background(
                            color = WarmBackground,
                            shape = RoundedCornerShape(4.dp)
                        )
                ) {
                    if (progress > 0f) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progress)
                                .fillMaxHeight()
                                .background(
                                    color = AccentOrange,
                                    shape = RoundedCornerShape(4.dp)
                                )
                        )
                    }
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Terkumpul $percentage%",
                        style = androidx.compose.ui.text.TextStyle(fontSize = 11.sp),
                        fontWeight = FontWeight.Bold,
                        color = DarkNavBrown.copy(alpha = 0.8f)
                    )
                    if (progress >= 1f) {
                        Text(
                            text = "SELESAI 🎉",
                            style = androidx.compose.ui.text.TextStyle(fontSize = 9.sp),
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF047857)
                        )
                    }
                }
            }
        }
    }
}

fun getCategoryEmojiAndColor(categoryName: String, isIncome: Boolean): Pair<String, Color> {
    val emoji = when (categoryName.lowercase()) {
        "gaji" -> "💰"
        "makanan", "makan", "kuliner" -> "🍔"
        "transportasi", "transport", "ojek" -> "🚗"
        "hiburan", "nonton", "game" -> "🎬"
        "transfer masuk", "transfer" -> "📥"
        "transfer keluar" -> "📤"
        "tabungan target" -> "🎯"
        else -> if (isIncome) "💰" else "🛍️"
    }
    val color = if (isIncome) {
        Color(0xFF10B981) // Emerald Green
    } else {
        when (categoryName.lowercase()) {
            "makanan", "makan", "kuliner" -> Color(0xFFF97316) // Orange
            "transportasi", "transport", "ojek" -> Color(0xFF06B6D4) // Cyan
            "hiburan", "nonton", "game" -> Color(0xFFA855F7) // Purple
            "tabungan target" -> Color(0xFFEC4899) // Pink
            else -> Color(0xFFF43F5E) // Red/Rose
        }
    }
    return Pair(emoji, color)
}

@Composable
fun ThemeCoinButton(
    coinRotation: Float,
    isAnimating: Boolean,
    onPositioned: (Offset) -> Unit,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val angle = ((coinRotation % 360f) + 360f) % 360f
    val isBackFace = angle in 90f..270f
    val normalizedPhase = (((coinRotation % 180f) + 180f) % 180f) / 180f
    val hop = kotlin.math.sin(normalizedPhase * Math.PI).toFloat()

    Box(
        modifier = modifier
            .onGloballyPositioned { coordinates ->
                val pos = coordinates.positionInRoot()
                val size = coordinates.size
                onPositioned(Offset(pos.x + size.width / 2f, pos.y + size.height / 2f))
            }
            .size(44.dp)
            .graphicsLayer {
                rotationY = coinRotation
                cameraDistance = 16f * density
                translationY = -hop * 16.dp.toPx()
                scaleX = 1f + hop * 0.15f
                scaleY = 1f + hop * 0.15f
                shadowElevation = (3.dp + (hop * 10).dp).toPx()
                shape = CircleShape
                clip = false
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                if (!isAnimating) {
                    onToggle()
                }
            },
        contentAlignment = Alignment.Center
    ) {
        if (isBackFace) {
            // Moon face (Dark mode — Sleek Jet Black & Pure White Ring)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { rotationY = 180f }
                    .background(
                        color = Color.Black,
                        shape = CircleShape
                    )
                    .border(
                        width = 2.dp,
                        color = Color.White,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(33.dp)
                        .border(1.dp, Color(0xFF555555), CircleShape)
                )
                Text(
                    text = "🌙",
                    fontSize = 17.sp
                )
            }
        } else {
            // Sun face (Light mode — Crisp Pure White & Jet Black Ring)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        color = Color.White,
                        shape = CircleShape
                    )
                    .border(
                        width = 2.dp,
                        color = Color.Black,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(33.dp)
                        .border(1.dp, Color(0xFFDDDDDD), CircleShape)
                )
                Text(
                    text = "☀️",
                    fontSize = 17.sp
                )
            }
        }
    }
}
