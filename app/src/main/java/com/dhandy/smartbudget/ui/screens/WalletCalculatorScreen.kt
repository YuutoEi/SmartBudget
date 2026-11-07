package com.dhandy.smartbudget.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dhandy.smartbudget.data.local.entity.WalletEntity
import com.dhandy.smartbudget.ui.viewmodel.TransactionViewModel
import com.dhandy.smartbudget.ui.theme.ElectricIndigo
import com.dhandy.smartbudget.ui.theme.NeonPink
import com.dhandy.smartbudget.ui.theme.getGradientForName
import java.text.NumberFormat
import java.util.Locale
import java.util.Stack

sealed class Token {
    data class Number(val value: String) : Token()
    data class Wallet(val wallet: WalletEntity) : Token()
    data class Operator(val op: Char) : Token()
    data class Parenthesis(val isOpen: Boolean) : Token()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletCalculatorScreen(
    viewModel: TransactionViewModel,
    onNavigateBack: () -> Unit
) {
    val wallets by viewModel.wallets.collectAsState()
    val tokens = remember { mutableStateListOf<Token>() }
    var walletDropdownExpanded by remember { mutableStateOf(false) }
    
    // Evaluasi hasil secara real-time
    val evaluatedResult = remember(tokens.size, if (tokens.isNotEmpty()) tokens.last() else null) {
        evaluateTokens(tokens)
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
                    onClick = {} // Mencegah klik di dalam sheet menutup kalkulator
                ),
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            color = MaterialTheme.colorScheme.background,
            shadowElevation = 16.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
            ) {
                // Drag Handle Bar minimalis di bagian atas
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 4.dp),
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

