package com.example.sudocean.network

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.Streaming

interface ApiService {

    @GET("products")
    suspend fun getProducts(): Response<List<ProductResponse>>

    @POST("orders")
    suspend fun sendOrder(@Body order: OrderRequest): Response<OrderResponse>

    @POST("clients")
    suspend fun syncUser(@Body user: UserRequest): Response<Unit>

    @POST("clients/password")
    suspend fun changePassword(@Body request: PasswordChangeRequest): Response<Unit>

    @POST("clients/delete")
    suspend fun deleteAccount(@Query("id") id: String): Response<Unit>

    @GET("clients/auth")
    suspend fun loginRemote(
        @Query("id") id: String,
        @Query("password") password: String
    ): Response<RemoteUserResponse>

    @GET("orders")
    suspend fun getUserOrders(@Query("user_id") userId: String): Response<List<RemoteOrderResponse>>

    @POST("orders/cancel")
    suspend fun cancelOrder(@Query("order_number") orderNumber: String): Response<Unit>

    @Streaming
    @GET("orders/print")
    suspend fun downloadInvoice(@Query("number") orderNumber: String): Response<ResponseBody>
}

data class PasswordChangeRequest(
    val id: String,
    val old_password: String,
    val new_password: String
)

data class ProductResponse(
    val id: String,
    val name: String,
    val description: String,
    val price: Double,
    val imageUrl: String? = null,
    val category: String? = null,
    val stock: Int? = null
)

data class OrderRequest(
    val user_id: String,
    val user_name: String,
    val phone: String,
    val total_amount: Double,
    val items: List<OrderItemRequest>,
    val order_number: String? = null // Опциональный номер для Pay Later
)

data class OrderResponse(
    val order_number: String,
    val status: String? = null,
    val payment_link: String? = null,
    val payment_qr: String? = null
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
    val password: String,
    val inn: String? = null,
    val kpp: String? = null,
    val legal_address: String? = null,
    val is_registration: Boolean
)

data class RemoteUserResponse(
    val full_name: String,
    val phone: String,
    val user_type: String,
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
