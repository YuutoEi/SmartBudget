package com.dhandy.smartbudget.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dhandy.smartbudget.data.local.model.TransactionWithDetails
import com.dhandy.smartbudget.ui.viewmodel.TransactionViewModel
import com.dhandy.smartbudget.ui.theme.*
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllTransactionsScreen(
    viewModel: TransactionViewModel,
    onNavigateBack: () -> Unit
) {
    val transactions by viewModel.transactions.collectAsState()
    
    // State untuk filter bulan dan tahun
    val calendar = remember { Calendar.getInstance() }
    val currentYear = calendar.get(Calendar.YEAR)
    val currentMonth = calendar.get(Calendar.MONTH) // 0-indexed (0 = Jan, 11 = Des)

    var selectedMonth by remember { mutableStateOf(currentMonth) }
    var selectedYear by remember { mutableStateOf(currentYear) }
    
    // Extract unique years from transactions, default to currentYear if empty
    val years = remember(transactions) {
        val cal = Calendar.getInstance()
        val uniqueYears = transactions.map {
            cal.timeInMillis = it.transaction.date
            cal.get(Calendar.YEAR)
        }.distinct().sorted()
        uniqueYears.ifEmpty { listOf(currentYear) }
    }
    
    LaunchedEffect(years) {
        if (selectedYear !in years) {
            selectedYear = years.lastOrNull() ?: currentYear
        }
    }
    
    // Extract unique months for the selected year
    val availableMonths = remember(transactions, selectedYear) {
        val cal = Calendar.getInstance()
        val uniqueMonths = transactions.filter {
            cal.timeInMillis = it.transaction.date
            cal.get(Calendar.YEAR) == selectedYear
        }.map {
            cal.timeInMillis = it.transaction.date
            cal.get(Calendar.MONTH)
        }.distinct().sorted()
        uniqueMonths.ifEmpty { listOf(currentMonth) }
    }
    
    LaunchedEffect(availableMonths) {
        if (selectedMonth !in availableMonths) {
            selectedMonth = availableMonths.firstOrNull() ?: currentMonth
        }
    }
    
    var yearDropdownExpanded by remember { mutableStateOf(false) }
    
    val months = listOf(
        "Jan", "Feb", "Mar", "Apr", "Mei", "Jun", 
        "Jul", "Agt", "Sep", "Okt", "Nov", "Des"
    )

    // Memfilter transaksi secara reaktif berdasarkan bulan & tahun terpilih
    val filteredTransactions = remember(transactions, selectedMonth, selectedYear) {
        val cal = Calendar.getInstance()
        transactions.filter {
            cal.timeInMillis = it.transaction.date
            val tMonth = cal.get(Calendar.MONTH)
            val tYear = cal.get(Calendar.YEAR)
            tMonth == selectedMonth && tYear == selectedYear
        }
    }
    
    // Rekap Pemasukan & Pengeluaran bulanan dinamis
    val totalIncome = remember(filteredTransactions) {
        filteredTransactions.filter { it.category.type == "INCOME" }.sumOf { it.transaction.amount }
    }
    val totalExpense = remember(filteredTransactions) {
        filteredTransactions.filter { it.category.type == "EXPENSE" }.sumOf { it.transaction.amount }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            // Header Minimalis
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
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
                    
                    Spacer(modifier = Modifier.width(14.dp))
                    
                    Text(
                        text = "Riwayat",
                        style = TextStyle(
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    )
                }
                
                // Dropdown Pilih Tahun
                ExposedDropdownMenuBox(
                    expanded = yearDropdownExpanded,
                    onExpandedChange = { yearDropdownExpanded = it },
                    modifier = Modifier.width(110.dp)
                ) {
                    OutlinedTextField(
                        value = selectedYear.toString(),
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = yearDropdownExpanded) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElectricIndigo,
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                            focusedLabelColor = ElectricIndigo
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = yearDropdownExpanded,
                        onDismissRequest = { yearDropdownExpanded = false }
                    ) {
                        years.forEach { y ->
                            DropdownMenuItem(
                                text = { Text(y.toString(), fontWeight = FontWeight.Bold) },
                                onClick = {
                                    selectedYear = y
                                    yearDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Pemilih Bulan Horizontal (LazyRow Swipable)
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(availableMonths.size) { index ->
                    val monthIndex = availableMonths[index]
                    val isMonthSelected = selectedMonth == monthIndex
                    Box(
                        modifier = Modifier
                            .width(64.dp)
                            .height(38.dp)
                            .background(
                                brush = if (isMonthSelected) {
                                    Brush.linearGradient(colors = listOf(ElectricIndigo, NeonPink))
                                } else {
                                    Brush.linearGradient(colors = listOf(Color.Transparent, Color.Transparent))
                                },
                                shape = RoundedCornerShape(19.dp)
                            )
                            .background(
                                color = if (isMonthSelected) Color.Transparent else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                shape = RoundedCornerShape(19.dp)
                            )
                            .clickable { selectedMonth = monthIndex },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = months.getOrElse(monthIndex) { "" },
                            fontWeight = if (isMonthSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isMonthSelected) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Rekap Bulanan Dinamis (Pemasukan vs Pengeluaran)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Rekap Masuk
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(Color.Black.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                        .padding(14.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("↙", color = Color(0xFF4ADE80), fontWeight = FontWeight.Black, fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Masuk", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.8f))
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = formatCurrency(totalIncome),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF4ADE80)
                        )
                    }
                }
                
                // Rekap Keluar
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(Color.Black.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                        .padding(14.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("↗", color = Color(0xFFFB7185), fontWeight = FontWeight.Black, fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Keluar", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.8f))
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = formatCurrency(totalExpense),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFFB7185)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Daftar Transaksi Terfilter
            if (filteredTransactions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📭", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Belum ada transaksi di bulan ini.", 
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredTransactions, key = { it.transaction.id }) { transactionWithDetails ->
                        TransactionItem(transactionWithDetails)
                    }
                }
            }
        }
    }
}