                // Header Sheet
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Kalkulator Dompet",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .size(38.dp)
                            .background(MaterialTheme.colorScheme.surface, CircleShape)
                    ) {
                        Icon(
                            Icons.Outlined.Close,
                            contentDescription = "Tutup",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // 1. Area Tampilan Rumus & Hasil (Layar Kalkulator)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .weight(1.3f),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    )
                ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Tampilan Formula (Token-based)
                    val formulaScrollState = rememberScrollState()
                    LaunchedEffect(tokens.size) {
                        // Scroll otomatis ke akhir formula saat token bertambah
                        formulaScrollState.animateScrollTo(formulaScrollState.maxValue)
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(formulaScrollState)
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End
                    ) {
                        if (tokens.isEmpty()) {
                            Text(
                                text = "Mulai berhitung...",
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Medium
                            )
                        } else {
                            tokens.forEach { token ->
                                when (token) {
                                    is Token.Wallet -> {
                                        WalletTokenChip(wallet = token.wallet)
                                    }
                                    is Token.Number -> {
                                        Text(
                                            text = formatPreviewNumber(token.value),
                                            fontSize = 22.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.padding(horizontal = 4.dp)
                                        )
                                    }
                                    is Token.Operator -> {
                                        Text(
                                            text = " ${token.op} ",
                                            fontSize = 22.sp,
                                            fontWeight = FontWeight.Black,
                                            color = ElectricIndigo,
                                            modifier = Modifier.padding(horizontal = 4.dp)
                                        )
                                    }
                                    is Token.Parenthesis -> {
                                        Text(
                                            text = if (token.isOpen) "(" else ")",
                                            fontSize = 22.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = NeonPink,
                                            modifier = Modifier.padding(horizontal = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Tampilan Nilai Riil di bawahnya (Preview Nilai)
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.End
                    ) {
                        if (tokens.isNotEmpty()) {
                            Text(
                                text = buildValuePreview(tokens),
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                textAlign = TextAlign.End,
                                maxLines = 2,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }

                        // Hasil Evaluasi Akhir
                        Text(
                            text = when {
                                tokens.isEmpty() -> "Rp 0"
                                evaluatedResult != null -> formatCurrency(evaluatedResult)
                                else -> "Formula tidak valid"
                            },
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            color = if (evaluatedResult != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.End
                        )
                    }
                }
            }

            // 2. Daftar Dompet (1 lebar dari kanan ke kiri dengan Dropdown)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                ExposedDropdownMenuBox(
                    expanded = walletDropdownExpanded,
                    onExpandedChange = { walletDropdownExpanded = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(16.dp),
                        shadowElevation = 2.dp,
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                            .clickable { walletDropdownExpanded = !walletDropdownExpanded }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "💰",
                                    fontSize = 18.sp
                                )
                                Column {
                                    Text(
                                        text = "Pilih Saldo Dompet",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Sentuh untuk masukkan saldo dompet",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    )
                                }
                            }

                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = walletDropdownExpanded)
                        }
                    }

                    ExposedDropdownMenu(
                        expanded = walletDropdownExpanded,
                        onDismissRequest = { walletDropdownExpanded = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (wallets.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("Belum ada dompet", fontSize = 13.sp) },
                                onClick = { walletDropdownExpanded = false }
                            )
                        } else {
                            wallets.forEach { wallet ->
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = wallet.name,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            )
                                            Spacer(modifier = Modifier.width(16.dp))
                                            Text(
                                                text = formatCurrency(wallet.currentBalance),
                                                fontWeight = FontWeight.ExtraBold,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontSize = 14.sp
                                            )
                                        }
                                    },
                                    onClick = {
                                        tokens.add(Token.Wallet(wallet))
                                        walletDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 3. Keyboard Kalkulator
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(3f)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
                    )
                    .padding(20.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val rowModifier = Modifier.weight(1f).fillMaxWidth()
                    
                    // Baris 1: C, (, ), ⌫
                    Row(modifier = rowModifier, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        CalcButton(text = "C", color = CoralButtonColor(), modifier = Modifier.weight(1f)) {
                            tokens.clear()
                        }
                        CalcButton(text = "(", color = ActionButtonColor(), modifier = Modifier.weight(1f)) {
                            tokens.add(Token.Parenthesis(isOpen = true))
                        }
                        CalcButton(text = ")", color = ActionButtonColor(), modifier = Modifier.weight(1f)) {
                            tokens.add(Token.Parenthesis(isOpen = false))
                        }
                        CalcButton(text = "⌫", color = ActionButtonColor(), modifier = Modifier.weight(1f)) {
                            if (tokens.isNotEmpty()) {
                                val lastToken = tokens.last()
                                if (lastToken is Token.Number && lastToken.value.length > 1) {
                                    tokens[tokens.lastIndex] = Token.Number(lastToken.value.dropLast(1))
                                } else {
                                    tokens.removeAt(tokens.lastIndex)
                                }
                            }
                        }
                    }

                    // Baris 2: 7, 8, 9, ÷
                    Row(modifier = rowModifier, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        CalcButton(text = "7", modifier = Modifier.weight(1f)) { appendNumber("7", tokens) }
                        CalcButton(text = "8", modifier = Modifier.weight(1f)) { appendNumber("8", tokens) }
                        CalcButton(text = "9", modifier = Modifier.weight(1f)) { appendNumber("9", tokens) }
                        CalcButton(text = "÷", color = OperatorButtonColor(), modifier = Modifier.weight(1f)) {
                            appendOperator('/', tokens)
                        }
                    }

                    // Baris 3: 4, 5, 6, ×
                    Row(modifier = rowModifier, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        CalcButton(text = "4", modifier = Modifier.weight(1f)) { appendNumber("4", tokens) }
                        CalcButton(text = "5", modifier = Modifier.weight(1f)) { appendNumber("5", tokens) }
                        CalcButton(text = "6", modifier = Modifier.weight(1f)) { appendNumber("6", tokens) }
                        CalcButton(text = "×", color = OperatorButtonColor(), modifier = Modifier.weight(1f)) {
                            appendOperator('*', tokens)
                        }
                    }

                    // Baris 4: 1, 2, 3, -
                    Row(modifier = rowModifier, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        CalcButton(text = "1", modifier = Modifier.weight(1f)) { appendNumber("1", tokens) }
                        CalcButton(text = "2", modifier = Modifier.weight(1f)) { appendNumber("2", tokens) }
                        CalcButton(text = "3", modifier = Modifier.weight(1f)) { appendNumber("3", tokens) }
                        CalcButton(text = "-", color = OperatorButtonColor(), modifier = Modifier.weight(1f)) {
                            appendOperator('-', tokens)
                        }
                    }

                    // Baris 5: 0, ., =, +
                    Row(modifier = rowModifier, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        CalcButton(text = "0", modifier = Modifier.weight(1.5f)) { appendNumber("0", tokens) }
                        CalcButton(text = ".", modifier = Modifier.weight(1f)) { appendDecimal(tokens) }
                        CalcButton(text = "+", color = OperatorButtonColor(), modifier = Modifier.weight(1f)) {
                            appendOperator('+', tokens)
                        }
                        // Tombol sama dengan (=) mengevaluasi hasil dan mengubah formula menjadi satu token angka hasil
                        CalcButton(
                            text = "=", 
                            color = EqualButtonColor(), 
                            modifier = Modifier.weight(1.5f)
                        ) {
                            if (evaluatedResult != null) {
                                tokens.clear()
                                val formattedVal = if (evaluatedResult % 1 == 0.0) {
                                    evaluatedResult.toLong().toString()
                                } else {
                                    evaluatedResult.toString()
                                }
                                tokens.add(Token.Number(formattedVal))
                            }
                        }
                    }
                }
            }
        }
    }
}
}

