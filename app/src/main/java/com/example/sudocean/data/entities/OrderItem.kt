package com.example.sudocean.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "order_items")
data class OrderItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val orderId: Int,
    val productId: Int,
    val productName: String, // Сохраняем имя на случай, если товар удалят из каталога
    val quantity: Int,
    val price: Double
)
