package com.example.sudocean.models

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.sudocean.data.MainRepository
import com.example.sudocean.data.entities.User
import kotlinx.coroutines.launch

class RegViewModel(private val repository: MainRepository) : ViewModel() {

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
