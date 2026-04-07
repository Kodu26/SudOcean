package com.example.sudocean.network

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object NetworkClient {

    // актуальный IP адрес пк
    private const val BASE_URL = "http://192.168.0.101/SudOcean/hs/v1/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val original = chain.request()
            // Настройка авторизации 1С (Basic Auth) - нужно зашифровать
            val requestBuilder = original.newBuilder()
                .header("Authorization", okhttp3.Credentials.basic("Admin", "123")) 
                .method(original.method(), original.body())
            val request = requestBuilder.build()
            chain.proceed(request)
        }
        .build()

    val apiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
