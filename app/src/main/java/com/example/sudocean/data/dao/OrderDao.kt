package com.example.sudocean.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.sudocean.data.entities.Order
import com.example.sudocean.data.entities.OrderWithItems
import kotlinx.coroutines.flow.Flow

@Dao
interface OrderDao {
    @Transaction
    @Query("SELECT * FROM orders WHERE userId = :userId ORDER BY date DESC")
    fun getUserOrdersWithItems(userId: Int): Flow<List<OrderWithItems>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: Order): Long

    @Update
    suspend fun updateOrder(order: Order)

    @Query("SELECT * FROM orders WHERE id = :orderId")
    suspend fun getOrderById(orderId: Int): Order?

    @Query("DELETE FROM orders WHERE userId = :userId")
    suspend fun deleteUserOrders(userId: Int)

    // Метод для полной очистки заказов и товаров пользователя перед синхронизацией
    @Transaction
    suspend fun clearAllUserOrderData(userId: Int, orderItemDao: OrderItemDao) {
        orderItemDao.deleteOrderItemsForUser(userId)
        deleteUserOrders(userId)
    }
}
