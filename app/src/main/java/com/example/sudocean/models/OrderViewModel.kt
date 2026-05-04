package com.example.sudocean.models

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.sudocean.SudOceanApplication
import com.example.sudocean.data.MainRepository
import com.example.sudocean.data.entities.Order
import com.example.sudocean.data.entities.OrderWithItems
import com.example.sudocean.data.entities.User
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class OrderViewModel(application: Application, private val repository: MainRepository) : AndroidViewModel(application) {

    private val app = application as SudOceanApplication
    private val currentUserIdFlow = MutableStateFlow(app.currentUserId)

    val currentUser: LiveData<User?> = currentUserIdFlow.flatMapLatest { userId ->
        repository.getUserByIdFlow(userId)
    }.asLiveData()

    val userOrders: LiveData<List<OrderWithItems>> = currentUserIdFlow.flatMapLatest { userId ->
        repository.getUserOrders(userId)
    }.asLiveData()

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    init {
        refreshOrders()
    }

    fun refreshOrders() {
        viewModelScope.launch {
            val userId = app.currentUserId
            if (userId != -1) {
                _isLoading.value = true
                repository.syncOrdersFrom1C(userId)
                repository.syncProducts() // Синхронизируем товары тоже
                _isLoading.value = false
            }
        }
    }

    fun cancelOrder(order: Order) {
        viewModelScope.launch {
            _isLoading.value = true
            
            // 1. Отменяем в 1С. 
            // Предполагается, что на стороне 1С при получении этого запроса товары автоматически возвращаются на склад.
            val isSuccess = repository.cancelOrderIn1C(order)
            
            if (isSuccess) {
                // 2. Вместо локального изменения returnItemsToStock, 
                // запрашиваем актуальные остатки и статусы из 1С
                repository.syncOrdersFrom1C(app.currentUserId)
                repository.syncProducts()
            }
            _isLoading.value = false
        }
    }
}

class OrderViewModelFactory(private val application: Application, private val repository: MainRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(OrderViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return OrderViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
