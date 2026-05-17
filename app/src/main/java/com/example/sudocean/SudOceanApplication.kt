package com.example.sudocean

import android.app.Application
import android.content.Context
import com.example.sudocean.data.AppDatabase
import com.example.sudocean.data.MainRepository
import com.example.sudocean.network.NetworkClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SudOceanApplication : Application() {
    // Инициализируем базу данных лениво
    val database by lazy { AppDatabase.getDatabase(this) }
    
    // Инициализируем сетевой сервис
    val apiService by lazy { NetworkClient.apiService }
    
    // Инициализируем репозиторий, передавая в него все DAO и Сеть
    val repository by lazy { 
        MainRepository(
            database.userDao(),
            database.productDao(), 
            database.cartDao(), 
            database.orderDao(),
            database.orderItemDao(),
            apiService
        ) 
    }

    // Храним ID текущего вошедшего пользователя
    var currentUserId: Int = -1

    override fun onCreate() {
        super.onCreate()
        
        // Очистка старых аккаунтов (инструмент для разработки/исправления базы)
        // Разово очищаем таблицу пользователей, чтобы гарантировать синхронность с 1С
        val sharedPref = getSharedPreferences("SudOceanPrefs", Context.MODE_PRIVATE)
        val isDbCleaned = sharedPref.getBoolean("db_cleaned_v1", false)
        if (!isDbCleaned) {
            CoroutineScope(Dispatchers.IO).launch {
                database.userDao().deleteAllUsers()
                clearUserSession()
                sharedPref.edit().putBoolean("db_cleaned_v1", true).apply()
            }
        }

        // Загружаем сохраненный ID пользователя при запуске
        currentUserId = sharedPref.getInt("current_user_id", -1)
    }

    fun saveUserSession(userId: Int) {
        currentUserId = userId
        val sharedPref = getSharedPreferences("SudOceanPrefs", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putInt("current_user_id", userId)
            apply()
        }
    }

    fun clearUserSession() {
        currentUserId = -1
        val sharedPref = getSharedPreferences("SudOceanPrefs", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            remove("current_user_id")
            apply()
        }
    }
}
