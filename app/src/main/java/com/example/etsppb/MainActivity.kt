@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.example.etsppb

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.room.*
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.etsppb.ui.theme.ETSPPBTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

// ─────────────────────────────────────────────
// DATABASE & LOGIC
// ─────────────────────────────────────────────

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val amount: Double,
    val category: String,
    val type: TransactionType,
    val date: String,
    val note: String = ""
)

enum class TransactionType { INCOME, EXPENSE }

class Converters {
    @TypeConverter
    fun fromTransactionType(value: TransactionType): String = value.name
    @TypeConverter
    fun toTransactionType(value: String): TransactionType = TransactionType.valueOf(value)
}

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions")
    fun getAllTransactions(): Flow<List<Transaction>>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: Transaction)
    @Delete
    suspend fun delete(transaction: Transaction)
}

@Database(entities = [Transaction::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "money_db")
                    .fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}

// ─────────────────────────────────────────────
// DESIGN SYSTEM
// ─────────────────────────────────────────────
val GreenIncome = Color(0xFF2E7D32)
val GreenLight = Color(0xFFE8F5E9)
val RedExpense = Color(0xFFC62828)
val RedLight = Color(0xFFFFEBEE)
val BackgroundColor = Color(0xFFF8FAFC)
val CardColor = Color.White
val PrimaryColor = Color(0xFF1565C0)
val TextPrimary = Color(0xFF1E293B)
val TextSecondary = Color(0xFF64748B)

val ChartColors = listOf(Color(0xFF3B82F6), Color(0xFF10B981), Color(0xFFF59E0B), Color(0xFF8B5CF6), Color(0xFFEF4444), Color(0xFF06B6D4))

val incomeCategories = listOf("Gaji", "Freelance", "Bonus", "Transfer", "Investasi", "Lainnya")
val expenseCategories = listOf("Makanan", "Transportasi", "Belanja", "Tagihan", "Hiburan", "Kesehatan", "Pendidikan", "Lainnya")

// ─────────────────────────────────────────────
// MAIN UI
// ─────────────────────────────────────────────

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val db = AppDatabase.getDatabase(this)
        val dao = db.transactionDao()
        setContent { ETSPPBTheme { MyMoneyApp(dao) } }
    }
}