// Menambahkan angka ke formula
private fun appendNumber(digit: String, tokens: MutableList<Token>) {
    if (tokens.isNotEmpty()) {
        val last = tokens.last()
        if (last is Token.Number) {
            if (last.value == "0") {
                tokens[tokens.lastIndex] = Token.Number(digit)
            } else {
                tokens[tokens.lastIndex] = Token.Number(last.value + digit)
            }
        } else {
            tokens.add(Token.Number(digit))
        }
    } else {
        tokens.add(Token.Number(digit))
    }
}

// Menambahkan desimal
private fun appendDecimal(tokens: MutableList<Token>) {
    if (tokens.isNotEmpty()) {
        val last = tokens.last()
        if (last is Token.Number) {
            if (!last.value.contains(".")) {
                tokens[tokens.lastIndex] = Token.Number(last.value + ".")
            }
        } else {
            tokens.add(Token.Number("0."))
        }
    } else {
        tokens.add(Token.Number("0."))
    }
}

// Menambahkan operator dengan cerdas (jika operator ganda, akan menimpa operator sebelumnya)
private fun appendOperator(op: Char, tokens: MutableList<Token>) {
    if (tokens.isNotEmpty()) {
        val last = tokens.last()
        if (last is Token.Operator) {
            tokens[tokens.lastIndex] = Token.Operator(op)
        } else {
            tokens.add(Token.Operator(op))
        }
    } else if (op == '-') {
        // Izinkan tanda minus di awal
        tokens.add(Token.Operator(op))
    }
}

// Membuat visualisasi nilai riil
private fun buildValuePreview(tokens: List<Token>): String {
    val sb = java.lang.StringBuilder()
    tokens.forEach { token ->
        when (token) {
            is Token.Wallet -> {
                sb.append("(${formatCurrency(token.wallet.currentBalance)})")
            }
            is Token.Number -> {
                sb.append(formatPreviewNumber(token.value))
            }
            is Token.Operator -> {
                sb.append(" ${token.op} ")
            }
            is Token.Parenthesis -> {
                sb.append(if (token.isOpen) "(" else ")")
            }
        }
    }
    return sb.toString()
}

// Chip mini bergaya untuk token dompet di dalam formula
@Composable
fun WalletTokenChip(wallet: WalletEntity) {
    val gradientColors = getGradientForName(wallet.name)
    Box(
        modifier = Modifier
            .padding(horizontal = 4.dp)
            .background(
                brush = Brush.linearGradient(gradientColors),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = wallet.name,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

// Kartu mini dompet yang bisa diklik untuk memasukkan saldo
@Composable
fun WalletButtonCard(
    wallet: WalletEntity,
    onClick: () -> Unit
) {
    val gradientColors = getGradientForName(wallet.name)
    Card(
        modifier = Modifier
            .width(130.dp)
            .height(55.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(brush = Brush.linearGradient(gradientColors))
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = wallet.name,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    maxLines = 1
                )
                Text(
                    text = formatCurrency(wallet.currentBalance),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.9f),
                    maxLines = 1
                )
            }
        }
    }
}

