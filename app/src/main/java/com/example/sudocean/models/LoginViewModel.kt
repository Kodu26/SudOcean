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
            // 1. Проверяем локально
            val localUser = if (type == "PHYSICAL") {
                repository.loginPhysical(identifier, password)
            } else {
                repository.loginLegal(identifier, password)
            }

            if (localUser != null) {
                // 2. Если есть локально, проверяем актуальность в 1С
                val isStillExistsIn1C = repository.verifyUserRemote(localUser)
                if (isStillExistsIn1C) {
                    onResult(localUser)
                } else {
                    onResult(null)
                }
            } else {
                // 3. НОВОЕ: Если локально не найден, пробуем авторизоваться через 1С
                val remoteUser = repository.remoteLogin(type, identifier, password)
                onResult(remoteUser)
            }
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
