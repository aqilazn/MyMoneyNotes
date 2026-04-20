package com.example.etsppb

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.etsppb.ui.theme.ETSPPBTheme
import java.text.NumberFormat
import java.util.*

// ─────────────────────────────────────────────
// DATA MODEL
// ─────────────────────────────────────────────

enum class TransactionType { INCOME, EXPENSE }

data class Transaction(
    val id: Int,
    val title: String,
    val amount: Double,
    val category: String,
    val type: TransactionType,
    val date: String,
    val note: String = ""
)

// ─────────────────────────────────────────────
// DUMMY DATA
// ─────────────────────────────────────────────

val dummyTransactions = mutableListOf(
    Transaction(1, "Gaji Bulanan", 5000000.0, "Gaji", TransactionType.INCOME, "01 Apr 2025"),
    Transaction(2, "Freelance Design", 1500000.0, "Freelance", TransactionType.INCOME, "05 Apr 2025"),
    Transaction(3, "Makan Siang", 45000.0, "Makanan", TransactionType.EXPENSE, "06 Apr 2025"),
    Transaction(4, "Token Listrik", 150000.0, "Tagihan", TransactionType.EXPENSE, "07 Apr 2025"),
    Transaction(5, "Belanja Groceries", 320000.0, "Belanja", TransactionType.EXPENSE, "08 Apr 2025"),
    Transaction(6, "Transfer dari Ortu", 800000.0, "Transfer", TransactionType.INCOME, "10 Apr 2025"),
    Transaction(7, "Bensin Motor", 80000.0, "Transportasi", TransactionType.EXPENSE, "11 Apr 2025"),
    Transaction(8, "Netflix", 54000.0, "Hiburan", TransactionType.EXPENSE, "12 Apr 2025"),
    Transaction(9, "Internet", 190000.0, "Tagihan", TransactionType.EXPENSE, "13 Apr 2025"),
    Transaction(10, "Bonus Proyek", 2000000.0, "Bonus", TransactionType.INCOME, "14 Apr 2025"),
)

val incomeCategories = listOf("Gaji", "Freelance", "Bonus", "Transfer", "Investasi", "Lainnya")
val expenseCategories = listOf("Makanan", "Transportasi", "Belanja", "Tagihan", "Hiburan", "Kesehatan", "Pendidikan", "Lainnya")

// ─────────────────────────────────────────────
// COLORS
// ─────────────────────────────────────────────

val GreenIncome = Color(0xFF2E7D32)
val GreenLight = Color(0xFF4CAF50)
val RedExpense = Color(0xFFC62828)
val RedLight = Color(0xFFEF5350)
val BackgroundColor = Color(0xFFF5F7FA)
val CardColor = Color.White
val PrimaryColor = Color(0xFF1565C0)
val TextPrimary = Color(0xFF1A1A2E)
val TextSecondary = Color(0xFF6B7280)

// ─────────────────────────────────────────────
// MAIN ACTIVITY
// ─────────────────────────────────────────────

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ETSPPBTheme {
                MyMoneyApp()
            }
        }
    }
}

// ─────────────────────────────────────────────
// MAIN APP
// ─────────────────────────────────────────────

@Composable
fun MyMoneyApp() {
    var transactions by remember { mutableStateOf(dummyTransactions.toList()) }
    var selectedTab by remember { mutableIntStateOf(0) }
    var showAddDialog by remember { mutableStateOf(false) }
    var filterType by remember { mutableStateOf<TransactionType?>(null) }

    val totalIncome = transactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
    val totalExpense = transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
    val balance = totalIncome - totalExpense

    val filteredTransactions = when (filterType) {
        TransactionType.INCOME -> transactions.filter { it.type == TransactionType.INCOME }
        TransactionType.EXPENSE -> transactions.filter { it.type == TransactionType.EXPENSE }
        null -> transactions
    }

    Scaffold(
        containerColor = BackgroundColor,
        floatingActionButton = {
            if (selectedTab == 0) {
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = PrimaryColor,
                    contentColor = Color.White,
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Tambah Transaksi")
                }
            }
        },
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text("Beranda") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Transaksi") },
                    label = { Text("Transaksi") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.BarChart, contentDescription = "Statistik") },
                    label = { Text("Statistik") }
                )
            }
        }
    ) { paddingValues ->
        when (selectedTab) {
            0 -> HomeScreen(
                modifier = Modifier.padding(paddingValues),
                transactions = filteredTransactions,
                totalIncome = totalIncome,
                totalExpense = totalExpense,
                balance = balance,
                filterType = filterType,
                onFilterChange = { filterType = it },
                onDelete = { id -> transactions = transactions.filter { it.id != id } }
            )
            1 -> TransactionListScreen(
                modifier = Modifier.padding(paddingValues),
                transactions = transactions,
                onDelete = { id -> transactions = transactions.filter { it.id != id } }
            )
            2 -> StatisticsScreen(
                modifier = Modifier.padding(paddingValues),
                transactions = transactions,
                totalIncome = totalIncome,
                totalExpense = totalExpense
            )
        }
    }

    // FIX: onDismiss sekarang benar-benar menutup dialog
    if (showAddDialog) {
        AddTransactionDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { newTransaction ->
                val newId = (transactions.maxOfOrNull { it.id } ?: 0) + 1
                transactions = transactions + newTransaction.copy(id = newId)
                showAddDialog = false
            }
        )
    }
}

