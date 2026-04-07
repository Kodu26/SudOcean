package com.example.sudocean.models

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.sudocean.data.MainRepository
import kotlinx.coroutines.launch

class PaymentViewModel(private val repository: MainRepository) : ViewModel() {

    fun updateOrderStatus(orderId: Long, status: String) {
        viewModelScope.launch {
            val order = repository.getOrderById(orderId.toInt())
            order?.let {
                repository.updateOrder(it.copy(status = status))
            }
        }
    }
}

class PaymentViewModelFactory(private val repository: MainRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PaymentViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PaymentViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
