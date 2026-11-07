package com.dhandy.smartbudget.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dhandy.smartbudget.data.local.entity.CategoryEntity
import com.dhandy.smartbudget.data.local.entity.TransactionEntity
import com.dhandy.smartbudget.data.local.entity.WalletEntity
import com.dhandy.smartbudget.ui.theme.*
import com.dhandy.smartbudget.ui.viewmodel.TransactionViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    viewModel: TransactionViewModel,
    onNavigateBack: () -> Unit
) {
    val wallets by viewModel.wallets.collectAsState()
    val categories by viewModel.categories.collectAsState()

    val displayCategories = remember(categories) {
        categories.filter {
            it.name != "Transfer Keluar" &&
            it.name != "Transfer Masuk" &&
            it.name != "Tabungan Target"
        }
    }

    var selectedType by remember { mutableStateOf("EXPENSE") } // "EXPENSE" or "INCOME"

    val currentCategories = remember(displayCategories, selectedType) {
        displayCategories.filter { it.type == selectedType }
    }

    // Input state
    var amountDigits by remember { mutableStateOf("") }
    var selectedWallet by remember { mutableStateOf<WalletEntity?>(null) }
    var selectedCategory by remember { mutableStateOf<CategoryEntity?>(null) }
    var note by remember { mutableStateOf("") }
    var showNoteDialog by remember { mutableStateOf(false) }

    // Auto-select first wallet if none selected
    LaunchedEffect(wallets) {
        if (selectedWallet == null && wallets.isNotEmpty()) {
            selectedWallet = wallets.first()
        }
    }
    // Auto-select first category of that type
    LaunchedEffect(currentCategories, selectedType) {
        if (currentCategories.isNotEmpty() && (selectedCategory == null || selectedCategory?.type != selectedType)) {
            selectedCategory = currentCategories.first()
        }
    }

    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = System.currentTimeMillis())
    var showDatePicker by remember { mutableStateOf(false) }

    val formatter = remember { SimpleDateFormat("dd MMM yyyy", Locale("id", "ID")) }
    val selectedDateText = datePickerState.selectedDateMillis?.let {
        formatter.format(Date(it))
    } ?: "Hari Ini"

    val amountDouble = (amountDigits.toDoubleOrNull() ?: 0.0)
    val isFormValid = amountDouble > 0.0 && selectedWallet != null && selectedCategory != null

    // Helper currency display
    val formattedAmount = remember(amountDigits) {
        if (amountDigits.isEmpty()) "0"
        else {
            val num = amountDigits.toDoubleOrNull() ?: 0.0
            val nf = NumberFormat.getNumberInstance(Locale("id", "ID"))
            nf.maximumFractionDigits = 0
            nf.format(num)
        }
    }

    // Modal Bottom Sheet Layout: Background gelap transparan di luar sheet, Sheet meluncur dari bawah dengan Rounded Top Corners
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onNavigateBack
            ),
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {} // Mencegah klik di dalam sheet menutup dialog
                ),
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            color = MaterialTheme.colorScheme.background,
            shadowElevation = 16.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Drag Handle Bar minimalis di bagian atas
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(42.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(MaterialTheme.colorScheme.outlineVariant)
                    )
                }

                // 1. Top Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (selectedType == "EXPENSE") "Catat Pengeluaran" else "Catat Pemasukan",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Date Picker trigger button
                        IconButton(
                            onClick = { showDatePicker = true },
                            modifier = Modifier
                                .size(40.dp)
                                .background(MaterialTheme.colorScheme.surface, CircleShape)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), CircleShape)
                        ) {
                            Icon(
                                Icons.Filled.DateRange,
                                contentDescription = "Pilih Tanggal",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Close Button (Silang)
                        IconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier
                                .size(40.dp)
                                .background(MaterialTheme.colorScheme.surface, CircleShape)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), CircleShape)
                        ) {
                            Icon(
                                Icons.Outlined.Close,
                                contentDescription = "Tutup",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

            // 2. Type Selector Tabs (Income / Expense)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(23.dp))
                    .padding(4.dp)
            ) {
                Row(modifier = Modifier.fillMaxSize()) {
                    val isIncome = selectedType == "INCOME"
                    val isExpense = selectedType == "EXPENSE"

                    // Income Tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isIncome) AccentOrange else Color.Transparent)
                            .clickable { selectedType = "INCOME" },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Income",
                            fontSize = 14.sp,
                            fontWeight = if (isIncome) FontWeight.Bold else FontWeight.Medium,
                            color = if (isIncome) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Expense Tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isExpense) AccentOrange else Color.Transparent)
                            .clickable { selectedType = "EXPENSE" },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Expense",
                            fontSize = 14.sp,
                            fontWeight = if (isExpense) FontWeight.Bold else FontWeight.Medium,
                            color = if (isExpense) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // 3. Wallet Selector Card ("Transfer From" / "Akun Dompet")
            if (wallets.isNotEmpty()) {
                var showWalletPickerSheet by remember { mutableStateOf(false) }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(WarmYellowCard)
                        .clickable { showWalletPickerSheet = true }
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Gunakan Dompet",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = DarkNavBrown.copy(alpha = 0.7f)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(AccentOrange, RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("💳", fontSize = 18.sp)
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = selectedWallet?.name ?: "Pilih Dompet",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = DarkNavBrown
                                    )
                                    val nf = NumberFormat.getNumberInstance(Locale("id", "ID"))
                                    val balanceText = selectedWallet?.let { "Rp " + nf.format(it.currentBalance) } ?: ""
                                    Text(
                                        text = balanceText,
                                        fontSize = 12.sp,
                                        color = DarkNavBrown.copy(alpha = 0.8f),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        Text(
                            text = "Ganti >",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = DarkNavBrown.copy(alpha = 0.7f)
                        )
                    }
                }

                if (showWalletPickerSheet) {
                    AlertDialog(
                        onDismissRequest = { showWalletPickerSheet = false },
                        title = { Text("Pilih Dompet", fontWeight = FontWeight.Bold) },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                wallets.forEach { w ->
                                    val isSelected = w.id == selectedWallet?.id
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (isSelected) WarmYellowLight else Color.Transparent)
                                            .clickable {
                                                selectedWallet = w
                                                showWalletPickerSheet = false
                                            }
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(w.name, fontWeight = FontWeight.Bold)
                                            val nf = NumberFormat.getNumberInstance(Locale("id", "ID"))
                                            Text("Rp " + nf.format(w.currentBalance), fontSize = 12.sp, color = MutedTextBrown)
                                        }
                                        if (isSelected) {
                                            Icon(Icons.Filled.Check, contentDescription = null, tint = AccentOrange)
                                        }
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { showWalletPickerSheet = false }) {
                                Text("Tutup")
                            }
                        }
                    )
                }
            }

            // 4. Large Nominal Display (Rp xxx.xxx)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Rp ",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formattedAmount,
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    // Cursor blinking-style bar
                    Box(
                        modifier = Modifier
                            .padding(start = 4.dp)
                            .width(2.dp)
                            .height(34.dp)
                            .background(AccentOrange)
                    )
                }

                // Sub-info: Tanggal & Catatan mini chip
                Row(
                    modifier = Modifier.padding(top = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                        modifier = Modifier.clickable { showDatePicker = true }
                    ) {
                        Text(
                            text = "📅 $selectedDateText",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                        modifier = Modifier.clickable { showNoteDialog = true }
                    ) {
                        Text(
                            text = if (note.isBlank()) "📝 + Catatan" else "📝 $note",
                            fontSize = 11.sp,
                            color = if (note.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // 5. Category Chips Carousel ("Quick Send" style)
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Pilih Kategori",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "${currentCategories.size} Kategori",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp)
                ) {
                    items(currentCategories, key = { it.id }) { cat ->
                        val isSelected = cat.id == selectedCategory?.id
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { selectedCategory = cat }
                                .padding(vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .background(
                                        if (isSelected) AccentOrange else MaterialTheme.colorScheme.surface,
                                        CircleShape
                                    )
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) AccentOrange else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                val (emoji, _) = getCategoryEmojiAndColor(cat.name, selectedType == "INCOME")
                                Text(
                                    text = emoji,
                                    fontSize = 22.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = cat.name,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) AccentOrange else MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // 6. Custom Phone Keypad (3x4 grid)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val row1 = listOf("1", "2", "3")
                val row2 = listOf("4", "5", "6")
                val row3 = listOf("7", "8", "9")

                KeypadRow(row1) { char ->
                    if (amountDigits.length < 12) amountDigits += char
                }
                KeypadRow(row2) { char ->
                    if (amountDigits.length < 12) amountDigits += char
                }
                KeypadRow(row3) { char ->
                    if (amountDigits.length < 12) amountDigits += char
                }

                // Row 4: "000", "0", Delete / Save
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // "000" shortcut button
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .clickable {
                                if (amountDigits.isNotBlank() && amountDigits.length < 10) {
                                    amountDigits += "000"
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "000",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // "0" button
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .clickable {
                                if (amountDigits.isNotBlank() && amountDigits.length < 12) {
                                    amountDigits += "0"
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "0",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Backspace button
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .clickable {
                                if (amountDigits.isNotEmpty()) {
                                    amountDigits = amountDigits.dropLast(1)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.Close,
                            contentDescription = "Hapus Angka",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Submit Action button (Orange button on right)
                    Box(
                        modifier = Modifier
                            .weight(1.2f)
                            .height(54.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isFormValid) AccentOrange else MaterialTheme.colorScheme.surfaceVariant)
                            .clickable(enabled = isFormValid) {
                                val selectedDateMillis = datePickerState.selectedDateMillis
                                val finalDateMillis = if (selectedDateMillis != null) {
                                    val calendar = Calendar.getInstance()
                                    val currentLocalTime = Calendar.getInstance()
                                    val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                                        timeInMillis = selectedDateMillis
                                    }
                                    calendar.set(Calendar.YEAR, utcCal.get(Calendar.YEAR))
                                    calendar.set(Calendar.MONTH, utcCal.get(Calendar.MONTH))
                                    calendar.set(Calendar.DAY_OF_MONTH, utcCal.get(Calendar.DAY_OF_MONTH))
                                    calendar.set(Calendar.HOUR_OF_DAY, currentLocalTime.get(Calendar.HOUR_OF_DAY))
                                    calendar.set(Calendar.MINUTE, currentLocalTime.get(Calendar.MINUTE))
                                    calendar.set(Calendar.SECOND, currentLocalTime.get(Calendar.SECOND))
                                    calendar.set(Calendar.MILLISECOND, currentLocalTime.get(Calendar.MILLISECOND))
                                    calendar.timeInMillis
                                } else {
                                    System.currentTimeMillis()
                                }

                                val newTransaction = TransactionEntity(
                                    walletId = selectedWallet!!.id,
                                    categoryId = selectedCategory!!.id,
                                    amount = amountDouble,
                                    date = finalDateMillis,
                                    note = note.takeIf { it.isNotBlank() }
                                )
                                viewModel.addTransaction(newTransaction)
                                onNavigateBack()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Filled.Check,
                                contentDescription = "Simpan",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Kirim",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

    // Modal Date Picker
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Pilih")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Batal")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Modal Edit Catatan
    if (showNoteDialog) {
        var tempNote by remember { mutableStateOf(note) }
        AlertDialog(
            onDismissRequest = { showNoteDialog = false },
            title = { Text("Tambah Catatan", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = tempNote,
                    onValueChange = { tempNote = it },
                    label = { Text("Keterangan transaksi") },
                    singleLine = false,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        note = tempNote
                        showNoteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentOrange)
                ) {
                    Text("Simpan", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showNoteDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
private fun KeypadRow(
    chars: List<String>,
    onClick: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        chars.forEach { char ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(54.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .clickable { onClick(char) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = char,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
