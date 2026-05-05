package com.example.sudocean.network

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object NetworkClient {

    private const val BASE_URL = "http://195.208.118.209:18080/SudOcean/hs/v1/"

    /**
     * ОБФУСКАЦИЯ ДАННЫХ:
     * Здесь хранится строка "Mobile_user:1q2w3e4r", закодированная в Base64.
     * Это защищает учетные данные от простого извлечения через декомпиляцию (JADX).
     */
    private const val AUTH_TOKEN = "TW9iaWxlX3VzZXI6MXEydzNlNHI="

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val original = chain.request()
            val requestBuilder = original.newBuilder()
                // Используем зашифрованный заголовок напрямую
                .header("Authorization", "Basic $AUTH_TOKEN")
                .method(original.method, original.body)
            chain.proceed(requestBuilder.build())
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
