package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sale_items")
data class SaleItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val saleId: Long, // Tegishli chek (Sale) IDsi
    val productId: Long, // Sotilgan mahsulot IDsi
    val productName: String, // O'sha paytdagi mahsulot nomi
    val quantity: Double, // Sotilgan miqdor (masalan: 1.5 kg yoki 3 dona)
    val sellPrice: Double, // Sotilgan paytdagi sotish narxi (bir donasi uchun)
    val buyPrice: Double // Sotilgan paytdagi sotib olish narxi (foyda hisoblash uchun)
)
