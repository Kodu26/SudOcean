package com.example.sudocean.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface ApiService {

    @GET("products")
    suspend fun getProducts(): Response<List<ProductResponse>>

    @POST("orders")
    suspend fun sendOrder(@Body order: OrderRequest): Response<OrderResponse>

    @POST("clients")
    suspend fun syncUser(@Body user: UserRequest): Response<Unit>

    @GET("orders")
    suspend fun getUserOrders(@Query("user_id") userId: String): Response<List<RemoteOrderResponse>>

    // НОВОЕ: Отмена заказа в 1С
    @POST("orders/cancel")
    suspend fun cancelOrder(@Query("order_number") orderNumber: String): Response<Unit>
}

data class ProductResponse(
    val id: String,
    val name: String,
    val description: String,
    val price: Double
)

data class OrderRequest(
    val user_id: String,
    val user_name: String,
    val phone: String,
    val total_amount: Double,
    val items: List<OrderItemRequest>
)

data class OrderResponse(
    val order_number: String
)

data class OrderItemRequest(
    val product_id: String,
    val quantity: Double,
    val price: Double
)

data class UserRequest(
    val id: String,
    val user_type: String,
    val full_name: String,
    val phone: String,
    val inn: String? = null,
    val kpp: String? = null,
    val legal_address: String? = null
)

data class RemoteOrderResponse(
    val id: Int,
    val remote_id: String,
    val date: String,
    val total_amount: Double,
    val status: String,
    val items: List<OrderItemResponse>
)

data class OrderItemResponse(
    val product_id: String,
    val product_name: String,
    val quantity: Int,
    val price: Double
)
