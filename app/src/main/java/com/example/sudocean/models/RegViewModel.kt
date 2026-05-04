package com.example.sudocean.models

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.sudocean.data.MainRepository
import com.example.sudocean.data.entities.User

class RegViewModel(private val repository: MainRepository) : ViewModel() {

    suspend fun checkUserExists(login: String): Boolean {
        return repository.isUserExistsLocally(login)
    }

    suspend fun register(user: User): Long {
        return repository.register(user)
    }
}

class RegViewModelFactory(private val repository: MainRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RegViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return RegViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
