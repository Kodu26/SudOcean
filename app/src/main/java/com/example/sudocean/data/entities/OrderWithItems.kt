package com.example.sudocean.data.entities

import androidx.room.Embedded
import androidx.room.Relation

data class OrderWithItems(
    @Embedded val order: Order,
    @Relation(
        parentColumn = "id",
        entityColumn = "orderId"
    )
    val items: List<OrderItem>
)
