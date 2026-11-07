package com.dhandy.smartbudget.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dhandy.smartbudget.data.local.model.TransactionWithDetails
import com.dhandy.smartbudget.ui.theme.*
import com.dhandy.smartbudget.ui.viewmodel.TransactionViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

data class WeeklyBarData(
    val dayLabel: String,
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

    // 0 = Income, 1 = Expense (Reference tab has Income / Expense toggle)
    var selectedType by remember { mutableStateOf(1) } // Default to Expense as per screenshot
    val targetType = if (selectedType == 0) "INCOME" else "EXPENSE"

    // Filter by type
    val typeFilteredTransactions = remember(transactions, targetType) {
        transactions.filter { it.category.type == targetType }
    }

    val totalBalance = remember(wallets) {
        wallets.sumOf { it.currentBalance }
    }

    // Top Spending / Income Categories (Top 3 as reference "Top 3 Spending Areas")
    val topCategories = remember(typeFilteredTransactions) {
        typeFilteredTransactions
            .groupBy { it.category.name }
            .mapValues { it.value.sumOf { tx -> tx.transaction.amount } }
            .toList()
            .sortedByDescending { it.second }
            .take(3)
    }

    // Weekly Bar Chart Data (Mon - Sun of current week)
    val weeklyData = remember(transactions, targetType) {
        val cal = Calendar.getInstance().apply {
            firstDayOfWeek = Calendar.MONDAY
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val daysLabels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        val result = mutableListOf<WeeklyBarData>()

        for (i in 0..6) {
            val dayStart = cal.timeInMillis + (i * 24 * 60 * 60 * 1000L)
            val dayEnd = dayStart + (24 * 60 * 60 * 1000L) - 1L

            val daySum = transactions
                .filter { it.category.type == targetType && it.transaction.date in dayStart..dayEnd }
                .sumOf { it.transaction.amount }

            result.add(WeeklyBarData(dayLabel = daysLabels[i], amount = daySum))
        }
        result
    }

    val totalWeeklyExpense = remember(weeklyData) {
        weeklyData.sumOf { it.amount }
    }

    val maxDayIndex = remember(weeklyData) {
        val maxAmount = weeklyData.maxOfOrNull { it.amount } ?: 0.0
        if (maxAmount > 0.0) weeklyData.indexOfFirst { it.amount == maxAmount } else 4 // Default Fri
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 90.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // 1. Header (Analytics title + Bell icon)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .size(44.dp)
                            .background(MaterialTheme.colorScheme.surface, CircleShape)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), CircleShape)
                    ) {
                        Icon(
                            Icons.Filled.ArrowBack,
                            contentDescription = "Kembali",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = "Analytics",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(MaterialTheme.colorScheme.surface, CircleShape)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.Notifications,
                        contentDescription = "Notifikasi",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // 2. Segmented Pill Tab: Income vs Expense
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                    .padding(4.dp)
            ) {
                Row(modifier = Modifier.fillMaxSize()) {
                    val isIncome = selectedType == 0
                    val isExpense = selectedType == 1

                    // Income Tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isIncome) AccentOrange else Color.Transparent)
                            .clickable { selectedType = 0 },
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
                            .clickable { selectedType = 1 },
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

            // 3. Total Balance Display (Centered)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Total Balance",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                val nf = NumberFormat.getNumberInstance(Locale("id", "ID"))
                Text(
                    text = "Rp " + nf.format(totalBalance),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            // 4. Top 3 Spending / Income Areas
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = if (selectedType == 1) "Top 3 Spending Areas" else "Top 3 Income Sources",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(10.dp))

                if (topCategories.isEmpty()) {
                    Text(
                        text = "Belum ada transaksi di periode ini.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        topCategories.forEachIndexed { index, (catName, _) ->
                            val isFirst = index == 0
                            Surface(
                                color = if (isFirst) AccentOrange else MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(14.dp),
                                border = if (isFirst) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = catName,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isFirst) Color.White else MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 5. Weekly Bar Chart Card (Rounded Card with Week dropdown & Rounded Pill Bars)
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header inside chart: Total expenses + "Week v" pill
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (selectedType == 1) "Total expenses" else "Total income",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
                            val nf = NumberFormat.getNumberInstance(Locale("id", "ID"))
                            Text(
                                text = "Rp " + nf.format(totalWeeklyExpense),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Week pill
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "Week ▾",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }

                    // Bar Chart Canvas Component
                    WeeklyBarChart(
                        data = weeklyData,
                        highlightIndex = maxDayIndex,
                        accentColor = AccentOrange,
                        barColor = WarmYellowCard,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                    )
                }
            }

            // 6. Recent Breakdown List
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Aktivitas Terbaru",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                if (typeFilteredTransactions.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Tidak ada catatan dalam kategori ini",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp
                        )
                    }
                } else {
                    typeFilteredTransactions.take(5).forEach { item ->
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(WarmYellowLight, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        val (emoji, _) = getCategoryEmojiAndColor(item.category.name, item.category.type == "INCOME")
                                        Text(text = emoji, fontSize = 18.sp)
                                    }
                                    Column {
                                        Text(
                                            text = item.category.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        val dateSdf = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID"))
                                        Text(
                                            text = dateSdf.format(Date(item.transaction.date)),
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                val nf = NumberFormat.getNumberInstance(Locale("id", "ID"))
                                val prefix = if (item.category.type == "INCOME") "+Rp " else "-Rp "
                                Text(
                                    text = prefix + nf.format(item.transaction.amount),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 14.sp,
                                    color = if (item.category.type == "INCOME") Color(0xFF10B981) else AccentOrange
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WeeklyBarChart(
    data: List<WeeklyBarData>,
    highlightIndex: Int,
    accentColor: Color,
    barColor: Color,
    modifier: Modifier = Modifier
) {
    val maxVal = remember(data) {
        val highest = data.maxOfOrNull { it.amount } ?: 1.0
        if (highest <= 0.0) 1.0 else highest
    }

    val animatedProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(800)
    )

    Column(modifier = modifier) {
        // Bar chart canvas
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            val count = data.size
            if (count == 0) return@Canvas

            val totalWidth = size.width
            val totalHeight = size.height
            val slotWidth = totalWidth / count
            val barWidth = 26.dp.toPx()

            data.forEachIndexed { i, barData ->
                val centerX = slotWidth * i + (slotWidth / 2f)
                val left = centerX - (barWidth / 2f)

                val ratio = (barData.amount / maxVal).toFloat() * animatedProgress
                // minimum bar height for pleasant visuals
                val minBarHeight = 16.dp.toPx()
                val calculatedHeight = totalHeight * 0.72f * ratio
                val actualHeight = if (barData.amount > 0.0) calculatedHeight.coerceAtLeast(minBarHeight) else minBarHeight

                val top = totalHeight - actualHeight
                val isHighlighted = i == highlightIndex && barData.amount > 0.0

                // Draw rounded pill bar
                drawRoundRect(
                    color = if (isHighlighted) accentColor else barColor,
                    topLeft = Offset(left, top),
                    size = Size(barWidth, actualHeight),
                    cornerRadius = CornerRadius(barWidth / 2, barWidth / 2)
                )

                // Draw top dot
                val dotRadius = 3.dp.toPx()
                drawCircle(
                    color = if (isHighlighted) Color.White else DarkNavBrown.copy(alpha = 0.6f),
                    radius = dotRadius,
                    center = Offset(centerX, top + (barWidth / 2))
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Day labels row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            data.forEachIndexed { i, barData ->
                val isHighlighted = i == highlightIndex && barData.amount > 0.0
                Text(
                    text = barData.dayLabel,
                    fontSize = 11.sp,
                    fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Medium,
                    color = if (isHighlighted) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
