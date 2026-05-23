package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SaleDao {
    @Query("SELECT * FROM sales ORDER BY timestamp DESC")
    fun getAllSales(): Flow<List<Sale>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSale(sale: Sale): Long

    @Query("SELECT * FROM sales WHERE id = :id")
    suspend fun getSaleById(id: Long): Sale?

    @Query("DELETE FROM sales WHERE id = :id")
    suspend fun deleteSale(id: Long)

    @Update
    suspend fun updateSale(sale: Sale)

    // Savdo elementlari (Chek dagi tovarlar)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSaleItems(items: List<SaleItem>)

    @Query("SELECT * FROM sale_items WHERE saleId = :saleId")
    fun getItemsForSale(saleId: Long): Flow<List<SaleItem>>

    @Query("SELECT * FROM sale_items WHERE saleId = :saleId")
    suspend fun getItemsForSaleSync(saleId: Long): List<SaleItem>

    @Query("DELETE FROM sale_items WHERE saleId = :saleId")
    suspend fun deleteItemsForSale(saleId: Long)

    // Nasiyalar (Qarz daftari)
    @Query("SELECT * FROM sales WHERE paymentType = 'Nasiya' AND isPaid = 0 ORDER BY timestamp DESC")
    fun getActiveDebts(): Flow<List<Sale>>

    @Query("UPDATE sales SET isPaid = 1 WHERE id = :saleId")
    suspend fun payOffDebt(saleId: Long)
}
