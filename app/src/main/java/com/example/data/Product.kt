package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class Product(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val sku: String, // shtrix-kod yoki artikul
    val category: String, // Kategoriya emas, balki "Ichimliklar", "Yeguliklar", etc.
    val buyPrice: Double, // Sotib olingan narxi
    val sellPrice: Double, // Sotilish narxi (Sotuv)
    val stock: Double, // Ombor qoldig'i (kg, litr yoki dona)
    val unit: String // O'lchov birligi ("dona", "kg", "litr")
) {
    // Kategoriya ro'yxati do'kon uchun
    companion object {
        val CATEGORIES = listOf(
            "Oziq-ovqat",
            "Ichimliklar",
            "Sut mahsulotlari",
            "Meva va Sabzavotlar",
            "Shirinliklar",
            "Konservalar",
            "Xo'jalik mollari",
            "Boshqa"
        )
        
        val UNITS = listOf(
            "dona",
            "kg",
            "litr",
            "metr",
            "qop"
        )
    }
}