// ─────────────────────────────────────────────
// HOME SCREEN
// ─────────────────────────────────────────────

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    transactions: List<Transaction>,
    totalIncome: Double,
    totalExpense: Double,
    balance: Double,
    filterType: TransactionType?,
    onFilterChange: (TransactionType?) -> Unit,
    onDelete: (Int) -> Unit
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                "MyMoney Notes",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
        item {
            BalanceCard(balance = balance, totalIncome = totalIncome, totalExpense = totalExpense)
        }
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                FilterChip(
                    selected = filterType == null,
                    onClick = { onFilterChange(null) },
                    label = { Text("Semua") }
                )
                FilterChip(
                    selected = filterType == TransactionType.INCOME,
                    onClick = { onFilterChange(TransactionType.INCOME) },
                    label = { Text("Pemasukan") }
                )
                FilterChip(
                    selected = filterType == TransactionType.EXPENSE,
                    onClick = { onFilterChange(TransactionType.EXPENSE) },
                    label = { Text("Pengeluaran") }
                )
            }
        }
        item {
            Text(
                "Transaksi Terbaru",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
        }
        items(transactions.takeLast(5).reversed()) { transaction ->
            TransactionCard(transaction = transaction, onDelete = onDelete)
        }
    }
}

// ─────────────────────────────────────────────
// BALANCE CARD
// ─────────────────────────────────────────────

@Composable
fun BalanceCard(balance: Double, totalIncome: Double, totalExpense: Double) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = PrimaryColor),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Saldo Saat Ini", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                formatCurrency(balance),
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                BalanceStat(label = "Pemasukan", amount = totalIncome, color = GreenLight)
                HorizontalDivider(
                    modifier = Modifier
                        .height(40.dp)
                        .width(1.dp),
                    color = Color.White.copy(alpha = 0.3f)
                )
                BalanceStat(label = "Pengeluaran", amount = totalExpense, color = RedLight)
            }
        }
    }
}

@Composable
fun BalanceStat(label: String, amount: Double, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
        Text(
            formatCurrency(amount),
            color = color,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

// ─────────────────────────────────────────────
// TRANSACTION CARD
// ─────────────────────────────────────────────

@Composable
fun TransactionCard(transaction: Transaction, onDelete: (Int) -> Unit) {
    val isIncome = transaction.type == TransactionType.INCOME
    val amountColor = if (isIncome) GreenIncome else RedExpense
    val prefix = if (isIncome) "+" else "-"
    val iconBg = if (isIncome) GreenIncome else RedExpense

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardColor),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isIncome) Icons.AutoMirrored.Filled.TrendingUp
                    else Icons.AutoMirrored.Filled.TrendingDown,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    transaction.title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = TextPrimary
                )
                Text(
                    "${transaction.category} · ${transaction.date}",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "$prefix${formatCurrency(transaction.amount)}",
                    color = amountColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Hapus",
                    tint = TextSecondary,
                    modifier = Modifier
                        .size(18.dp)
                        .clickable { onDelete(transaction.id) }
                )
            }
        }
    }
}

// ─────────────────────────────────────────────
// TRANSACTION LIST SCREEN
// ─────────────────────────────────────────────

@Composable
fun TransactionListScreen(
    modifier: Modifier = Modifier,
    transactions: List<Transaction>,
    onDelete: (Int) -> Unit
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                "Semua Transaksi",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
        items(transactions.reversed()) { transaction ->
            TransactionCard(transaction = transaction, onDelete = onDelete)
        }
    }
}

// ─────────────────────────────────────────────
// STATISTICS SCREEN
// ─────────────────────────────────────────────

