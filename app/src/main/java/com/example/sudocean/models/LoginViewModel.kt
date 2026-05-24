package com.example.sudocean.models

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.sudocean.data.MainRepository
import com.example.sudocean.data.entities.User
import kotlinx.coroutines.launch

class LoginViewModel(private val repository: MainRepository) : ViewModel() {

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    fun login(type: String, identifier: String, password: String, onResult: (User?) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            try {
                val cleanId = identifier.replace(Regex("[^\\d]"), "")
                
                // 1. Сначала ищем локально (для скорости)
                var user = repository.loginByRemoteId(cleanId, password, type)
                
                if (user == null) {
                    // 2. Если локально нет (после обновления БД) — идем в 1С
                    user = repository.remoteLogin(type, identifier, password)
                }

                if (user != null) {
                    onResult(user)
                } else {
                    _errorMessage.value = "WRONG_CREDENTIALS"
                    onResult(null)
                }
            } catch (e: Exception) {
                _errorMessage.value = when {
                    e.message?.contains("NETWORK_ERROR") == true -> "NETWORK_ERROR"
                    else -> "SERVER_ERROR"
                }
                onResult(null)
            } finally {
                _isLoading.value = false
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
