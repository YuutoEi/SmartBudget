package com.dhandy.smartbudget.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.dhandy.smartbudget.data.local.entity.CategoryEntity
import com.dhandy.smartbudget.data.local.entity.TransactionEntity
import com.dhandy.smartbudget.data.local.entity.WalletEntity
import com.dhandy.smartbudget.ui.viewmodel.TransactionViewModel
import com.dhandy.smartbudget.ui.theme.*
import com.dhandy.smartbudget.ui.utils.ThousandsSeparatorVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.Alignment
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    viewModel: TransactionViewModel,
    onNavigateBack: () -> Unit
) {
    // Membaca state dompet dan kategori dari ViewModel
    val wallets by viewModel.wallets.collectAsState()
    val categories by viewModel.categories.collectAsState()

    // Menyaring dan mengelompokkan kategori agar kategori sistem tidak muncul
    val displayCategories = remember(categories) {
        categories.filter {
            it.name != "Transfer Keluar" &&
            it.name != "Transfer Masuk" &&
            it.name != "Tabungan Target"
        }
    }
    val incomeCategories = remember(displayCategories) {
        displayCategories.filter { it.type == "INCOME" }
    }
    val expenseCategories = remember(displayCategories) {
        displayCategories.filter { it.type == "EXPENSE" }
    }

    // State untuk Form Input
    var amount by remember { mutableStateOf("") }
    var selectedWallet by remember { mutableStateOf<WalletEntity?>(null) }
    var selectedCategory by remember { mutableStateOf<CategoryEntity?>(null) }
    var note by remember { mutableStateOf("") }

    // State untuk DatePicker bawaan Compose
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = System.currentTimeMillis())

    // Format tanggal untuk ditampilkan ke pengguna
    val formatter = remember { SimpleDateFormat("dd MMM yyyy", Locale("id", "ID")) }
    val selectedDateText = datePickerState.selectedDateMillis?.let {
        formatter.format(Date(it))
    } ?: "Pilih Tanggal"

    // Validasi form: Pastikan nominal tidak kosong, berupa angka valid, dan dompet serta kategori sudah dipilih
    val isFormValid = amount.isNotBlank() && amount.toDoubleOrNull() != null &&
            selectedWallet != null && selectedCategory != null && datePickerState.selectedDateMillis != null

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Minimalis Kustom
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = CircleShape
                        )
                        .size(40.dp)
                ) {
                    Icon(
                        Icons.Filled.ArrowBack, 
                        contentDescription = "Kembali",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Text(
                    text = "Tambah Transaksi",
                    style = TextStyle(
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                )
            }

            // 1. Input Nominal Transaksi
            OutlinedTextField(
                value = amount,
                onValueChange = { input ->
                    amount = input.replace(Regex("[^0-9]"), "")
                },
                label = { Text("Nominal") },
                prefix = { Text("Rp ", fontWeight = FontWeight.Bold, color = ElectricIndigo) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                visualTransformation = ThousandsSeparatorVisualTransformation(),
                shape = RoundedCornerShape(18.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ElectricIndigo,
                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                    focusedLabelColor = ElectricIndigo
                ),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // 2. Dropdown Pilih Dompet
            var expandedWallet by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = expandedWallet,
                onExpandedChange = { expandedWallet = it },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = selectedWallet?.name ?: "Pilih Dompet",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Dompet Asal") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedWallet) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElectricIndigo,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                        focusedLabelColor = ElectricIndigo
                    ),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = expandedWallet,
                    onDismissRequest = { expandedWallet = false }
                ) {
                    wallets.forEach { wallet ->
                        DropdownMenuItem(
                            text = { Text(wallet.name) },
                            onClick = {
                                selectedWallet = wallet
                                expandedWallet = false
                            }
                        )
                    }
                }
            }

            // 3. Dropdown Pilih Kategori
            var expandedCategory by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = expandedCategory,
                onExpandedChange = { expandedCategory = it },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = selectedCategory?.name ?: "Pilih Kategori",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Kategori") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCategory) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElectricIndigo,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                        focusedLabelColor = ElectricIndigo
                    ),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = expandedCategory,
                    onDismissRequest = { expandedCategory = false }
                ) {
                    if (incomeCategories.isNotEmpty()) {
                        DropdownMenuItem(
                            text = { Text("Pemasukan (Income)", fontWeight = FontWeight.Bold, color = ElectricIndigo) },
                            onClick = {},
                            enabled = false
                        )
                        incomeCategories.forEach { category ->
                            DropdownMenuItem(
                                text = { Text("  " + category.name) },
                                onClick = {
                                    selectedCategory = category
                                    expandedCategory = false
                                }
                            )
                        }
                    }
                    if (expenseCategories.isNotEmpty()) {
                        if (incomeCategories.isNotEmpty()) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        }
                        DropdownMenuItem(
                            text = { Text("Pengeluaran (Expense)", fontWeight = FontWeight.Bold, color = NeonPink) },
                            onClick = {},
                            enabled = false
                        )
                        expenseCategories.forEach { category ->
                            DropdownMenuItem(
                                text = { Text("  " + category.name) },
                                onClick = {
                                    selectedCategory = category
                                    expandedCategory = false
                                }
                            )
                        }
                    }
                }
            }

            // 4. Pemilih Tanggal
            OutlinedTextField(
                value = selectedDateText,
                onValueChange = {},
                readOnly = true,
                label = { Text("Tanggal Transaksi") },
                trailingIcon = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Default.DateRange, contentDescription = "Pilih Tanggal")
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ElectricIndigo,
                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                    focusedLabelColor = ElectricIndigo
                ),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            )

            if (showDatePicker) {
                DatePickerDialog(
                    onDismissRequest = { showDatePicker = false },
                    confirmButton = {
                        TextButton(onClick = { showDatePicker = false }) {
                            Text("OK")
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

            // 5. Input Catatan Opsional
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Catatan (Opsional)") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ElectricIndigo,
                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                    focusedLabelColor = ElectricIndigo
                ),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Tombol Simpan
            Button(
                onClick = {
                    if (isFormValid) {
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
                            amount = amount.toDouble(),
                            date = finalDateMillis,
                            note = note.takeIf { it.isNotBlank() }
                        )
                        viewModel.addTransaction(newTransaction)
                        onNavigateBack() // Kembali ke Dashboard setelah menyimpan
                    }
                },
                enabled = isFormValid,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = if (isFormValid) {
                                Brush.linearGradient(colors = listOf(ElectricIndigo, NeonPink))
                            } else {
                                Brush.linearGradient(colors = listOf(Color.Gray.copy(alpha = 0.3f), Color.Gray.copy(alpha = 0.3f)))
                            },
                            shape = RoundedCornerShape(27.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Simpan Transaksi 🚀",
                        fontWeight = FontWeight.Bold,
                        color = if (isFormValid) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}
