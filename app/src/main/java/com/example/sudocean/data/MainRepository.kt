package com.example.sudocean.data

import android.util.Log
import com.example.sudocean.data.dao.CartDao
import com.example.sudocean.data.dao.OrderDao
import com.example.sudocean.data.dao.OrderItemDao
import com.example.sudocean.data.dao.ProductDao
import com.example.sudocean.data.dao.UserDao
import com.example.sudocean.data.entities.CartItem
import com.example.sudocean.data.entities.Order
import com.example.sudocean.data.entities.OrderItem
import com.example.sudocean.data.entities.OrderWithItems
import com.example.sudocean.data.entities.Product
import com.example.sudocean.data.entities.User
import com.example.sudocean.network.ApiService
import com.example.sudocean.network.OrderItemRequest
import com.example.sudocean.network.OrderRequest
import com.example.sudocean.network.UserRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Locale

class MainRepository(
    private val userDao: UserDao,
    private val productDao: ProductDao,
    private val cartDao: CartDao,
    private val orderDao: OrderDao,
    private val orderItemDao: OrderItemDao,
    private val apiService: ApiService
) {
    private val isoDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())

    // USER
    suspend fun loginPhysical(phone: String, password: String): User? = userDao.loginPhysical(phone, password)
    suspend fun loginLegal(inn: String, password: String): User? = userDao.loginLegal(inn, password)
    suspend fun register(user: User): Long {
        val id = userDao.register(user)
        syncUserWith1C(user.copy(id = id.toInt()))
        return id
    }
    suspend fun updateUser(user: User) {
        userDao.updateUser(user)
        syncUserWith1C(user)
    }
    private suspend fun syncUserWith1C(user: User) {
        try {
            val request = UserRequest(user.id.toString(), user.userType, user.fullName, user.phone, user.inn, user.kpp, user.legalAddress)
            apiService.syncUser(request)
        } catch (e: Exception) { Log.e("1C_SYNC", "User sync failed: ${e.message}") }
    }
    fun getUserByIdFlow(userId: Int): Flow<User?> = userDao.getUserById(userId)
    suspend fun getUserById(userId: Int): User? = userDao.getUserByIdDirect(userId)

    // PRODUCTS
    val allProducts: Flow<List<Product>> = productDao.getAllProducts()
    suspend fun syncProducts() {
        try {
            val response = apiService.getProducts()
            if (response.isSuccessful) {
                response.body()?.forEach { remote ->
                    productDao.insertProduct(Product(remote.id.toIntOrNull() ?: 0, remote.name, remote.description, remote.price))
                }
            }
        } catch (e: Exception) { Log.e("1C_SYNC", "Products sync error: ${e.message}") }
    }

    // CART
    fun getCartItems(userId: Int): Flow<List<CartItem>> = cartDao.getCartItems(userId)
    suspend fun addToCart(cartItem: CartItem) = cartDao.addToCart(cartItem)
    suspend fun updateCartQuantity(cartItem: CartItem) = cartDao.updateQuantity(cartItem)
    suspend fun deleteFromCart(cartItem: CartItem) = cartDao.deleteFromCart(cartItem)
    suspend fun clearCart(userId: Int) = cartDao.clearCart(userId)

    // ORDERS
    fun getUserOrders(userId: Int): Flow<List<OrderWithItems>> = orderDao.getUserOrdersWithItems(userId)
    suspend fun insertOrder(order: Order): Long = orderDao.insertOrder(order)
    suspend fun updateOrder(order: Order) = orderDao.updateOrder(order)
    suspend fun getOrderById(orderId: Int): Order? = orderDao.getOrderById(orderId)
    suspend fun insertOrderItems(items: List<OrderItem>) = orderItemDao.insertOrderItems(items)

    // Синхронизация истории
    suspend fun syncOrdersFrom1C(userId: Int) {
        try {
            val response = apiService.getUserOrders(userId.toString())
            if (response.isSuccessful) {
                val remoteOrders = response.body()
                orderDao.deleteUserOrders(userId)
                
                remoteOrders?.forEach { remoteOrder ->
                    val parsedDate = try { isoDateFormat.parse(remoteOrder.date)?.time ?: System.currentTimeMillis() } catch (e: Exception) { System.currentTimeMillis() }
                    
                    // Используем реальный ID из 1С или стабильный номер
                    val orderIdForDb = if (remoteOrder.id > 0) remoteOrder.id else remoteOrder.remote_id.filter { it.isDigit() }.toIntOrNull() ?: (1000..99999).random()
                    
                    val localOrder = Order(
                        id = orderIdForDb,
                        userId = userId,
                        date = parsedDate,
                        totalAmount = remoteOrder.total_amount,
                        status = remoteOrder.status + " (№" + remoteOrder.remote_id + ")"
                    )
                    orderDao.insertOrder(localOrder)
                    
                    orderItemDao.deleteOrderItems(localOrder.id)
                    val localItems = remoteOrder.items.map { item ->
                        OrderItem(orderId = localOrder.id, productId = item.product_id.toIntOrNull() ?: 0, productName = item.product_name, quantity = item.quantity, price = item.price)
                    }
                    orderItemDao.insertOrderItems(localItems)
                }
            }
        } catch (e: Exception) { Log.e("1C_DEBUG", "Sync error: ${e.message}") }
    }

    // ОТМЕНА ЗАКАЗА В 1С
    suspend fun cancelOrderIn1C(order: Order): Boolean {
        return try {
            val orderNumber = order.status.substringAfterLast("№", "").substringBefore(")")
            Log.d("1C_DEBUG", "Попытка отмены в 1С заказа: $orderNumber")
            
            if (orderNumber.isNotEmpty()) {
                val response = apiService.cancelOrder(orderNumber)
                if (response.isSuccessful) {
                    Log.d("1C_DEBUG", "1С подтвердила отмену заказа $orderNumber")
                    true
                } else {
                    Log.e("1C_DEBUG", "1С отклонила отмену: ${response.code()} ${response.errorBody()?.string()}")
                    false
                }
            } else false
        } catch (e: Exception) {
            Log.e("1C_DEBUG", "Network error during cancel: ${e.message}")
            false
        }
    }

    // SEND TO 1C
    suspend fun sendOrderTo1C(user: User, order: Order, cartItems: List<CartItem>, products: List<Product>): String? {
        return try {
            val itemsRequest = cartItems.map { cartItem ->
                val p = products.find { it.id == cartItem.productId }
                OrderItemRequest(cartItem.productId.toString(), cartItem.quantity.toDouble(), p?.price ?: 0.0)
            }
            val request = OrderRequest(user.id.toString(), user.fullName, user.phone, order.totalAmount, itemsRequest)
            val response = apiService.sendOrder(request)
            if (response.isSuccessful) response.body()?.order_number ?: "000" else null
        } catch (e: Exception) { null }
    }
}
