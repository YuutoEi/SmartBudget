package com.dhandy.smartbudget.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dhandy.smartbudget.data.local.model.TransactionWithDetails
import com.dhandy.smartbudget.ui.viewmodel.TransactionViewModel
import com.dhandy.smartbudget.ui.theme.*
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

data class CategoryGroupData(
    val name: String,
    val totalAmount: Double,
    val count: Int,
    val percentage: Float,
    val color: Color,
    val emoji: String,
    val budgetLimit: Double? = null
)

data class DayTrendData(
    val dayName: String,
    val amount: Double
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    viewModel: TransactionViewModel,
    onNavigateBack: () -> Unit
) {
    val transactions by viewModel.transactions.collectAsState()
    val wallets by viewModel.wallets.collectAsState()

    var selectedTimePeriod by remember { mutableStateOf(0) } // 0 = Semua, 1 = Bulan Ini, 2 = Minggu Ini
    var selectedType by remember { mutableStateOf(0) } // 0 = Pengeluaran (EXPENSE), 1 = Pemasukan (INCOME)
    var selectedWalletId by remember { mutableStateOf<Int?>(null) } // null = Semua Dompet
    var selectedChartTab by remember { mutableStateOf(0) } // 0 = Distribusi (Donut), 1 = Tren (Line)

    // Helper Filter Transaksi
    val filteredTransactions = remember(transactions, selectedTimePeriod, selectedWalletId) {
        val cal = Calendar.getInstance()
        val currentMonth = cal.get(Calendar.MONTH)
        val currentYear = cal.get(Calendar.YEAR)
        val sevenDaysAgo = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L

        transactions.filter { item ->
            // Filter Dompet
            val matchesWallet = selectedWalletId == null || item.transaction.walletId == selectedWalletId
            
            // Filter Waktu
            val matchesTime = when (selectedTimePeriod) {
                1 -> { // Bulan Ini
                    val transCal = Calendar.getInstance().apply { timeInMillis = item.transaction.date }
                    transCal.get(Calendar.MONTH) == currentMonth && transCal.get(Calendar.YEAR) == currentYear
                }
                2 -> { // Minggu Ini
                    item.transaction.date >= sevenDaysAgo
                }
                else -> true // Semua
            }

            matchesWallet && matchesTime
        }
    }

    // Rekap Nominal
    val totalIncome = remember(filteredTransactions) {
        filteredTransactions.filter { it.category.type == "INCOME" }.sumOf { it.transaction.amount }
    }
    val totalExpense = remember(filteredTransactions) {
        filteredTransactions.filter { it.category.type == "EXPENSE" }.sumOf { it.transaction.amount }
    }
    val netBalance = totalIncome - totalExpense
    val savingRate = if (totalIncome > 0) ((netBalance / totalIncome) * 100).toInt() else 0

    // Grouping Kategori untuk Donut Chart
    val categoryGroups = remember(filteredTransactions, selectedType) {
        val targetType = if (selectedType == 0) "EXPENSE" else "INCOME"
        val typeFiltered = filteredTransactions.filter { it.category.type == targetType }
        val totalTypeAmount = typeFiltered.sumOf { it.transaction.amount }

        typeFiltered.groupBy { it.category.name }
            .mapValues { entry ->
                val amount = entry.value.sumOf { it.transaction.amount }
                val count = entry.value.size
                val categoryObj = entry.value.first().category
                val (emoji, color) = getAnalyticsCategoryEmojiAndColor(categoryObj.name, targetType == "INCOME")
                CategoryGroupData(
                    name = categoryObj.name,
                    totalAmount = amount,
                    count = count,
                    percentage = if (totalTypeAmount > 0) (amount / totalTypeAmount).toFloat() else 0f,
                    color = color,
                    emoji = emoji,
                    budgetLimit = categoryObj.budgetLimit
                )
            }
            .values
            .sortedByDescending { it.totalAmount }
    }

    val totalSelectedTypeAmount = remember(categoryGroups) {
        categoryGroups.sumOf { it.totalAmount }
    }

    // Data Harian 7 Hari Terakhir untuk Line Chart
    val last7DaysData = remember(filteredTransactions, selectedType) {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        val days = mutableListOf<DayTrendData>()
        val sdf = SimpleDateFormat("EEE", Locale("id", "ID"))
        val targetType = if (selectedType == 0) "EXPENSE" else "INCOME"

        for (i in 6 downTo 0) {
            val targetCal = Calendar.getInstance().apply {
                timeInMillis = cal.timeInMillis
                add(Calendar.DAY_OF_YEAR, -i)
            }
            val startOfDay = targetCal.timeInMillis
            val endOfDay = startOfDay + 24 * 60 * 60 * 1000L - 1L

            val dailyAmount = filteredTransactions
                .filter { it.category.type == targetType && it.transaction.date in startOfDay..endOfDay }
                .sumOf { it.transaction.amount }

            days.add(
                DayTrendData(
                    dayName = sdf.format(targetCal.time),
                    amount = dailyAmount
                )
            )
        }
        days
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
            Spacer(modifier = Modifier.height(16.dp))
            
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
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
                
                Spacer(modifier = Modifier.width(14.dp))
                
                Text(
                    text = "Analisis",
                    style = TextStyle(
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Scrollable Content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Selector Periode Waktu
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            shape = RoundedCornerShape(22.dp)
                        )
                        .padding(4.dp)
                ) {
                    val periods = listOf("Semua", "Bulan Ini", "Minggu Ini")
                    periods.forEachIndexed { index, title ->
                        val isActive = selectedTimePeriod == index
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .background(
                                    brush = if (isActive) {
                                        Brush.linearGradient(colors = listOf(ElectricIndigo, CyberTeal))
                                    } else {
                                        Brush.linearGradient(colors = listOf(Color.Transparent, Color.Transparent))
                                    },
                                    shape = RoundedCornerShape(18.dp)
                                )
                                .clickable { selectedTimePeriod = index },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = title,
                                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                                color = if (isActive) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                // 2. Horizontal Filter Dompet
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Filter berdasarkan Dompet",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        item {
                            FilterChip(
                                selected = selectedWalletId == null,
                                onClick = { selectedWalletId = null },
                                label = { Text("Semua Dompet", fontWeight = FontWeight.Bold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ElectricIndigo,
                                    selectedLabelColor = Color.White
                                ),
                                shape = RoundedCornerShape(16.dp)
                            )
                        }
                        items(wallets) { wallet ->
                            FilterChip(
                                selected = selectedWalletId == wallet.id,
                                onClick = { selectedWalletId = wallet.id },
                                label = { Text(wallet.name, fontWeight = FontWeight.Bold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ElectricIndigo,
                                    selectedLabelColor = Color.White
                                ),
                                shape = RoundedCornerShape(16.dp)
                            )
                        }
                    }
                }

                // 3. Ringkasan Statistika Card (Glassmorphic)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "SALDO BERSIH",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = formatCurrency(netBalance),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Black,
                                    color = if (netBalance >= 0) EmeraldGreen else CoralRed
                                )
                            }
                            
                            Box(
                                modifier = Modifier
                                    .background(
                                        color = (if (savingRate >= 0) EmeraldGreen else CoralRed).copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = if (savingRate >= 0) "Rasio Tabung: +$savingRate%" else "Defisit: $savingRate%",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (savingRate >= 0) EmeraldGreen else CoralRed
                                )
                            }
                        }

                        Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                        Row(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "TOTAL MASUK",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = formatCurrency(totalIncome),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldGreen
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "TOTAL KELUAR",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = formatCurrency(totalExpense),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = CoralRed
                                )
                            }
                        }
                    }
                }

                // 4. Tab Selector Tipe Transaksi (Pemasukan vs Pengeluaran)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val types = listOf("Pengeluaran ↗", "Pemasukan ↙")
                    types.forEachIndexed { index, title ->
                        val isActive = selectedType == index
                        val activeColor = if (index == 0) CoralRed else EmeraldGreen
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .background(
                                    color = if (isActive) activeColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .border(
                                    width = 1.5.dp,
                                    color = if (isActive) activeColor else Color.Transparent,
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .clickable { selectedType = index },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = title,
                                fontWeight = FontWeight.Bold,
                                color = if (isActive) activeColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                // 5. Card Grafis Visual Utama (Donut vs Line)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Sub-Tab Switcher inside Chart Card
                        Row(
                            modifier = Modifier
                                .width(220.dp)
                                .height(38.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    shape = RoundedCornerShape(19.dp)
                                )
                                .padding(3.dp)
                        ) {
                            val tabs = listOf("Kategori", "Tren")
                            tabs.forEachIndexed { index, tabTitle ->
                                val isTabActive = selectedChartTab == index
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .background(
                                            color = if (isTabActive) MaterialTheme.colorScheme.surface else Color.Transparent,
                                            shape = RoundedCornerShape(16.dp)
                                        )
                                        .clickable { selectedChartTab = index },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = tabTitle,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isTabActive) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }

                        // Chart Rendering Area
                        if (selectedChartTab == 0) {
                            // Donut Chart
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .padding(vertical = 12.dp)
                                    .size(200.dp)
                            ) {
                                // Animated progress
                                val sweepProgress by animateFloatAsState(
                                    targetValue = 1f,
                                    animationSpec = tween(durationMillis = 1000)
                                )

                                Canvas(modifier = Modifier.size(200.dp)) {
                                    var startAngle = -90f
                                    val strokeWidth = 24.dp.toPx()
                                    val sizePx = size.minDimension
                                    val padding = strokeWidth / 2
                                    val drawRect = Rect(
                                        padding, padding, sizePx - padding, sizePx - padding
                                    )

                                    if (categoryGroups.isEmpty()) {
                                        drawArc(
                                            color = Color.Gray.copy(alpha = 0.2f),
                                            startAngle = 0f,
                                            sweepAngle = 360f,
                                            useCenter = false,
                                            style = Stroke(width = strokeWidth),
                                            size = Size(drawRect.width, drawRect.height),
                                            topLeft = Offset(drawRect.left, drawRect.top)
                                        )
                                    } else {
                                        categoryGroups.forEach { category ->
                                            val sweepAngle = category.percentage * 360f * sweepProgress
                                            if (sweepAngle > 0f) {
                                                drawArc(
                                                    color = category.color,
                                                    startAngle = startAngle,
                                                    sweepAngle = sweepAngle,
                                                    useCenter = false,
                                                    style = Stroke(width = strokeWidth, cap = StrokeCap.Butt),
                                                    size = Size(drawRect.width, drawRect.height),
                                                    topLeft = Offset(drawRect.left, drawRect.top)
                                                )
                                                startAngle += sweepAngle
                                            }
                                        }
                                    }
                                }

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = if (selectedType == 0) "Total Keluar" else "Total Masuk",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = formatCurrency(totalSelectedTypeAmount),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        } else {
                            // Line Chart (Tren)
                            val chartColor = if (selectedType == 0) CoralRed else EmeraldGreen
                            if (last7DaysData.all { it.amount == 0.0 }) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "Belum ada data tren dalam 7 hari terakhir.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                    )
                                }
                            } else {
                                TrendLineChart(
                                    daysData = last7DaysData,
                                    color = chartColor,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }

                // 6. Section Rincian Kategori (Progress Bar per Kategori)
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(bottom = 24.dp)
                ) {
                    Text(
                        text = "Rincian Kategori",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    if (categoryGroups.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Tidak ada transaksi dalam kategori ini.",
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    } else {
                        categoryGroups.forEach { item ->
                            val pctText = (item.percentage * 100).toInt()
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(34.dp)
                                                .background(
                                                    color = item.color.copy(alpha = 0.15f),
                                                    shape = CircleShape
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(text = item.emoji, fontSize = 16.sp)
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = item.name,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                fontSize = 14.sp
                                            )
                                            Text(
                                                text = "${item.count} Transaksi",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                            )
                                        }
                                    }
                                    
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = formatCurrency(item.totalAmount),
                                            fontWeight = FontWeight.Black,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = "$pctText%",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = item.color
                                        )
                                    }
                                }

                                // Custom Horizontal Progress bar
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .background(
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                                            shape = RoundedCornerShape(3.dp)
                                        )
                                ) {
                                    if (item.percentage > 0f) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth(item.percentage)
                                                .fillMaxHeight()
                                                .background(
                                                    color = item.color,
                                                    shape = RoundedCornerShape(3.dp)
                                                )
                                        )
                                    }
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
fun TrendLineChart(
    daysData: List<DayTrendData>,
    color: Color,
    modifier: Modifier = Modifier
) {
    val maxVal = remember(daysData) {
        val maxAmount = daysData.maxOfOrNull { it.amount } ?: 0.0
        if (maxAmount == 0.0) 100000.0 else maxAmount * 1.1 // Headroom 10%
    }

    Column(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .padding(horizontal = 8.dp, vertical = 12.dp)
        ) {
            val width = size.width
            val height = size.height
            val spacing = width / 6

            // Draw grid lines
            val gridLines = 4
            for (i in 0..gridLines) {
                val gridY = height * i / gridLines
                drawLine(
                    color = Color.Gray.copy(alpha = 0.12f),
                    start = Offset(0f, gridY),
                    end = Offset(width, gridY),
                    strokeWidth = 1.dp.toPx()
                )
            }

            val points = daysData.mapIndexed { index, dayTrend ->
                val x = index * spacing
                val y = height - ((dayTrend.amount / maxVal).toFloat() * height)
                Offset(x, y)
            }

            if (points.isNotEmpty()) {
                val strokePath = Path().apply {
                    moveTo(points[0].x, points[0].y)
                    for (i in 0 until points.size - 1) {
                        val p1 = points[i]
                        val p2 = points[i + 1]
                        val controlPointX1 = p1.x + (p2.x - p1.x) / 2f
                        val controlPointY1 = p1.y
                        val controlPointX2 = p1.x + (p2.x - p1.x) / 2f
                        val controlPointY2 = p2.y

                        cubicTo(
                            controlPointX1, controlPointY1,
                            controlPointX2, controlPointY2,
                            p2.x, p2.y
                        )
                    }
                }

                val fillPath = Path().apply {
                    addPath(strokePath)
                    lineTo(points.last().x, height)
                    lineTo(points.first().x, height)
                    close()
                }

                // Gradient Fill
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            color.copy(alpha = 0.25f),
                            color.copy(alpha = 0.0f)
                        )
                    )
                )

                // Stroke Line
                drawPath(
                    path = strokePath,
                    color = color,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )

                // Circles on points
                points.forEach { point ->
                    drawCircle(
                        color = Color.White,
                        radius = 4.dp.toPx(),
                        center = point
                    )
                    drawCircle(
                        color = color,
                        radius = 2.5.dp.toPx(),
                        center = point
                    )
                }
            }
        }

        // X Axis labels
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            daysData.forEach { dayTrend ->
                Text(
                    text = dayTrend.dayName,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                    modifier = Modifier.width(36.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

private fun getAnalyticsCategoryEmojiAndColor(name: String, isIncome: Boolean): Pair<String, Color> {
    val emoji = when (name.lowercase(Locale.ROOT).trim()) {
        "gaji" -> "💰"
        "investasi" -> "📈"
        "bonus" -> "🎁"
        "penjualan" -> "🏷️"
        "lain-lain (pemasukan)" -> "💵"
        "makanan & minuman", "makanan", "makan", "kuliner" -> "🍔"
        "transportasi", "transport", "ojek" -> "🚗"
        "hiburan & rekreasi", "hiburan", "nonton", "game" -> "🎬"
        "belanja" -> "🛍️"
        "tagihan & utilitas", "tagihan", "listrik", "air" -> "⚡"
        "kesehatan", "obat", "dokter" -> "🏥"
        "pendidikan", "sekolah", "kuliah" -> "🎓"
        "tempat tinggal", "kos", "kontrakan" -> "🏠"
        "asuransi" -> "🛡️"
        "hobi & olahraga", "hobi", "olahraga" -> "⚽"
        "donasi & hadiah", "donasi", "sedekah" -> "💝"
        "lain-lain (pengeluaran)" -> "💸"
        "transfer masuk", "transfer" -> "📥"
        "transfer keluar" -> "📤"
        "tabungan target" -> "🎯"
        else -> if (isIncome) "💰" else "🛍️"
    }

    val color = if (isIncome) {
        when (name.lowercase(Locale.ROOT).trim()) {
            "gaji" -> EmeraldGreen
            "investasi" -> CyberTeal
            "bonus" -> NeonPink
            "penjualan" -> NeonOrange
            else -> EmeraldGreen
        }
    } else {
        when (name.lowercase(Locale.ROOT).trim()) {
            "makanan & minuman", "makanan", "makan", "kuliner" -> NeonOrange
            "transportasi", "transport", "ojek" -> NeonBlue
            "hiburan & rekreasi", "hiburan", "nonton", "game" -> NeonPurple
            "belanja" -> NeonPink
            "tagihan & utilitas", "tagihan" -> ElectricIndigo
            "kesehatan" -> CoralRed
            "pendidikan" -> NeonPurple
            "tempat tinggal" -> ElectricIndigo
            "asuransi" -> CyberTeal
            "hobi & olahraga" -> NeonOrange
            "donasi & hadiah" -> NeonPink
            "tabungan target" -> NeonPink
            else -> CoralRed
        }
    }
    return Pair(emoji, color)
}
