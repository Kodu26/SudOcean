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
import com.example.sudocean.data.entities.CartItem
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class ItemViewModel(application: Application, private val repository: MainRepository) : AndroidViewModel(application) {

    private val app = application as SudOceanApplication
    private val currentUserIdFlow = MutableStateFlow(app.currentUserId)

    val allProducts = repository.allProducts.asLiveData()
    
    val cartItems = currentUserIdFlow.flatMapLatest { userId ->
        repository.getCartItems(userId)
    }.asLiveData()

    // Состояние загрузки для SwipeRefresh
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    init {
        refreshProducts()
    }

    fun refreshProducts() {
        viewModelScope.launch {
            _isLoading.value = true
            repository.syncProducts()
            _isLoading.value = false
        }
    }

    fun addToCart(productId: Int) {
        viewModelScope.launch {
            val userId = app.currentUserId
            val existingItem = repository.getCartItems(userId).first().find { it.productId == productId }
            
            if (existingItem != null) {
                existingItem.quantity += 1
                repository.updateCartQuantity(existingItem)
            } else {
                repository.addToCart(CartItem(userId = userId, productId = productId, quantity = 1))
            }
        }
    }

    fun removeFromCart(productId: Int) {
        viewModelScope.launch {
            val userId = app.currentUserId
            val existingItem = repository.getCartItems(userId).first().find { it.productId == productId }
            
            if (existingItem != null) {
                if (existingItem.quantity > 1) {
                    existingItem.quantity -= 1
                    repository.updateCartQuantity(existingItem)
                } else {
                    repository.deleteFromCart(existingItem)
                }
            }
        }
    }
}

class ItemViewModelFactory(private val application: Application, private val repository: MainRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ItemViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ItemViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
