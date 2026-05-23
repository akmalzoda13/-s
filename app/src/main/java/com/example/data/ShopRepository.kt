package com.example.data

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow

class ShopRepository(private val database: AppDatabase) {
    private val productDao = database.productDao()
    private val saleDao = database.saleDao()

    val allProducts: Flow<List<Product>> = productDao.getAllProducts()
    val lowStockProducts: Flow<List<Product>> = productDao.getLowStockProducts()
    val allSales: Flow<List<Sale>> = saleDao.getAllSales()
    val activeDebts: Flow<List<Sale>> = saleDao.getActiveDebts()

    fun searchProducts(query: String): Flow<List<Product>> {
        return productDao.searchProducts("%$query%")
    }

    suspend fun insertProduct(product: Product): Long = productDao.insertProduct(product)

    suspend fun updateProduct(product: Product) = productDao.updateProduct(product)

    suspend fun deleteProduct(product: Product) = productDao.deleteProduct(product)

    suspend fun getProductBySku(sku: String): Product? = productDao.getProductBySku(sku)

    suspend fun getProductById(id: Long): Product? = productDao.getProductById(id)

    fun getItemsForSale(saleId: Long): Flow<List<SaleItem>> = saleDao.getItemsForSale(saleId)

    // Tranzaksiya orqali Savdo saqlash va Ombor tushumini hisoblash kodi
    suspend fun executeSale(sale: Sale, items: List<SaleItem>) {
        database.withTransaction {
            val saleId = saleDao.insertSale(sale)
            val updatedItems = items.map { it.copy(saleId = saleId) }
            saleDao.insertSaleItems(updatedItems)
            
            // Ombor qoldig'ini kamaytirish (reduce stock)
            for (item in updatedItems) {
                productDao.reduceStock(item.productId, item.quantity)
            }
        }
    }

    // Bekor qilish / Qaytarilgan cheklar
    suspend fun cancelSale(saleId: Long) {
        database.withTransaction {
            val items = saleDao.getItemsForSaleSync(saleId)
            // Ombor qoldig'ini qayta tiklash (increase stock)
            for (item in items) {
                productDao.increaseStock(item.productId, item.quantity)
            }
            saleDao.deleteItemsForSale(saleId)
            saleDao.deleteSale(saleId)
        }
    }

    suspend fun payOffDebt(saleId: Long) {
        saleDao.payOffDebt(saleId)
    }
}
