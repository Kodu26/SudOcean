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
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
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

    private fun getRemoteId(user: User): String {
        return user.remoteId.replace(Regex("[^\\d]"), "")
    }

    // USER logic
    suspend fun loginPhysical(phone: String, password: String): User? = userDao.loginPhysical(phone, password)
    suspend fun loginLegal(inn: String, password: String): User? = userDao.loginLegal(inn, password)

    suspend fun isUserExistsLocally(login: String): Boolean {
        val cleanLogin = login.replace(Regex("[^\\d]"), "")
        return userDao.getUserByLogin(cleanLogin) != null
    }

    suspend fun remoteLogin(type: String, identifier: String, password: String): User? {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.loginRemote(identifier, password)
                if (response.isSuccessful) {
                    val remoteUser = response.body() ?: return@withContext null
                    val cleanIdentifier = identifier.replace(Regex("[^\\d]"), "")
                    
                    val newUser = User(
                        remoteId = cleanIdentifier,
                        userType = remoteUser.user_type,
                        fullName = remoteUser.full_name,
                        phone = remoteUser.phone,
                        password = password,
                        inn = remoteUser.inn,
                        kpp = remoteUser.kpp,
                        legalAddress = remoteUser.legal_address
                    )
                    val id = userDao.register(newUser)
                    return@withContext newUser.copy(id = id.toInt())
                }
                null
            } catch (e: Exception) {
                Log.e("1C_AUTH", "Remote login error: ${e.message}")
                null
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
        val id = userDao.register(user)
        return id
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
        try {
            val response = apiService.getProducts()
            if (response.isSuccessful) {
                response.body()?.forEach { remote ->
                    val product = Product(
                        id = remote.id.toIntOrNull() ?: 0,
                        name = remote.name,
                        description = remote.description,
                        price = remote.price,
                        imageUrl = remote.imageUrl,
                        category = remote.category ?: "Без категории",
                        stock = remote.stock ?: 0
                    )
                    productDao.insertProduct(product)
                }
            }
        } catch (e: Exception) { Log.e("1C_SYNC", "Products sync error: ${e.message}") }
    }

    // CART logic
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

    suspend fun syncOrdersFrom1C(userId: Int) {
        try {
            val user = userDao.getUserByIdDirect(userId) ?: return
            val remoteUserId = getRemoteId(user)
            val response = apiService.getUserOrders(remoteUserId)
            if (response.isSuccessful) {
                val remoteOrders = response.body()
                orderDao.deleteUserOrders(userId)
                remoteOrders?.forEach { remoteOrder ->
                    val parsedDate = try { isoDateFormat.parse(remoteOrder.date)?.time ?: System.currentTimeMillis() } catch (e: Exception) { System.currentTimeMillis() }
                    val localId = remoteOrder.remote_id.filter { it.isDigit() }.toIntOrNull() ?: (1000..99999).random()
                    val localOrder = Order(id = localId, userId = userId, date = parsedDate, totalAmount = remoteOrder.total_amount, status = remoteOrder.status + " (№" + remoteOrder.remote_id + ")")
                    orderDao.insertOrder(localOrder)
                    val localItems = remoteOrder.items.map { item ->
                        OrderItem(orderId = localOrder.id, productId = item.product_id.toIntOrNull() ?: 0, productName = item.product_name, quantity = item.quantity, price = item.price)
                    }
                    orderItemDao.insertOrderItems(localItems)
                }
            }
        } catch (e: Exception) { Log.e("1C_DEBUG", "Sync error: ${e.message}") }
    }

    suspend fun downloadAndOpenInvoice(context: Context, order: Order) {
        withContext(Dispatchers.IO) {
            try {
                Log.d("1C_DOWNLOAD", "Starting download for order status: ${order.status}")
                val orderNumber = order.status.substringAfterLast("№", "").substringBefore(")")
                Log.d("1C_DOWNLOAD", "Extracted order number: '$orderNumber'")
                
                if (orderNumber.isEmpty()) {
                    Log.e("1C_DOWNLOAD", "Order number is empty, cannot download.")
                    return@withContext
                }
                
                val response = apiService.downloadInvoice(orderNumber)
                Log.d("1C_DOWNLOAD", "Response code: ${response.code()}")
                
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body == null) {
                        Log.e("1C_DOWNLOAD", "Response body is null")
                        return@withContext
                    }
                    
                    val fileName = "Invoice_${orderNumber}_${System.currentTimeMillis()}.pdf"
                    Log.d("1C_DOWNLOAD", "Saving file: $fileName")
                    
                    val uri = saveFileToDownloads(context, body, fileName)
                    if (uri != null) { 
                        Log.d("1C_DOWNLOAD", "File saved successfully at: $uri")
                        withContext(Dispatchers.Main) { openPdf(context, uri) } 
                    } else {
                        Log.e("1C_DOWNLOAD", "Failed to save file to downloads")
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e("1C_DOWNLOAD", "Server returned error: $errorBody")
                }
            } catch (e: Exception) { 
                Log.e("1C_DOWNLOAD", "Critical download error: ${e.message}", e)
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
            if (uri == null) {
                Log.e("1C_DOWNLOAD", "ContentResolver.insert returned null")
                return null
            }
            resolver.openOutputStream(uri)?.use { outputStream ->
                body.byteStream().use { inputStream -> 
                    val bytesCopied = inputStream.copyTo(outputStream)
                    Log.d("1C_DOWNLOAD", "Bytes copied: $bytesCopied")
                }
            }
            uri
        } catch (e: Exception) { 
            Log.e("1C_DOWNLOAD", "Error in saveFileToDownloads: ${e.message}")
            null 
        }
    }

    private fun openPdf(context: Context, uri: Uri) {
        try {
            Log.d("1C_DOWNLOAD", "Attempting to open PDF: $uri")
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Log.e("1C_DOWNLOAD", "No PDF viewer found")
            Toast.makeText(context, "Установите PDF-просмотрщик", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Log.e("1C_DOWNLOAD", "Error opening PDF: ${e.message}")
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

    suspend fun sendOrderTo1C(user: User, order: Order, cartItems: List<CartItem>, products: List<Product>): OrderResponse? {
        val itemsRequest = cartItems.map { cartItem ->
            val p = products.find { it.id == cartItem.productId }
            OrderItemRequest(cartItem.productId.toString(), cartItem.quantity.toDouble(), p?.price ?: 0.0)
        }
        val request = OrderRequest(getRemoteId(user), user.fullName, user.phone, order.totalAmount, itemsRequest)
        
        val response = apiService.sendOrder(request)
        
        if (response.isSuccessful) {
            return response.body()
        } else {
            val errorMsg = response.errorBody()?.string() ?: "Ошибка на стороне 1С"
            throw Exception(errorMsg)
        }
    }
}