// Tombol Keyboard Kalkulator
@Composable
fun CalcButton(
    text: String,
    modifier: Modifier = Modifier,
    color: ButtonColors? = null,
    onClick: () -> Unit
) {
    val targetColor = color ?: ButtonDefaults.filledTonalButtonColors(
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurface
    )
    
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxHeight(),
        shape = RoundedCornerShape(16.dp),
        colors = targetColor,
        contentPadding = PaddingValues(0.dp)
    ) {
        Text(
            text = text,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

// Warna Khusus Tombol Keyboard
@Composable
fun ActionButtonColor(): ButtonColors {
    return ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.8f),
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
    )
}

@Composable
fun OperatorButtonColor(): ButtonColors {
    return ButtonDefaults.buttonColors(
        containerColor = ElectricIndigo,
        contentColor = Color.White
    )
}

@Composable
fun EqualButtonColor(): ButtonColors {
    return ButtonDefaults.buttonColors(
        containerColor = NeonPink,
        contentColor = Color.White
    )
}

@Composable
fun CoralButtonColor(): ButtonColors {
    return ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer
    )
}

// Format Angka Ribuan
fun formatPreviewNumber(numStr: String): String {
    val doubleVal = numStr.toDoubleOrNull() ?: return numStr
    val format = NumberFormat.getNumberInstance(Locale("id", "ID"))
    if (numStr.contains(".")) {
        val parts = numStr.split(".")
        val intPart = parts[0].toLongOrNull()
        if (intPart != null) {
            return format.format(intPart) + "," + parts[1]
        }
    }
    val longVal = numStr.toLongOrNull()
    if (longVal != null) {
        return format.format(longVal)
    }
    return format.format(doubleVal)
}

// ============================================
// Evaluator Matematika Menggunakan Shunting-Yard
// ============================================
fun evaluateTokens(tokens: List<Token>): Double? {
    if (tokens.isEmpty()) return 0.0
    
    val symbols = mutableListOf<String>()
    var expectOperand = true
    for (token in tokens) {
        when (token) {
            is Token.Number -> {
                symbols.add(token.value)
                expectOperand = false
            }
            is Token.Wallet -> {
                symbols.add(token.wallet.currentBalance.toString())
                expectOperand = false
            }
            is Token.Operator -> {
                if (expectOperand && token.op == '-') {
                    symbols.add("0") // Representasikan sebagai 0 - X
                }
                symbols.add(token.op.toString())
                expectOperand = true
            }
            is Token.Parenthesis -> {
                symbols.add(if (token.isOpen) "(" else ")")
                expectOperand = token.isOpen
            }
        }
    }
    
    val output = mutableListOf<String>()
    val operators = Stack<String>()
    val precedence = mapOf("+" to 1, "-" to 1, "*" to 2, "/" to 2)
    
    try {
        for (symbol in symbols) {
            when {
                symbol.toDoubleOrNull() != null -> {
                    output.add(symbol)
                }
                symbol == "(" -> {
                    operators.push(symbol)
                }
                symbol == ")" -> {
                    while (operators.isNotEmpty() && operators.peek() != "(") {
                        output.add(operators.pop())
                    }
                    if (operators.isNotEmpty() && operators.peek() == "(") {
                        operators.pop()
                    } else {
                        return null
                    }
                }
                precedence.containsKey(symbol) -> {
                    while (operators.isNotEmpty() && precedence.containsKey(operators.peek()) &&
                        (precedence[operators.peek()] ?: 0) >= (precedence[symbol] ?: 0)) {
                        output.add(operators.pop())
                    }
                    operators.push(symbol)
                }
                else -> {
                    val cleaned = if (symbol.endsWith(".")) symbol + "0" else symbol
                    if (cleaned.toDoubleOrNull() != null) {
                        output.add(cleaned)
                    } else {
                        return null
                    }
                }
            }
        }
        
        while (operators.isNotEmpty()) {
            val op = operators.pop()
            if (op == "(" || op == ")") return null
            output.add(op)
        }
        
        val stack = Stack<Double>()
        for (token in output) {
            val number = token.toDoubleOrNull()
            if (number != null) {
                stack.push(number)
            } else {
                if (stack.size < 2) return null
                val b = stack.pop()
                val a = stack.pop()
                val result = when (token) {
                    "+" -> a + b
                    "-" -> a - b
                    "*" -> a * b
                    "/" -> {
                        if (b == 0.0) return null
                        a / b
                    }
                    else -> return null
                }
                stack.push(result)
            }
        }
        
        return if (stack.size == 1) stack.pop() else null
    } catch (e: Exception) {
        return null
    }
}
