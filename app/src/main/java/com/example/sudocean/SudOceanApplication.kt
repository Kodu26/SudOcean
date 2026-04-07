package com.example.sudocean

import android.app.Application
import android.content.Context
import com.example.sudocean.data.AppDatabase
import com.example.sudocean.data.MainRepository
import com.example.sudocean.network.NetworkClient

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
            database.orderItemDao(), // ДОБАВЛЕНО
            apiService
        ) 
    }

    // Храним ID текущего вошедшего пользователя
    var currentUserId: Int = -1

    override fun onCreate() {
        super.onCreate()
        // Загружаем сохраненный ID пользователя при запуске
        val sharedPref = getSharedPreferences("SudOceanPrefs", Context.MODE_PRIVATE)
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
