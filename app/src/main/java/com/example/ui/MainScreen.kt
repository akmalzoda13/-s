package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Product
import com.example.data.Sale
import com.example.data.SaleItem
import java.text.SimpleDateFormat
import java.util.*

// Pul miqdorlarini chiroyli formatlash uchun yordamchi funksiya (Masalan: 12 500 UZS/so'm)
fun formatSum(amount: Double): String {
    return String.format(Locale.US, "%,.0f", amount).replace(',', ' ') + " so'm"
}

// Vaqt formatlash
fun formatTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: ShopViewModel) {
    var selectedTab by remember { mutableStateOf(0) }
    val products by viewModel.products.collectAsStateWithLifecycle()
    val lowStock by viewModel.lowStockProducts.collectAsStateWithLifecycle()
    val salesHistory by viewModel.salesHistory.collectAsStateWithLifecycle()
    val activeDebts by viewModel.activeDebts.collectAsStateWithLifecycle()

    val currentLanguage by viewModel.currentLanguage.collectAsStateWithLifecycle()
    val storeName by viewModel.storeName.collectAsStateWithLifecycle()

    var showLanguageDialog by remember { mutableStateOf(false) }
    var showStoreNameDialog by remember { mutableStateOf(false) }

    // Namuna ma'lumotlarni bir marta yuklab qo'yamiz (agar baza bo'sh bo'lsa)
    LaunchedEffect(Unit) {
        viewModel.loadSampleData()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { showStoreNameDialog = true }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Storefront,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = storeName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = Translation.get("store_name_edit"),
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                    modifier = Modifier.size(10.dp)
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ),
                actions = {
                    // Tilni o'zgartirish tugmasi
                    TextButton(
                        onClick = { showLanguageDialog = true },
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (currentLanguage == "ru") "Русский" else "O'zbekcha",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(
                tonalElevation = 8.dp,
                modifier = Modifier.testTag("navigation_bar")
            ) {
                val tabs = listOf(
                    Triple(Translation.get("dashboard"), Icons.Default.Dashboard, Icons.Outlined.Dashboard),
                    Triple(Translation.get("pos"), Icons.Default.ShoppingCart, Icons.Outlined.ShoppingCart),
                    Triple(Translation.get("warehouse"), Icons.Default.Inventory, Icons.Outlined.Inventory),
                    Triple(Translation.get("history"), Icons.Default.History, Icons.Outlined.History),
                    Triple(Translation.get("debts"), Icons.Default.Assignment, Icons.Outlined.Assignment)
                )

                tabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = {
                            Icon(
                                imageVector = if (selectedTab == index) tab.second else tab.third,
                                contentDescription = tab.first
                            )
                        },
                        label = { Text(tab.first, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        alwaysShowLabel = true
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> DashboardTab(viewModel, products, lowStock, salesHistory, activeDebts)
                1 -> KassaTab(viewModel, products)
                2 -> OmborTab(viewModel, products)
                3 -> TarixTab(viewModel, salesHistory)
                4 -> NasiyaTab(viewModel, activeDebts)
            }
        }
    }

    // 1. Tilni tanlash dialogi
    if (showLanguageDialog) {
        Dialog(onDismissRequest = { showLanguageDialog = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = Translation.get("select_lang"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    Button(
                        onClick = {
                            viewModel.setLanguage("uz")
                            showLanguageDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (currentLanguage == "uz") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (currentLanguage == "uz") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Text("O'zbekcha", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            viewModel.setLanguage("ru")
                            showLanguageDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (currentLanguage == "ru") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (currentLanguage == "ru") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Text("Русский", fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    TextButton(onClick = { showLanguageDialog = false }) {
                        Text(Translation.get("back"))
                    }
                }
            }
        }
    }

    // 2. Do'kon nomini tahrirlash dialogi
    if (showStoreNameDialog) {
        var tempStoreName by remember { mutableStateOf(storeName) }
        Dialog(onDismissRequest = { showStoreNameDialog = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth()
                ) {
                    Text(
                        text = Translation.get("store_name_edit"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    OutlinedTextField(
                        value = tempStoreName,
                        onValueChange = { tempStoreName = it },
                        label = { Text(Translation.get("store_name_label")) },
                        placeholder = { Text(Translation.get("store_name_placeholder")) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showStoreNameDialog = false }) {
                            Text(Translation.get("cancel"))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (tempStoreName.trim().isNotEmpty()) {
                                    viewModel.setStoreName(tempStoreName)
                                    showStoreNameDialog = false
                                }
                            }
                        ) {
                            Text(Translation.get("save"))
                        }
                    }
                }
            }
        }
    }
}

// ---------------------- 1-TAB: HISOBOTLAR / DASHBOARD ----------------------
@Composable
fun DashboardTab(
    viewModel: ShopViewModel,
    products: List<Product>,
    lowStock: List<Product>,
    sales: List<Sale>,
    debts: List<Sale>
) {
    // Statisika hisoblash
    val todayStart = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    val todaySales = sales.filter { it.timestamp >= todayStart }
    val todayRevenue = todaySales.sumOf { it.totalAmount }
    val todayProfit = todaySales.sumOf { it.totalProfit }
    val totalUnpaidDebt = debts.sumOf { it.totalAmount }

    val totalStockValue = products.sumOf { it.stock * it.sellPrice }
    val totalBuyValue = products.sumOf { it.stock * it.buyPrice }
    val expectedProfit = totalStockValue - totalBuyValue

    ScrollableColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = Translation.get("today_financial"),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Asosiy ko'rsatkichlar griddi
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.TrendingUp, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(Translation.get("today_revenue"), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(formatSum(todayRevenue), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }

            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)) // Sof yashil foni
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.MonetizationOn, contentDescription = null, tint = Color(0xFF2E7D32))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(Translation.get("today_profit"), fontSize = 12.sp, color = Color(0xFF1B5E20), fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(formatSum(todayProfit), fontSize = 16.sp, color = Color(0xFF1B5E20), fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AssignmentLate, contentDescription = null, tint = Color(0xFFE65100))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(Translation.get("active_debts_card"), fontSize = 12.sp, color = Color(0xFFE65100), fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(formatSum(totalUnpaidDebt), fontSize = 16.sp, color = Color(0xFFE65100), fontWeight = FontWeight.Bold)
                }
            }

            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Storefront, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(Translation.get("warehouse_value"), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(formatSum(totalStockValue), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Ogohlantirishlar - Kam Qolgan Mahsulotlar (Stock Alert)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = Translation.get("low_stock_alert"),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error
            )
            Badge(containerColor = MaterialTheme.colorScheme.error) {
                Text(
                    text = "${lowStock.size} " + (if (Translation.currentLanguage == "ru") "шт" else "dona"),
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))

        if (lowStock.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(Translation.get("all_stock_ok"), style = MaterialTheme.typography.bodyMedium)
                }
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f))
            ) {
                Column {
                    lowStock.take(5).forEach { product ->
                        val displayUnit = when (product.unit) {
                            "kg" -> Translation.get("units_kg")
                            "litr" -> Translation.get("units_litr")
                            "metr" -> Translation.get("units_metr")
                            "qop" -> Translation.get("units_qop")
                            else -> Translation.get("units_dona")
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(product.name, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("SKU / Artikul: ${product.sku}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    "${product.stock} $displayUnit",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Text(Translation.get("low_stock_warning"), fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Biznes Tahlil
        Text(
            text = Translation.get("warehouse_analytics"),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                AnalyticsRow(
                    label = Translation.get("product_types") + ":",
                    value = "${products.size} " + (if (Translation.currentLanguage == "ru") "вид(ов)" else "tur")
                )
                AnalyticsRow(
                    label = Translation.get("expected_profit") + ":",
                    value = formatSum(expectedProfit)
                )
                AnalyticsRow(
                    label = Translation.get("warehouse_cost") + ":",
                    value = formatSum(totalBuyValue)
                )
                AnalyticsRow(
                    label = Translation.get("today_checks") + ":",
                    value = "${todaySales.size} " + (if (Translation.currentLanguage == "ru") "чек(ов)" else "dona chek")
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Ilovani yuklab olish haqida yo'riqnoma qismi (Foydalanuvchi "buni qanday yuklab olaman endi" deb so'ragani uchun yordam)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = Translation.get("download_hint_title"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = Translation.get("download_hint_desc"),
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 20.sp
                )
            }
        }
    }
}

@Composable
fun AnalyticsRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
    }
}


// ---------------------- 2-TAB: POS KASSA ----------------------
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun KassaTab(viewModel: ShopViewModel, products: List<Product>) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Barchasi") }
    val cartItems by viewModel.cartItems.collectAsStateWithLifecycle()
    val cartTotal by viewModel.cartTotal.collectAsStateWithLifecycle()

    var productToAddQuantity by remember { mutableStateOf<Product?>(null) }
    var showCheckoutDialog by remember { mutableStateOf(false) }

    // Qidiruv maydoni viewmodelga yuborish
    LaunchedEffect(searchQuery) {
        viewModel.searchQuery.value = searchQuery
    }

    // Saralangan mahsulotlar ro'yxati (kategoriya bo'yicha)
    val filteredProducts = if (selectedCategory == "Barchasi") {
        products
    } else {
        products.filter { it.category == selectedCategory }
    }

    // Ikki ustunli layout: Kassa tushunarliligi uchun ekran kengligi moslashtirilgan
    Row(modifier = Modifier.fillMaxSize()) {
        // Chap taraf: Mahsulotlar paneli
        Column(
            modifier = Modifier
                .weight(1.3f)
                .fillMaxHeight()
                .padding(start = 12.dp, top = 12.dp, bottom = 12.dp, end = 6.dp)
        ) {
            // Qidiruv inputi
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text(Translation.get("search_product")) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("kassa_search_bar")
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Kategoriya tanlov pufakchalari (Pills)
            val categories = listOf("Barchasi") + Product.CATEGORIES
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                categories.forEach { cat ->
                    val displayCat = if (cat == "Barchasi") Translation.get("all_categories") else cat
                    val isSelected = selectedCategory == cat
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCategory = cat },
                        label = { Text(displayCat, fontSize = 12.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Mahsulot panjarasi (Product list)
            if (filteredProducts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1.3f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            Translation.get("not_found"),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredProducts) { prod ->
                        val isLow = prod.stock <= 0
                        val displayUnit = when (prod.unit) {
                            "kg" -> Translation.get("units_kg")
                            "litr" -> Translation.get("units_litr")
                            "metr" -> Translation.get("units_metr")
                            "qop" -> Translation.get("units_qop")
                            else -> Translation.get("units_dona")
                        }
                        Card(
                            onClick = {
                                if (prod.stock > 0) {
                                    productToAddQuantity = prod
                                }
                            },
                            enabled = !isLow,
                            colors = CardDefaults.cardColors(
                                containerColor = if (isLow) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surface
                            ),
                            border = BorderStroke(
                                1.dp,
                                if (isLow) MaterialTheme.colorScheme.error.copy(alpha = 0.3f) else MaterialTheme.colorScheme.outlineVariant
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = prod.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    "${Translation.get("category").take(3)}: ${prod.category}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = formatSum(prod.sellPrice),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (isLow) (if (Translation.currentLanguage == "ru") "Нет" else "Qolmagan") else "${prod.stock} $displayUnit",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (prod.stock <= 5) MaterialTheme.colorScheme.error else Color(0xFF2E7D32)
                                    )
                                    Icon(
                                        Icons.Default.AddShoppingCart,
                                        contentDescription = "Savatga qo'shish",
                                        tint = if (isLow) Color.Gray else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // O'ng taraf: Savat oynasi (Cart Pane)
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                .padding(horizontal = 12.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(Translation.get("cart"), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                }
                if (cartItems.isNotEmpty()) {
                    TextButton(onClick = { viewModel.clearCart() }) {
                        Text(Translation.get("clear_cart"), color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Savat elementlari ro'yxati
            if (cartItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.ShoppingCart,
                            contentDescription = null,
                            modifier = Modifier.size(54.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            Translation.get("cart_empty"),
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            Translation.get("cart_empty_hint"),
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(cartItems) { item ->
                        val displayUnit = when (item.product.unit) {
                            "kg" -> Translation.get("units_kg")
                            "litr" -> Translation.get("units_litr")
                            "metr" -> Translation.get("units_metr")
                            "qop" -> Translation.get("units_qop")
                            else -> Translation.get("units_dona")
                        }
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        item.product.name,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = { viewModel.removeFromCart(item.product.id) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Savatdan o'chirish",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "${formatSum(item.product.sellPrice)} / $displayUnit",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        formatSum(item.product.sellPrice * item.quantity),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                // Mikdorni o'zgartirish knopkalari (Counter)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = { viewModel.updateCartQuantity(item.product.id, item.quantity - 1) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.Remove, contentDescription = "Kamaytirish", modifier = Modifier.size(14.dp))
                                    }
                                    
                                    Text(
                                        text = if (item.product.unit == "kg") "%.1f".format(item.quantity) else "${item.quantity.toInt()}",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp)
                                    )

                                    IconButton(
                                        onClick = { viewModel.updateCartQuantity(item.product.id, item.quantity + 1) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = "Ko'paytirish", modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Spacer(modifier = Modifier.height(10.dp))

            // Jami summa paneli
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(Translation.get("total_sum") + ":", fontSize = 14.sp)
                    Text(formatSum(cartTotal), fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = { showCheckoutDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("checkout_button"),
                    enabled = cartItems.isNotEmpty()
                ) {
                    Icon(Icons.Default.Paid, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(Translation.get("checkout"), fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // 1. Mahsulot miqdorini kiritish Dialogi (kg yoki dona tanlov uchun)
    productToAddQuantity?.let { prod ->
        var quantityInput by remember { mutableStateOf("1") }
        var isError by remember { mutableStateOf(false) }
        val displayUnit = when (prod.unit) {
            "kg" -> Translation.get("units_kg")
            "litr" -> Translation.get("units_litr")
            "metr" -> Translation.get("units_metr")
            "qop" -> Translation.get("units_qop")
            else -> Translation.get("units_dona")
        }

        Dialog(onDismissRequest = { productToAddQuantity = null }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        prod.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        "${Translation.get("unit_price_label")}: ${formatSum(prod.sellPrice)} / $displayUnit",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "${Translation.get("in_stock")}: ${prod.stock} $displayUnit",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (prod.stock <= 5) MaterialTheme.colorScheme.error else Color(0xFF2E7D32)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = quantityInput,
                        onValueChange = {
                            quantityInput = it
                            isError = false
                        },
                        label = { Text("${Translation.get("enter_quantity")} ($displayUnit)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        isError = isError,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (isError) {
                        Text(
                            Translation.get("invalid_qty"),
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Tezkash tugmalar (+0.5, +1, +2, +5)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("1", "2", "5", "10").forEach { quick ->
                            ElevatedButton(
                                onClick = { quantityInput = quick },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("+$quick")
                            }
                        }
                    }
                    if (prod.unit == "kg") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("0.5", "1.5", "2.5", "0.2").forEach { quick ->
                                ElevatedButton(
                                    onClick = { quantityInput = quick },
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text("${quick}kg")
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { productToAddQuantity = null }) {
                            Text(Translation.get("cancel"))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val qty = quantityInput.toDoubleOrNull()
                                if (qty == null || qty <= 0.0 || qty > prod.stock) {
                                    isError = true
                                } else {
                                    viewModel.addToCart(prod, qty)
                                    productToAddQuantity = null
                                }
                            }
                        ) {
                            Text(Translation.get("add"))
                        }
                    }
                }
            }
        }
    }

    // 2. Kassa to'lov Dialogi
    if (showCheckoutDialog) {
        var paymentType by remember { mutableStateOf("Naqd") }
        var customerName by remember { mutableStateOf("") }
        var isNasiyaError by remember { mutableStateOf(false) }

        val cashTranslated = Translation.get("cash")
        val cardTranslated = Translation.get("card")
        val debtTranslated = Translation.get("debt")

        Dialog(onDismissRequest = { showCheckoutDialog = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(18.dp)
                        .fillMaxWidth()
                ) {
                    Text(
                        Translation.get("checkout"),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        "${Translation.get("total_sum")}: ${formatSum(cartTotal)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("${Translation.get("payment_type")}:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val paymentOptions = listOf(
                            Triple("Naqd", cashTranslated, Sale.PAYMENT_CASH),
                            Triple("Plastik", cardTranslated, Sale.PAYMENT_CARD),
                            Triple("Nasiya", debtTranslated, Sale.PAYMENT_DEBT)
                        )
                        paymentOptions.forEach { (key, title, code) ->
                            val selected = paymentType == key
                            ElevatedFilterChip(
                                selected = selected,
                                onClick = {
                                    paymentType = key
                                    if (key != "Nasiya") isNasiyaError = false
                                },
                                label = { Text(title, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Agar Nasiya tanlansa yoki xaridor ismi ixtiyoriy bo'lsa
                    val isNasiya = paymentType == "Nasiya"
                    val finalPayCode = if (paymentType == "Naqd") Sale.PAYMENT_CASH else if (paymentType == "Plastik") Sale.PAYMENT_CARD else Sale.PAYMENT_DEBT

                    OutlinedTextField(
                        value = customerName,
                        onValueChange = {
                            customerName = it
                            if (isNasiya && it.isNotBlank()) {
                                isNasiyaError = false
                            }
                        },
                        label = { Text(if (isNasiya) Translation.get("debtor_name") else Translation.get("customer_name_opt")) },
                        singleLine = true,
                        isError = isNasiyaError,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (isNasiyaError) {
                        Text(
                            Translation.get("debt_name_required"),
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showCheckoutDialog = false }) {
                            Text(Translation.get("back"))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Button(
                            onClick = {
                                if (isNasiya && customerName.isBlank()) {
                                    isNasiyaError = true
                                } else {
                                    viewModel.checkout(
                                        paymentType = finalPayCode,
                                        customerName = customerName,
                                        onSuccess = {
                                            showCheckoutDialog = false
                                        }
                                    )
                                }
                            }
                        ) {
                            Text(Translation.get("confirm_sale"))
                        }
                    }
                }
            }
        }
    }
}


// ---------------------- 3-TAB: OMBOR (PRODUCTS CRUD) ----------------------
@Composable
fun OmborTab(viewModel: ShopViewModel, products: List<Product>) {
    var searchQuery by remember { mutableStateOf("") }
    var showDialog by remember { mutableStateOf(false) }
    var editingProduct by remember { mutableStateOf<Product?>(null) }

    LaunchedEffect(searchQuery) {
        viewModel.searchQuery.value = searchQuery
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingProduct = null
                    showDialog = true
                },
                modifier = Modifier.testTag("add_product_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Yangi mahsulot")
            }
        }
    ) { paddingVals ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingVals)
                .padding(16.dp)
        ) {
            Text(
                Translation.get("warehouse_and_stock"),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text(Translation.get("search_inventory")) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (products.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(Translation.get("no_inventory"), color = Color.Gray, textAlign = TextAlign.Center)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(products) { prod ->
                        val isDanger = prod.stock <= 5.0
                        val displayUnit = when (prod.unit) {
                            "kg" -> Translation.get("units_kg")
                            "litr" -> Translation.get("units_litr")
                            "metr" -> Translation.get("units_metr")
                            "qop" -> Translation.get("units_qop")
                            else -> Translation.get("units_dona")
                        }
                        Card(
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isDanger) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    editingProduct = prod
                                    showDialog = true
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(prod.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.QrCode, contentDescription = null, modifier = Modifier.size(11.dp), tint = Color.Gray)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("${Translation.get("sku")}: ${prod.sku}", fontSize = 11.sp, color = Color.Gray)
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text("${Translation.get("category").take(3)}: ${prod.category}", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row {
                                        Text("${if (Translation.currentLanguage == "ru") "Закуп" else "Kelim"}: ${formatSum(prod.buyPrice)}", fontSize = 11.sp, color = Color(0xFFE65100))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("${if (Translation.currentLanguage == "ru") "Продажа" else "Sotuv"}: ${formatSum(prod.sellPrice)}", fontSize = 11.sp, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                                    }
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        "${prod.stock} $displayUnit",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 15.sp,
                                        color = if (isDanger) MaterialTheme.colorScheme.error else Color(0xFF2E7D32)
                                    )
                                    Text(
                                        text = if (isDanger) Translation.get("low_stock_warning") else Translation.get("amount"),
                                        fontSize = 11.sp,
                                        color = if (isDanger) MaterialTheme.colorScheme.error else Color.Gray,
                                        fontWeight = FontWeight.Bold
                                    )
                                    
                                    Spacer(modifier = Modifier.height(6.dp))
                                    
                                    // Delete action directly
                                    IconButton(
                                        onClick = { viewModel.deleteProduct(prod) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = "O'chirish", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Yangi yoki Tahrirlash Dialogi
    if (showDialog) {
        var name by remember { mutableStateOf(editingProduct?.name ?: "") }
        var sku by remember { mutableStateOf(editingProduct?.sku ?: "") }
        var category by remember { mutableStateOf(editingProduct?.category ?: Product.CATEGORIES[0]) }
        var buyPrice by remember { mutableStateOf(editingProduct?.buyPrice?.toString() ?: "") }
        var sellPrice by remember { mutableStateOf(editingProduct?.sellPrice?.toString() ?: "") }
        var stock by remember { mutableStateOf(editingProduct?.stock?.toString() ?: "") }
        var unit by remember { mutableStateOf(editingProduct?.unit ?: Product.UNITS[0]) }

        var isError by remember { mutableStateOf(false) }

        Dialog(onDismissRequest = { showDialog = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = if (editingProduct == null) Translation.get("add_new_product") else Translation.get("edit_product"),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("${Translation.get("product_name")} *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = sku,
                        onValueChange = { sku = it },
                        label = { Text("${Translation.get("barcode")} *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text("${Translation.get("category")}:", fontSize = 11.sp, color = Color.Gray)
                    var catExpanded by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { catExpanded = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(category)
                            Spacer(modifier = Modifier.weight(1f))
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                        DropdownMenu(expanded = catExpanded, onDismissRequest = { catExpanded = false }) {
                            Product.CATEGORIES.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat) },
                                    onClick = {
                                        category = cat
                                        catExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = buyPrice,
                            onValueChange = { buyPrice = it },
                            label = { Text("${Translation.get("buy_price")} *") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = sellPrice,
                            onValueChange = { sellPrice = it },
                            label = { Text("${Translation.get("sell_price")} *") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = stock,
                            onValueChange = { stock = it },
                            label = { Text("${Translation.get("quantity_limit")} *") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1.2f)
                        )

                        // O'lchov birligi dropdownd
                        var unitExpanded by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.weight(1f).padding(top = 8.dp)) {
                            OutlinedButton(
                                onClick = { unitExpanded = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 6.dp)
                            ) {
                                val currentUnitDisplay = when (unit) {
                                    "kg" -> Translation.get("units_kg")
                                    "litr" -> Translation.get("units_litr")
                                    "metr" -> Translation.get("units_metr")
                                    "qop" -> Translation.get("units_qop")
                                    else -> Translation.get("units_dona")
                                }
                                Text(currentUnitDisplay, fontSize = 12.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(12.dp))
                            }
                            DropdownMenu(expanded = unitExpanded, onDismissRequest = { unitExpanded = false }) {
                                Product.UNITS.forEach { un ->
                                    val unitDisplayMenu = when (un) {
                                        "kg" -> Translation.get("units_kg")
                                        "litr" -> Translation.get("units_litr")
                                        "metr" -> Translation.get("units_metr")
                                        "qop" -> Translation.get("units_qop")
                                        else -> Translation.get("units_dona")
                                    }
                                    DropdownMenuItem(
                                        text = { Text(unitDisplayMenu) },
                                        onClick = {
                                            unit = un
                                            unitExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    if (isError) {
                        Text(
                            Translation.get("empty_fields_error"),
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showDialog = false }) {
                            Text(Translation.get("cancel"))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Button(
                            onClick = {
                                val buy = buyPrice.toDoubleOrNull()
                                val sell = sellPrice.toDoubleOrNull()
                                val stk = stock.toDoubleOrNull()

                                if (name.isBlank() || sku.isBlank() || buy == null || sell == null || stk == null) {
                                    isError = true
                                } else {
                                    viewModel.saveProduct(
                                        id = editingProduct?.id ?: 0L,
                                        name = name,
                                        sku = sku,
                                        category = category,
                                        buyPrice = buy,
                                        sellPrice = sell,
                                        stock = stk,
                                        unit = unit,
                                        onComplete = {
                                            showDialog = false
                                        }
                                    )
                                }
                            }
                        ) {
                            Text(Translation.get("save"))
                        }
                    }
                }
            }
        }
    }
}


// ---------------------- 4-TAB: SAVDO TARIXI / RECEIPTS JOURNAL ----------------------
@Composable
fun TarixTab(viewModel: ShopViewModel, sales: List<Sale>) {
    var expandedSaleId by remember { mutableStateOf<Long?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                Translation.get("history_title"),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            val netRevenue = sales.sumOf { it.totalAmount }
            val netProfit = sales.sumOf { it.totalProfit }
            Column(horizontalAlignment = Alignment.End) {
                Text("${Translation.get("total_revenue_short")}: ${formatSum(netRevenue)}", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text("${Translation.get("expected_profit")}: ${formatSum(netProfit)}", fontSize = 10.sp, color = Color(0xFF2E7D32))
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (sales.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(Translation.get("no_sales_warning"), color = Color.Gray, textAlign = TextAlign.Center)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(sales) { sale ->
                    val isExpanded = expandedSaleId == sale.id
                    Card(
                        border = BorderStroke(
                            1.dp,
                            if (sale.paymentType == Sale.PAYMENT_DEBT) Color(0xFFFFE0B2) else MaterialTheme.colorScheme.outlineVariant
                        ),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isExpanded) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surface
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("${Translation.get("check_prefix")} #SVD-${sale.id}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(formatTime(sale.timestamp), fontSize = 11.sp, color = Color.Gray)
                                }

                                val paymentText = when (sale.paymentType) {
                                    "Naqd" -> Translation.get("cash")
                                    "Plastik" -> Translation.get("card")
                                    else -> Translation.get("debt")
                                }
                                val statusText = paymentText + if (sale.paymentType == "Nasiya" && sale.isPaid) (if (Translation.currentLanguage == "ru") " (Закрыт)" else " (Yopildi)") else ""

                                Surface(
                                    color = when (sale.paymentType) {
                                        "Naqd" -> Color(0xFFE8F5E9)
                                        "Plastik" -> Color(0xFFE0F7FA)
                                        else -> Color(0xFFFFF3E0)
                                    },
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                ) {
                                    Text(
                                        text = statusText,
                                        color = when (sale.paymentType) {
                                            "Naqd" -> Color(0xFF2E7D32)
                                            "Plastik" -> Color(0xFF006064)
                                            else -> Color(0xFFE65100)
                                        },
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${Translation.get("customer_label")}: " + if (sale.customerName.isBlank()) Translation.get("general_buyer") else sale.customerName,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = formatSum(sale.totalAmount),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            // Kengaytirilganda sotilgan chekdagi tovarlarni ko'rsatish
                            if (isExpanded) {
                                val collectedItems by viewModel.getItemsForSale(sale.id).collectAsStateWithLifecycle(emptyList())
                                
                                AnimatedVisibility(visible = true) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 8.dp)
                                            .background(
                                                MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                                                RoundedCornerShape(6.dp)
                                            )
                                            .padding(8.dp)
                                    ) {
                                        Text(Translation.get("sold_items_list"), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray)
                                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant)
                                        
                                        collectedItems.forEach { item ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 2.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(item.productName, fontSize = 12.sp, modifier = Modifier.weight(1.5f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                Text(
                                                    "${item.quantity} x ${formatSum(item.sellPrice)}",
                                                    fontSize = 11.sp,
                                                    color = Color.Gray,
                                                    modifier = Modifier.weight(1.2f),
                                                    textAlign = TextAlign.End
                                                )
                                                Text(
                                                    formatSum(item.sellPrice * item.quantity),
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.weight(1f),
                                                    textAlign = TextAlign.End
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                "${Translation.get("net_profit_label")}: ${formatSum(sale.totalProfit)}",
                                                color = Color(0xFF2E7D32),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            )
                                            ElevatedButton(
                                                onClick = { viewModel.cancelSale(sale.id) },
                                                colors = ButtonDefaults.elevatedButtonColors(
                                                    containerColor = MaterialTheme.colorScheme.errorContainer,
                                                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                                                ),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                modifier = Modifier.height(26.dp)
                                            ) {
                                                Icon(Icons.Default.Undo, contentDescription = null, modifier = Modifier.size(12.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(Translation.get("cancel_sale_action"), fontSize = 10.sp)
                                            }
                                        }
                                    }
                                }
                            }

                            // Yoyish/yig'ish knopkasi
                            TextButton(
                                onClick = { expandedSaleId = if (isExpanded) null else sale.id },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text(if (isExpanded) Translation.get("close_details") else Translation.get("view_details"), fontSize = 11.sp)
                                Icon(
                                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}


// ---------------------- 5-TAB: NASIYA (DEBT REGISTER) ----------------------
@Composable
fun NasiyaTab(viewModel: ShopViewModel, debts: List<Sale>) {
    var showDebtSettleConfirm by remember { mutableStateOf<Sale?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            Translation.get("debt_notebook"),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        Text(
            Translation.get("debt_subtitle"),
            fontSize = 11.sp,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 14.dp)
        )

        val totalDebtValue = debts.sumOf { it.totalAmount }
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f))
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(Translation.get("total_debt_value"), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                Text(
                    formatSum(totalDebtValue),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        if (debts.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Celebration, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(Translation.get("debt_empty"), fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32), textAlign = TextAlign.Center)
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(debts) { sale ->
                    Card(
                        border = BorderStroke(1.dp, Color(0xFFFFE0B2)),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = sale.customerName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text("${if (Translation.currentLanguage == "ru") "Время" else "Vaqti"}: ${formatTime(sale.timestamp)}", fontSize = 11.sp, color = Color.Gray)
                                Text("${if (Translation.currentLanguage == "ru") "Документ" else "Hujjat"}: ${Translation.get("check_prefix")} #SVD-${sale.id}", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = formatSum(sale.totalAmount),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Button(
                                    onClick = { showDebtSettleConfirm = sale },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Icon(Icons.Default.Done, contentDescription = null, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(Translation.get("pay_debt"), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Debt Settle Confirmation Dialog
    showDebtSettleConfirm?.let { sale ->
        val confirmBody = if (Translation.currentLanguage == "ru") {
            "Была ли полностью возвращена задолженность в размере ${formatSum(sale.totalAmount)} от ${sale.customerName}?\\nЭто действие отметит долг как закрытый в системе отчетов."
        } else {
            "${sale.customerName} tomondan kiritilgan ${formatSum(sale.totalAmount)} qarz to'liq qaytarildimi?\\nUshbu amal hisobot tizimida qarz yopilganini qayd etadi."
        }

        AlertDialog(
            onDismissRequest = { showDebtSettleConfirm = null },
            icon = { Icon(Icons.Default.Payment, contentDescription = null, tint = Color(0xFF2E7D32)) },
            title = { Text(Translation.get("settle_debt_title")) },
            text = {
                Text(
                    text = confirmBody,
                    textAlign = TextAlign.Center
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.payOffDebt(sale.id)
                        showDebtSettleConfirm = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                ) {
                    Text(Translation.get("confirm_settled"))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDebtSettleConfirm = null }) {
                    Text(Translation.get("cancel"))
                }
            }
        )
    }
}

// Custom Scrollable Column helper to deal with standard lists inside screen scrolling issues
@Composable
fun ScrollableColumn(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState())
    ) {
        content()
    }
}
