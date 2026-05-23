package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sales")
data class Sale(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val customerName: String = "", // Bo'sh bo'lsa - Umumiy Mijoz (for cash/card), yoki Qarzdor ismi (for Nasiya)
    val totalAmount: Double, // Jami savdo summasi
    val totalProfit: Double, // Jami sof foyda (tushumdan sotib olish narxi ayirilgani)
    val paymentType: String, // "Naqd", "Plastik", "Nasiya" (Qarz)
    val isPaid: Boolean = true // Nasiya bo'lsa, qarz yopildimi yoki yo'q (false - qarzdor, true - qarz uzilgan yoki oddiy savdo)
) {
    companion object {
        const val PAYMENT_CASH = "Naqd"
        const val PAYMENT_CARD = "Plastik"
        const val PAYMENT_DEBT = "Nasiya"
    }
}
