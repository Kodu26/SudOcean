package com.example.sudocean.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "orders")
data class Order(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: Int,
    val date: Long, // Храним дату в миллисекундах
    val totalAmount: Double,
    val status: String // "В процессе", "Завершено", "Отменено"
)
