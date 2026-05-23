package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// Savat elementlari uchun model
data class CartItem(
    val product: Product,
    val quantity: Double
)

class ShopViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val repository = ShopRepository(database)

    private val prefs = application.getSharedPreferences("shop_prefs", android.content.Context.MODE_PRIVATE)

    // Til holati
    private val _currentLanguage = MutableStateFlow(prefs.getString("lang", "uz") ?: "uz")
    val currentLanguage: StateFlow<String> = _currentLanguage.asStateFlow()

    // Do'kon nomi
    private val _storeName = MutableStateFlow(prefs.getString("store_name", "Mening Do'konim") ?: "Mening Do'konim")
    val storeName: StateFlow<String> = _storeName.asStateFlow()

    init {
        Translation.currentLanguage = _currentLanguage.value
    }

    fun setLanguage(lang: String) {
        prefs.edit().putString("lang", lang).apply()
        _currentLanguage.value = lang
        Translation.currentLanguage = lang
    }

    fun setStoreName(name: String) {
        val trimmed = name.trim()
        if (trimmed.isNotEmpty()) {
            prefs.edit().putString("store_name", trimmed).apply()
            _storeName.value = trimmed
        }
    }

    // Qidiruv va mahsulotlar oqimi
    val searchQuery = MutableStateFlow("")

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    val products: StateFlow<List<Product>> = searchQuery
        .debounce(150)
        .flatMapLatest { query ->
            if (query.trim().isEmpty()) {
                repository.allProducts
            } else {
                repository.searchProducts(query)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val lowStockProducts: StateFlow<List<Product>> = repository.lowStockProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val salesHistory: StateFlow<List<Sale>> = repository.allSales
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeDebts: StateFlow<List<Sale>> = repository.activeDebts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Savat holati (Cart State)
    private val _cartItems = MutableStateFlow<List<CartItem>>(emptyList())
    val cartItems: StateFlow<List<CartItem>> = _cartItems.asStateFlow()

    // Savat jami summasi va foyda tahlili
    val cartTotal: StateFlow<Double> = _cartItems
        .map { list -> list.sumOf { it.product.sellPrice * it.quantity } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val cartProfit: StateFlow<Double> = _cartItems
        .map { list -> list.sumOf { (it.product.sellPrice - it.product.buyPrice) * it.quantity } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Mahsulotlar boshqaruvi
    fun saveProduct(
        id: Long = 0,
        name: String,
        sku: String,
        category: String,
        buyPrice: Double,
        sellPrice: Double,
        stock: Double,
        unit: String,
        onComplete: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val product = Product(
                id = id,
                name = name.trim(),
                sku = sku.trim(),
                category = category,
                buyPrice = buyPrice,
                sellPrice = sellPrice,
                stock = stock,
                unit = unit
            )
            if (id == 0L) {
                repository.insertProduct(product)
            } else {
                repository.updateProduct(product)
            }
            onComplete()
        }
    }

    fun deleteProduct(product: Product) {
        viewModelScope.launch {
            repository.deleteProduct(product)
        }
    }

    // Savat boshqaruvi
    fun addToCart(product: Product, quantity: Double = 1.0) {
        val currentList = _cartItems.value.toMutableList()
        val existingIndex = currentList.indexOfFirst { it.product.id == product.id }

        if (existingIndex >= 0) {
            val currentItem = currentList[existingIndex]
            val newQuantity = currentItem.quantity + quantity
            currentList[existingIndex] = currentItem.copy(quantity = newQuantity)
        } else {
            currentList.add(CartItem(product, quantity))
        }

        _cartItems.value = currentList
    }

    fun updateCartQuantity(productId: Long, quantity: Double) {
        if (quantity <= 0.0) {
            removeFromCart(productId)
            return
        }
        val currentList = _cartItems.value.toMutableList()
        val index = currentList.indexOfFirst { it.product.id == productId }
        if (index >= 0) {
            currentList[index] = currentList[index].copy(quantity = quantity)
            _cartItems.value = currentList
        }
    }

    fun removeFromCart(productId: Long) {
        val currentList = _cartItems.value.filter { it.product.id != productId }
        _cartItems.value = currentList
    }

    fun clearCart() {
        _cartItems.value = emptyList()
    }

    // Savdo hisob-kitobini yakunlash va chek chiqarish
    fun checkout(paymentType: String, customerName: String, onSuccess: () -> Unit = {}) {
        val items = _cartItems.value
        if (items.isEmpty()) return

        viewModelScope.launch {
            val totalAmount = cartTotal.value
            val totalProfit = cartProfit.value

            val isPaid = paymentType != Sale.PAYMENT_DEBT

            val sale = Sale(
                timestamp = System.currentTimeMillis(),
                customerName = if (paymentType == Sale.PAYMENT_DEBT) customerName.trim() else if (customerName.isNotBlank()) customerName.trim() else "Savat Savdosi",
                totalAmount = totalAmount,
                totalProfit = totalProfit,
                paymentType = paymentType,
                isPaid = isPaid
            )

            val saleItems = items.map {
                SaleItem(
                    saleId = 0L, // Repository insert qilinganda avtomatik olinadi
                    productId = it.product.id,
                    productName = it.product.name,
                    quantity = it.quantity,
                    sellPrice = it.product.sellPrice,
                    buyPrice = it.product.buyPrice
                )
            }

            repository.executeSale(sale, saleItems)
            clearCart()
            onSuccess()
        }
    }

    // Chekni bekor qilish / Qaytarish qilish
    fun cancelSale(saleId: Long) {
        viewModelScope.launch {
            repository.cancelSale(saleId)
        }
    }

    // Qarzni yopish (Nasiyani so'ndirish)
    fun payOffDebt(saleId: Long) {
        viewModelScope.launch {
            repository.payOffDebt(saleId)
        }
    }

    // Savdo chekidagi mahsulotlarni ko'rish
    fun getItemsForSale(saleId: Long): Flow<List<SaleItem>> {
        return repository.getItemsForSale(saleId)
    }

    // Ba'zi namunaviy ma'lumotlarni yuklash (Ilova birinchi marta ochilganda bo'sh qolmasligi uchun)
    fun loadSampleData() {
        viewModelScope.launch {
            val existing = products.value
            if (existing.isEmpty()) {
                val samples = listOf(
                    Product(name = "Coca-Cola 1.5L", sku = "5449000131805", category = "Ichimliklar", buyPrice = 8500.0, sellPrice = 11000.0, stock = 48.0, unit = "dona"),
                    Product(name = "Pepsi 1.5L", sku = "4820011100022", category = "Ichimliklar", buyPrice = 8300.0, sellPrice = 10500.0, stock = 36.0, unit = "dona"),
                    Product(name = "Buxoro non", sku = "1001", category = "Oziq-ovqat", buyPrice = 2500.0, sellPrice = 3500.0, stock = 100.0, unit = "dona"),
                    Product(name = "Lactel Sut 3.2%", sku = "3051412210034", category = "Sut mahsulotlari", buyPrice = 11000.0, sellPrice = 14500.0, stock = 24.0, unit = "dona"),
                    Product(name = "Moy - Oleina 1L", sku = "4605151010141", category = "Oziq-ovqat", buyPrice = 16000.0, sellPrice = 19500.0, stock = 15.0, unit = "dona"),
                    Product(name = "Meva (Olma qizil)", sku = "1002", category = "Meva va Sabzavotlar", buyPrice = 8000.0, sellPrice = 12000.0, stock = 35.5, unit = "kg"),
                    Product(name = "Meva (Banan import)", sku = "1003", category = "Meva va Sabzavotlar", buyPrice = 15000.0, sellPrice = 22000.0, stock = 12.0, unit = "kg"),
                    Product(name = "Alpen Gold shokolad", sku = "7622210459586", category = "Shirinliklar", buyPrice = 7500.0, sellPrice = 10500.0, stock = 40.0, unit = "dona")
                )
                for (item in samples) {
                    repository.insertProduct(item)
                }
            }
        }
    }
}
