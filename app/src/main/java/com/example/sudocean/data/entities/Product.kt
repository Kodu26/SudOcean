package com.example.sudocean.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class Product(
    @PrimaryKey val id: Int, // Используем ID из 1С как основной ключ
    val name: String,
    val description: String,
    val price: Double,
    val imageUrl: String? = null,
    val category: String = "Без категории",
    val stock: Int = 0
)
