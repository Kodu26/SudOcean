package com.example.sudocean.data

import android.content.ActivityNotFoundException
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
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
import com.example.sudocean.network.OrderResponse
import com.example.sudocean.network.PasswordChangeRequest
import com.example.sudocean.network.UserRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import java.io.IOException
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
    private val syncMutex = Mutex()

    private fun getRemoteId(user: User): String {
        return user.remoteId.replace(Regex("[^\\d]"), "")
    }

    private fun normalizeId(identifier: String): String {
        val clean = identifier.replace(Regex("[^\\d]"), "")
        return if (clean.length == 11 && clean.startsWith("8")) {
            "7" + clean.substring(1)
        } else {
            clean
        }
    }

    // USER logic
    suspend fun isUserExistsLocally(login: String): Boolean {
        val cleanLogin = normalizeId(login)
        return userDao.getUserByLogin(cleanLogin) != null
    }

    suspend fun loginByRemoteId(remoteId: String, password: String, type: String): User? {
        val normalizedId = normalizeId(remoteId)
        return userDao.loginByRemoteId(normalizedId, password, type)
    }

    suspend fun remoteLogin(type: String, identifier: String, password: String): User? {
        return withContext(Dispatchers.IO) {
            try {
                val cleanId = normalizeId(identifier)
                val response = apiService.loginRemote(cleanId, password)
                
                if (response.isSuccessful) {
                    val remoteUser = response.body() ?: return@withContext null
                    
                    val normalizedType = when(remoteUser.user_type.uppercase()) {
                        "PHYSICAL", "INDIVIDUAL", "ФИЗЛИЦО", "ОБЫЧНЫЙ", "ФИЗ. ЛИЦО" -> "PHYSICAL"
                        "LEGAL", "COMPANY", "ЮРЛИЦО", "БИЗНЕС", "ЮР. ЛИЦО" -> "LEGAL"
                        else -> type
                    }
                    
                    val newUser = User(
                        remoteId = cleanId,
                        userType = normalizedType,
                        fullName = remoteUser.full_name,
                        phone = remoteUser.phone,
                        password = password,
                        inn = remoteUser.inn,
                        kpp = remoteUser.kpp,
                        legalAddress = remoteUser.legal_address,
                        legalForm = remoteUser.form
                    )
                    
                    val existing = userDao.getUserByLogin(cleanId)
                    return@withContext if (existing != null) {
                        val updated = newUser.copy(id = existing.id)
                        userDao.updateUser(updated)
                        updated
                    } else {
                        val id = userDao.register(newUser)
                        newUser.copy(id = id.toInt())
                    }
                } else if (response.code() == 401 || response.code() == 404) {
                    return@withContext null
                } else {
                    throw Exception("SERVER_ERROR")
                }
            } catch (e: IOException) {
                throw Exception("NETWORK_ERROR")
            } catch (e: Exception) {
                Log.e("1C_AUTH", "Remote login error: ${e.message}")
                throw e
            }
        }
    }

    suspend fun verifyUserRemote(user: User): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val remoteId = getRemoteId(user)
                val response = apiService.getUserOrders(remoteId)
                if (response.isSuccessful) return@withContext true
                if (response.code() == 404 || response.code() == 401) {
                    userDao.deleteUser(user)
                    return@withContext false
                }
                false
            } catch (e: Exception) {
                false
            }
        }
    }

    suspend fun register(user: User): Long {
        syncUserWith1C(user, isRegistration = true)
        val existingUser = userDao.getUserByLogin(user.remoteId)
        return if (existingUser != null) {
            val updatedUser = user.copy(id = existingUser.id)
            userDao.updateUser(updatedUser)
            existingUser.id.toLong()
        } else {
            userDao.register(user)
        }
    }

    suspend fun updateUser(user: User) {
        syncUserWith1C(user, isRegistration = false)
        val cleanPhone = user.phone.replace(Regex("[^\\d]"), "")
        val newRemoteId = if (user.userType == "LEGAL") (user.inn ?: user.remoteId) else cleanPhone
        val updatedUser = user.copy(remoteId = newRemoteId)
        userDao.updateUser(updatedUser)
    }

    suspend fun changePassword(user: User, oldPass: String, newPass: String) {
        withContext(Dispatchers.IO) {
            val remoteId = getRemoteId(user)
            val request = PasswordChangeRequest(id = remoteId, old_password = oldPass, new_password = newPass)
            try {
                val response = apiService.changePassword(request)
                if (response.isSuccessful) {
                    val updatedUser = user.copy(password = newPass)
                    userDao.updateUser(updatedUser)
                } else {
                    throw Exception(response.errorBody()?.string() ?: "Ошибка смены пароля")
                }
            } catch (e: Exception) { throw e }
        }
    }

    suspend fun deleteAccount(user: User) {
        withContext(Dispatchers.IO) {
            val remoteId = getRemoteId(user)
            try {
                val response = apiService.deleteAccount(remoteId)
                if (response.isSuccessful || response.code() == 404) {
                    userDao.deleteUser(user)
                } else {
                    throw Exception("Ошибка удаления аккаунта")
                }
            } catch (e: Exception) { throw e }
        }
    }

    private suspend fun syncUserWith1C(user: User, isRegistration: Boolean) {
        withContext(Dispatchers.IO) {
            val request = UserRequest(
                id = getRemoteId(user),
                user_type = user.userType,
                full_name = user.fullName,
                phone = user.phone,
                password = user.password,
                inn = user.inn,
                kpp = user.kpp,
                legal_address = user.legalAddress,
                form = user.legalForm,
                is_registration = isRegistration
            )
            val response = apiService.syncUser(request)
            if (!response.isSuccessful) {
                if (response.code() == 409) {
                    throw Exception("Этот телефон/инн уже зарегистрирован")
                }
                val errorMsg = response.errorBody()?.string() ?: "Неизвестная ошибка 1С"
                throw Exception(errorMsg)
            }
        }
    }

    fun getUserByIdFlow(userId: Int): Flow<User?> = userDao.getUserById(userId)
    suspend fun getUserById(userId: Int): User? = userDao.getUserByIdDirect(userId)

    // PRODUCTS
    val allProducts: Flow<List<Product>> = productDao.getAllProducts()
    suspend fun getProductById(productId: Int): Product? = productDao.getProductById(productId)

    suspend fun syncProducts() {
        withContext(Dispatchers.IO) {
            try {
                val response = apiService.getProducts()
                if (response.isSuccessful) {
                    val remoteProducts = response.body() ?: emptyList()
                    val products = remoteProducts.map { remote ->
                        Product(
                            id = remote.id.filter { it.isDigit() }.toIntOrNull() ?: 0,
                            name = remote.name,
                            description = remote.description,
                            price = remote.price,
                            imageUrl = remote.imageUrl?.takeIf { it.isNotBlank() && it != "null" },
                            category = remote.category ?: "Без категории",
                            stock = remote.stock ?: 0
                        )
                    }
                    
                    // 1. Вставляем/обновляем полученные товары
                    productDao.insertProducts(products)
                    
                    // 2. Определяем список актуальных ID
                    val currentIds = products.map { it.id }
                    
                    // 3. Удаляем товары, которых больше нет в 1С
                    productDao.deleteProductsNotInList(currentIds)
                    
                    // 4. Очищаем корзину от товаров, которых больше нет
                    cartDao.deleteOrphanedItems(currentIds)
                    
                } else {
                    throw Exception("1C_UNAVAILABLE")
                }
            } catch (e: IOException) {
                throw Exception("NO_INTERNET")
            } catch (e: Exception) { 
                Log.e("1C_SYNC", "Products sync error: ${e.message}")
                throw Exception("1C_UNAVAILABLE")
            }
        }
    }

    // CART logic
    fun getCartItems(userId: Int): Flow<List<CartItem>> = cartDao.getCartItems(userId)
    
    suspend fun addToCart(cartItem: CartItem) {
        val product = productDao.getProductById(cartItem.productId)
        if (product != null && cartItem.quantity <= product.stock) {
            cartDao.addToCart(cartItem)
        }
    }

    suspend fun updateCartQuantity(cartItem: CartItem) {
        val product = productDao.getProductById(cartItem.productId)
        if (product != null && cartItem.quantity <= product.stock) {
            cartDao.updateQuantity(cartItem)
        }
    }

    suspend fun deleteFromCart(cartItem: CartItem) = cartDao.deleteFromCart(cartItem)
    suspend fun clearCart(userId: Int) = cartDao.clearCart(userId)

    // ORDERS
    fun getUserOrders(userId: Int): Flow<List<OrderWithItems>> = orderDao.getUserOrdersWithItems(userId)
    suspend fun insertOrder(order: Order): Long = orderDao.insertOrder(order)
    suspend fun updateOrder(order: Order) = orderDao.updateOrder(order)
    suspend fun getOrderById(orderId: Int): Order? = orderDao.getOrderById(orderId)
    suspend fun insertOrderItems(items: List<OrderItem>) = orderItemDao.insertOrderItems(items)

    suspend fun syncOrdersFrom1C(userId: Int) {
        syncMutex.withLock {
            try {
                val user = userDao.getUserByIdDirect(userId) ?: return@withLock
                val remoteUserId = getRemoteId(user)
                val response = apiService.getUserOrders(remoteUserId)
                if (response.isSuccessful) {
                    val remoteOrders = response.body() ?: return@withLock
                    orderDao.deleteUserOrders(userId)
                    remoteOrders.forEach { remoteOrder ->
                        val parsedDate = try { isoDateFormat.parse(remoteOrder.date)?.time ?: System.currentTimeMillis() } catch (e: Exception) { System.currentTimeMillis() }
                        val localId = remoteOrder.remote_id.filter { it.isDigit() }.toIntOrNull() ?: (1000..99999).random()
                        val localOrder = Order(
                            id = localId, 
                            userId = userId, 
                            date = parsedDate, 
                            totalAmount = remoteOrder.total_amount, 
                            status = remoteOrder.status + " (№" + remoteOrder.remote_id + ")"
                        )
                        orderDao.insertOrder(localOrder)
                        val localItems = remoteOrder.items.map { item ->
                            OrderItem(
                                orderId = localId, 
                                productId = item.product_id.toIntOrNull() ?: 0, 
                                productName = item.product_name, 
                                quantity = item.quantity, 
                                price = item.price
                            )
                        }
                        orderItemDao.insertOrderItems(localItems)
                    }
                }
            } catch (e: Exception) { 
                Log.e("1C_DEBUG", "Sync error: ${e.message}") 
            }
        }
    }

    suspend fun downloadAndOpenInvoice(context: Context, order: Order) {
        withContext(Dispatchers.IO) {
            try {
                val orderNumber = order.status.substringAfterLast("№", "").substringBefore(")")
                if (orderNumber.isEmpty()) return@withContext
                val response = apiService.downloadInvoice(orderNumber)
                if (response.isSuccessful) {
                    val body = response.body() ?: return@withContext
                    val fileName = "Invoice_${orderNumber}_${System.currentTimeMillis()}.pdf"
                    val uri = saveFileToDownloads(context, body, fileName)
                    if (uri != null) { 
                        withContext(Dispatchers.Main) { openPdf(context, uri) } 
                    }
                }
            } catch (e: Exception) { 
                Log.e("1C_DOWNLOAD", "Critical download error: ${e.message}")
            }
        }
    }

    private fun saveFileToDownloads(context: Context, body: ResponseBody, fileName: String): Uri? {
        val resolver = context.contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
        }
        return try {
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            if (uri == null) return null
            resolver.openOutputStream(uri)?.use { outputStream ->
                body.byteStream().use { inputStream -> 
                    inputStream.copyTo(outputStream)
                }
            }
            uri
        } catch (e: Exception) { null }
    }

    private fun openPdf(context: Context, uri: Uri) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "PDF-счет не удалось открыть.", Toast.LENGTH_LONG).show()
        }
    }

    suspend fun cancelOrderIn1C(order: Order): Boolean {
        return try {
            val orderNumber = order.status.substringAfterLast("№", "").substringBefore(")")
            if (orderNumber.isNotEmpty()) {
                val response = apiService.cancelOrder(orderNumber)
                response.isSuccessful
            } else { false }
        } catch (e: Exception) { false }
    }

    suspend fun sendOrderTo1C(
        user: User, 
        order: Order, 
        cartItems: List<CartItem>, 
        products: List<Product>,
        orderNumber: String? = null
    ): OrderResponse? {
        val itemsRequest = cartItems.map { cartItem ->
            val p = products.find { it.id == cartItem.productId }
            OrderItemRequest(cartItem.productId.toString(), cartItem.quantity.toDouble(), p?.price ?: 0.0)
        }
        val request = OrderRequest(
            user_id = getRemoteId(user),
            user_name = user.fullName,
            phone = user.phone,
            total_amount = order.totalAmount,
            items = itemsRequest,
            order_number = orderNumber
        )
        
        return try {
            val response = apiService.sendOrder(request)
            if (response.isSuccessful) {
                response.body()
            } else {
                throw Exception("1C_UNAVAILABLE")
            }
        } catch (e: IOException) {
            throw Exception("NO_NETWORK")
        } catch (e: Exception) {
            throw e
        }
    }
}