@Composable
fun StatisticsScreen(
    modifier: Modifier = Modifier,
    transactions: List<Transaction>,
    totalIncome: Double,
    totalExpense: Double
) {
    val expenseByCategory = transactions
        .filter { it.type == TransactionType.EXPENSE }
        .groupBy { it.category }
        .mapValues { entry -> entry.value.sumOf { it.amount } }
        .toList()
        .sortedByDescending { it.second }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                "Statistik",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SummaryCard(
                    modifier = Modifier.weight(1f),
                    label = "Total Pemasukan",
                    amount = totalIncome,
                    color = GreenIncome
                )
                SummaryCard(
                    modifier = Modifier.weight(1f),
                    label = "Total Pengeluaran",
                    amount = totalExpense,
                    color = RedExpense
                )
            }
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardColor),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Pengeluaran per Kategori",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    expenseByCategory.forEach { (category, amount) ->
                        CategoryBar(category = category, amount = amount, total = totalExpense)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
        item {
            Text(
                "Jumlah Transaksi: ${transactions.size}",
                fontSize = 14.sp,
                color = TextSecondary,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }
    }
}

@Composable
fun SummaryCard(modifier: Modifier = Modifier, label: String, amount: Double, color: Color) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = CardColor),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(label, fontSize = 12.sp, color = TextSecondary)
            Spacer(modifier = Modifier.height(4.dp))
            Text(formatCurrency(amount), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
fun CategoryBar(category: String, amount: Double, total: Double) {
    val percentage = if (total > 0) (amount / total).toFloat() else 0f
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(category, fontSize = 13.sp, color = TextPrimary)
            Text(formatCurrency(amount), fontSize = 13.sp, color = RedExpense, fontWeight = FontWeight.SemiBold)
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { percentage },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = RedExpense,
            trackColor = Color(0xFFFFCDD2)
        )
        Text("${(percentage * 100).toInt()}%", fontSize = 11.sp, color = TextSecondary)
    }
}

// ─────────────────────────────────────────────
// ADD TRANSACTION DIALOG
// ─────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionDialog(onDismiss: () -> Unit, onAdd: (Transaction) -> Unit) {
    var title by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(TransactionType.EXPENSE) }
    var selectedCategory by remember { mutableStateOf("") }
    var expandedCategory by remember { mutableStateOf(false) }

    val categories = if (selectedType == TransactionType.INCOME) incomeCategories else expenseCategories

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(8.dp),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .heightIn(max = 580.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    "Tambah Transaksi",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(BackgroundColor),
                ) {
                    TypeButton(
                        label = "Pengeluaran",
                        selected = selectedType == TransactionType.EXPENSE,
                        color = RedExpense,
                        modifier = Modifier.weight(1f)
                    ) {
                        selectedType = TransactionType.EXPENSE
                        selectedCategory = ""
                    }
                    TypeButton(
                        label = "Pemasukan",
                        selected = selectedType == TransactionType.INCOME,
                        color = GreenIncome,
                        modifier = Modifier.weight(1f)
                    ) {
                        selectedType = TransactionType.INCOME
                        selectedCategory = ""
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Judul Transaksi") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it.filter { c -> c.isDigit() } },
                    label = { Text("Nominal (Rp)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))

                ExposedDropdownMenuBox(
                    expanded = expandedCategory,
                    onExpandedChange = { expandedCategory = it }
                ) {
                    OutlinedTextField(
                        value = selectedCategory,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Kategori") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCategory) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = expandedCategory,
                        onDismissRequest = { expandedCategory = false }
                    ) {
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat) },
                                onClick = {
                                    selectedCategory = cat
                                    expandedCategory = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Catatan (opsional)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("Batal") }

                    Button(
                        onClick = {
                            val amt = amount.toDoubleOrNull() ?: 0.0
                            if (title.isNotBlank() && amt > 0 && selectedCategory.isNotBlank()) {
                                onAdd(
                                    Transaction(
                                        id = 0,
                                        title = title,
                                        amount = amt,
                                        category = selectedCategory,
                                        type = selectedType,
                                        date = "19 Apr 2026",
                                        note = note
                                    )
                                )
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor)
                    ) { Text("Simpan") }
                }
            }
        }
    }
}

@Composable
fun TypeButton(label: String, selected: Boolean, color: Color, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .padding(4.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) color else Color.Transparent)
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = if (selected) Color.White else TextSecondary,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            fontSize = 14.sp
        )
    }
}

// ─────────────────────────────────────────────
// HELPER
// ─────────────────────────────────────────────

fun formatCurrency(amount: Double): String {
    val formatter = NumberFormat.getNumberInstance(Locale("id", "ID"))
    formatter.maximumFractionDigits = 0
    return "Rp ${formatter.format(amount)}"
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    ETSPPBTheme {
        MyMoneyApp()
    }
}