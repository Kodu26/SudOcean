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
import com.example.sudocean.data.entities.Product
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class ItemViewModel(application: Application, private val repository: MainRepository) : AndroidViewModel(application) {

    private val app = application as SudOceanApplication
    private val currentUserIdFlow = MutableStateFlow(app.currentUserId)

    // Состояния для поиска и фильтрации
    private val searchQuery = MutableStateFlow("")
    private val selectedCategory = MutableStateFlow("Все")

    // Список всех товаров из БД
    private val productsFlow = repository.allProducts

    // ОТФИЛЬТРОВАННЫЙ СПИСОК ТОВАРОВ
    val filteredProducts: LiveData<List<Product>> = combine(
        productsFlow,
        searchQuery,
        selectedCategory
    ) { products, query, category ->
        products.filter { product ->
            val matchesQuery = product.name.contains(query, ignoreCase = true) || 
                               product.description.contains(query, ignoreCase = true)
            val matchesCategory = category == "Все" || product.category == category
            matchesQuery && matchesCategory
        }
    }.asLiveData()

    // Список уникальных категорий для создания кнопок (Chips)
    val categories: LiveData<List<String>> = productsFlow.map { products ->
        listOf("Все") + products.map { it.category }.distinct().filter { it != "Без категории" }
    }.asLiveData()

    val cartItems = currentUserIdFlow.flatMapLatest { userId ->
        repository.getCartItems(userId)
    }.asLiveData()

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    init {
        refreshProducts()
    }

    fun setSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun setCategory(category: String) {
        selectedCategory.value = category
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
