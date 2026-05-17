package com.example.sudocean.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.sudocean.data.entities.OrderItem
import kotlinx.coroutines.flow.Flow

@Dao
interface OrderItemDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrderItems(items: List<OrderItem>)

    @Query("SELECT * FROM order_items WHERE orderId = :orderId")
    fun getOrderItems(orderId: Int): Flow<List<OrderItem>>

    @Query("DELETE FROM order_items WHERE orderId = :orderId")
    suspend fun deleteOrderItems(orderId: Int)

    @Query("DELETE FROM order_items WHERE orderId IN (SELECT id FROM orders WHERE userId = :userId)")
    suspend fun deleteOrderItemsForUser(userId: Int)
}