@Composable
fun MyMoneyApp(dao: TransactionDao) {
    val coroutineScope = rememberCoroutineScope()
    val transactions by dao.getAllTransactions().collectAsState(initial = emptyList())
    var selectedTab by remember { mutableIntStateOf(0) }
    var showAddDialog by remember { mutableStateOf(false) }
    var transactionToEdit by remember { mutableStateOf<Transaction?>(null) }

    Scaffold(
        containerColor = BackgroundColor,
        floatingActionButton = {
            if (selectedTab == 0) {
                FloatingActionButton(onClick = { showAddDialog = true }, containerColor = PrimaryColor, contentColor = Color.White, shape = CircleShape)
                { Icon(Icons.Default.Add, "Tambah") }
            }
        },
        bottomBar = {
            NavigationBar(containerColor = Color.White, tonalElevation = 8.dp) {
                NavigationBarItem(selected = selectedTab == 0, onClick = { selectedTab = 0 }, icon = { Icon(Icons.Default.Home, null) }, label = { Text("Beranda") })
                NavigationBarItem(selected = selectedTab == 2, onClick = { selectedTab = 2 }, icon = { Icon(Icons.Default.PieChart, null) }, label = { Text("Statistik") })
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when (selectedTab) {
                0 -> HomeScreen(
                    transactions = transactions,
                    onAdd = { showAddDialog = true },
                    onEdit = { transactionToEdit = it }
                )
                1 -> TransactionListScreen(
                    modifier = Modifier.padding(paddingValues),
                    transactions = transactions,
                    onBack = { selectedTab = 0 },
                    onEdit = { transactionToEdit = it }
                )
                2 -> StatisticsScreen(transactions)
            }
        }
    }

    if (showAddDialog) {
        AddTransactionDialog(onDismiss = { showAddDialog = false }, onAdd = { coroutineScope.launch { dao.insert(it) }; showAddDialog = false })
    }

    // Dialog EDIT
    if (transactionToEdit != null) {
        AddTransactionDialog(
            transaction = transactionToEdit,
            onDismiss = { transactionToEdit = null },
            onAdd = { coroutineScope.launch { dao.insert(it) }; transactionToEdit = null },
            onDelete = { coroutineScope.launch { dao.delete(it) }; transactionToEdit = null }
        )
    }

}

// ─────────────────────────────────────────────
// BERANDA SCREEN
// ─────────────────────────────────────────────

@Composable
fun HomeScreen(transactions: List<Transaction>, onAdd: () -> Unit, onEdit: (Transaction) -> Unit) {
    var filterType by remember { mutableStateOf<TransactionType?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    val totalIncome = transactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
    val totalExpense = transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }

    val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    val sortedList = transactions.sortedByDescending {
        try { sdf.parse(it.date)?.time ?: 0L } catch (e: Exception) { 0L }
    }

    val filteredList = sortedList.filter {
        val matchesFilter = when(filterType) {
            TransactionType.INCOME -> it.type == TransactionType.INCOME
            TransactionType.EXPENSE -> it.type == TransactionType.EXPENSE
            else -> true
        }
        val matchesSearch = it.title.contains(searchQuery, ignoreCase = true) ||
                it.category.contains(searchQuery, ignoreCase = true)

        matchesFilter && matchesSearch
    }

    val groupedTransactions = filteredList.groupBy { it.date }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            ScreenHeader(title = "MyMoney Notes")
        }

        item {
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                BalanceCard(totalIncome - totalExpense, totalIncome, totalExpense)
            }
        }

        stickyHeader {
            Column(modifier = Modifier
                .background(BackgroundColor)
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 0.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Cari transaksi...") },
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = TextSecondary) },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = PrimaryColor,
                        unfocusedBorderColor = Color(0xFFE2E8F0)
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val chipModifier = Modifier.weight(1f)
                    FilterChipModern(selected = filterType == null, label = "Semua", modifier = chipModifier, onClick = { filterType = null })
                    FilterChipModern(selected = filterType == TransactionType.INCOME, label = "Masuk", modifier = chipModifier, onClick = { filterType = TransactionType.INCOME })
                    FilterChipModern(selected = filterType == TransactionType.EXPENSE, label = "Keluar", modifier = chipModifier, onClick = { filterType = TransactionType.EXPENSE })
                }

                Divider(modifier = Modifier.padding(top = 12.dp), color = Color(0xFFF1F5F9))
            }
        }

        groupedTransactions.forEach { (date, transactionsInDate) ->
            item {
                Text(
                    text = date,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            items(transactionsInDate) { item ->
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    TransactionCard(
                        transaction = item,
                        onClick = { onEdit(item) }
                    )
                }
            }
        }

        if(filteredList.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(top = 40.dp), contentAlignment = Alignment.Center) {
                    Text("Tidak ada transaksi ditemukan", color = TextSecondary)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────
// STATISTIK SCREEN
// ─────────────────────────────────────────────

@Composable
fun StatisticsScreen(transactions: List<Transaction>) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val titles = listOf("Pengeluaran", "Pemasukan")

    val filteredData = if (selectedTab == 0) transactions.filter { it.type == TransactionType.EXPENSE }
    else transactions.filter { it.type == TransactionType.INCOME }

    val stats = filteredData.groupBy { it.category }.mapValues { it.value.sumOf { t -> t.amount } }.toList().sortedByDescending { it.second }
    val totalAmount = stats.sumOf { it.second }

    Column(modifier = Modifier.fillMaxSize()) {
        ScreenHeader(title = "Statistik Keuangan")

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent,
            contentColor = PrimaryColor,
            divider = {},
            indicator = { tabPositions -> TabRowDefaults.SecondaryIndicator(Modifier.tabIndicatorOffset(tabPositions[selectedTab]), color = PrimaryColor) }
        ) {
            titles.forEachIndexed { index, title ->
                Tab(selected = selectedTab == index, onClick = { selectedTab = index }, text = { Text(title, fontWeight = if(selectedTab == index) FontWeight.Bold else FontWeight.Normal) })
            }
        }

        LazyColumn(contentPadding = PaddingValues(16.dp)) {
            item {
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = CardColor), shape = RoundedCornerShape(24.dp), elevation = CardDefaults.cardElevation(2.dp)) {
                    Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        if (stats.isNotEmpty()) {
                            Box(contentAlignment = Alignment.Center) {
                                SimplePieChart(stats)
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Total", fontSize = 14.sp, color = TextSecondary)
                                    Text(formatCurrency(totalAmount).replace(" ", "\n"), fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center, lineHeight = 20.sp)
                                }
                            }
                            Spacer(modifier = Modifier.height(32.dp))
                            stats.forEachIndexed { index, item ->
                                CategoryStatRowModern(item.first, item.second, ChartColors[index % ChartColors.size], (if(totalAmount>0) (item.second/totalAmount*100).toInt() else 0))
                            }
                        } else {
                            Text("Belum ada data transaksi", modifier = Modifier.padding(vertical = 40.dp), color = TextSecondary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryStatRowModern(label: String, amount: Double, color: Color, percentage: Int) {
    Column(modifier = Modifier.padding(vertical = 10.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(color))
                Spacer(modifier = Modifier.width(8.dp))
                Text(label, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            Text(formatCurrency(amount), fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
        Spacer(modifier = Modifier.height(6.dp))
        Box(modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape).background(Color(0xFFF1F5F9))) {
            Box(modifier = Modifier.fillMaxWidth(percentage/100f).height(6.dp).clip(CircleShape).background(color))
        }
    }
}

// ─────────────────────────────────────────────
// DIALOG & INPUT
// ─────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionDialog(
    transaction: Transaction? = null,
    onDismiss: () -> Unit,
    onAdd: (Transaction) -> Unit,
    onDelete: ((Transaction) -> Unit)? = null
) {
    var title by remember { mutableStateOf(transaction?.title ?: "") }
    var amount by remember { mutableStateOf(transaction?.amount?.toInt()?.toString() ?: "") }
    var note by remember { mutableStateOf(transaction?.note ?: "") }
    var selectedType by remember { mutableStateOf(transaction?.type ?: TransactionType.EXPENSE) }
    var selectedCategory by remember { mutableStateOf(transaction?.category ?: "") }
    var expanded by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    val calendar = Calendar.getInstance()
    val localTimeWithOffset = calendar.timeInMillis + TimeZone.getDefault().getOffset(calendar.timeInMillis)
    val dateFormatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    val initialDateMillis = if (transaction != null) {
        try {
            val parsedDate = dateFormatter.parse(transaction.date)
            parsedDate!!.time + TimeZone.getDefault().getOffset(parsedDate.time)
        } catch (e: Exception) { localTimeWithOffset }
    } else {
        localTimeWithOffset
    }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDateMillis)
    val dateDisplay = dateFormatter.format(
        Date(datePickerState.selectedDateMillis ?: initialDateMillis))

    if (showDatePicker) {
        DatePickerDialog(onDismissRequest = { showDatePicker = false }, confirmButton = { TextButton(onClick = { showDatePicker = false }) { Text("OK") } })
        { DatePicker(state = datePickerState) }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Hapus Transaksi", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
            text = { Text("Yakin ingin menghapus transaksi ini? Tindakan ini tidak bisa dibatalkan.", fontSize = 14.sp) },
            confirmButton = {
                TextButton(onClick = {
                    if (transaction != null) onDelete?.invoke(transaction)
                    showDeleteConfirm = false
                }) {
                    Text("Hapus", color = RedExpense, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Batal", color = TextSecondary)
                }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = Color.White
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState())) {
                Text(
                    text = if (transaction == null) "Tambah Transaksi" else "Detail Transaksi",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp
                )
                Spacer(modifier = Modifier.height(20.dp))

                Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0xFFF8FAFC))) {
                    val m = Modifier.weight(1f)
                    TypeButton("Keluar", selectedType == TransactionType.EXPENSE, RedExpense, m.clickable { selectedType = TransactionType.EXPENSE })
                    TypeButton("Masuk", selectedType == TransactionType.INCOME, GreenIncome, m.clickable { selectedType = TransactionType.INCOME })
                }

                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(value = dateDisplay, onValueChange = {}, readOnly = true, label = { Text("Tanggal", fontSize = 13.sp) }, textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
                    trailingIcon = { IconButton(onClick = { showDatePicker = true }) { Icon(Icons.Default.Event, null) } },
                    modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true }, shape = RoundedCornerShape(12.dp))

                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Judul Transaksi", fontSize = 13.sp) }, textStyle = LocalTextStyle.current.copy(fontSize = 14.sp), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))

                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = amount, onValueChange = { amount = it.filter { c -> c.isDigit() } }, label = { Text("Nominal", fontSize = 13.sp) }, textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
                    prefix = { Text("Rp ", color = TextSecondary) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))

                Spacer(modifier = Modifier.height(8.dp))
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                    OutlinedTextField(value = selectedCategory, onValueChange = {}, readOnly = true, label = { Text("Kategori", fontSize = 13.sp) }, textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) }, modifier = Modifier.fillMaxWidth().menuAnchor(), shape = RoundedCornerShape(12.dp))
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        val cats = if (selectedType == TransactionType.INCOME) incomeCategories else expenseCategories
                        cats.forEach { cat -> DropdownMenuItem(text = { Text(cat) }, onClick = { selectedCategory = cat; expanded = false }) }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("Catatan Tambahan", fontSize = 13.sp) }, textStyle = LocalTextStyle.current.copy(fontSize = 14.sp), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))

                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = {
                    val amt = amount.toDoubleOrNull() ?: 0.0
                    if (title.isNotBlank() && amt > 0 && selectedCategory.isNotBlank()) {
                        onAdd(Transaction(
                            id = transaction?.id ?: 0,
                            title = title,
                            amount = amt,
                            category = selectedCategory,
                            type = selectedType,
                            date = dateDisplay,
                            note = note
                        ))                    }
                }, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor))
                { Text("Simpan", fontWeight = FontWeight.Bold, fontSize = 16.sp) }
                if (transaction != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Delete, null, tint = RedExpense, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Hapus Transaksi", color = RedExpense, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────
// REUSABLE COMPONENTS
// ─────────────────────────────────────────────

@Composable
fun SimplePieChart(data: List<Pair<String, Double>>) {
    val total = data.sumOf { it.second }
    Canvas(modifier = Modifier.size(200.dp)) {
        var startAngle = -90f
        data.forEachIndexed { index, item ->
            val sweepAngle = (if(total>0) (item.second / total).toFloat() * 360f else 0f)
            drawArc(color = ChartColors[index % ChartColors.size], startAngle = startAngle, sweepAngle = sweepAngle, useCenter = true)
            startAngle += sweepAngle
        }
        drawCircle(color = CardColor, radius = size.minDimension / 3.2f)
    }
}

@Composable
fun TransactionCard(transaction: Transaction, onClick: () -> Unit) {
    val isIncome = transaction.type == TransactionType.INCOME
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = CardColor),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(0.5.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(if(isIncome) GreenLight else RedLight), contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (isIncome) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward, // <--- TUKAR DI SINI
                    contentDescription = null,
                    tint = if (isIncome) GreenIncome else RedExpense
                )            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(transaction.title, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text("${transaction.category} • ${transaction.date}", fontSize = 12.sp, color = TextSecondary)
                if(transaction.note.isNotEmpty()) Text(transaction.note, fontSize = 11.sp, color = TextSecondary, maxLines = 1)
            }
            Text(formatCurrency(transaction.amount), color = if(isIncome) GreenIncome else RedExpense, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
fun BalanceCard(balance: Double, income: Double, expense: Double) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = PrimaryColor), shape = RoundedCornerShape(24.dp)) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text("Total Saldo", color = Color.White.copy(0.7f), fontSize = 14.sp)
            Text(formatCurrency(balance), color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Black)
            Spacer(modifier = Modifier.height(24.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                BalanceInfoItem("Pemasukan", income, Icons.Default.ArrowDownward)
                BalanceInfoItem("Pengeluaran", expense, Icons.Default.ArrowUpward)
            }
        }
    }
}

