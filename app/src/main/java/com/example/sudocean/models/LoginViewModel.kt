package com.example.sudocean.models

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.sudocean.data.MainRepository
import com.example.sudocean.data.entities.User
import kotlinx.coroutines.launch

class LoginViewModel(private val repository: MainRepository) : ViewModel() {

    fun login(type: String, identifier: String, password: String, onResult: (User?) -> Unit) {
        viewModelScope.launch {
            val user = if (type == "PHYSICAL") {
                repository.loginPhysical(identifier, password)
            } else {
                repository.loginLegal(identifier, password)
            }
            onResult(user)
        }
    }
}

class LoginViewModelFactory(private val repository: MainRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LoginViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LoginViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