@Composable
fun BalanceInfoItem(label: String, amount: Double, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(Color.White.copy(0.2f)), contentAlignment = Alignment.Center) {
            Icon(icon, null, modifier = Modifier.size(16.dp), tint = Color.White)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(label, color = Color.White.copy(0.7f), fontSize = 11.sp)
            Text(formatCurrency(amount), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
    }
}

@Composable
fun FilterChipModern(
    selected: Boolean,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = if (selected) PrimaryColor else Color.White,
        border = if (selected) null else androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(vertical = 8.dp),
            color = if (selected) Color.White else TextSecondary,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun TypeButton(label: String, selected: Boolean, selectedColor: Color, modifier: Modifier) {
    Box(
        modifier = modifier
            .padding(4.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) selectedColor else Color.Transparent)
            .padding(10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (selected) Color.White else TextSecondary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun TransactionListScreen(modifier: Modifier, transactions: List<Transaction>, onBack: () -> Unit, onEdit: (Transaction) -> Unit) {
    Column(modifier = modifier.fillMaxSize().background(BackgroundColor)) {
        IconButton(onClick = onBack, modifier = Modifier.padding(8.dp)) { Icon(Icons.Default.ArrowBack, null) }
        Text("Riwayat Transaksi", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(horizontal = 16.dp))
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(transactions) { item ->
                TransactionCard(
                    transaction = item,
                    onClick = { onEdit(item) }
                )
            }
        }
    }
}

fun formatCurrency(amount: Double): String = "Rp " + NumberFormat.getNumberInstance(Locale("id", "ID")).format(amount)

@Composable
fun ScreenHeader(title: String) {
    Text(
        text = title,
        fontSize = 28.sp,
        fontWeight = FontWeight.ExtraBold,
        color = TextPrimary,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp)
    )
}