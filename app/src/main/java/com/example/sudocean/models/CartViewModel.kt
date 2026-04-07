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
import com.example.sudocean.data.entities.Order
import com.example.sudocean.data.entities.Product
import com.example.sudocean.data.entities.User
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

data class CartProduct(
    val product: Product,
    val quantity: Int
)

@OptIn(ExperimentalCoroutinesApi::class)
class CartViewModel(application: Application, private val repository: MainRepository) : AndroidViewModel(application) {

    private val app = application as SudOceanApplication
    private val currentUserIdFlow = MutableStateFlow(app.currentUserId)

    val currentUser: LiveData<User?> = currentUserIdFlow.flatMapLatest { userId ->
        repository.getUserByIdFlow(userId)
    }.asLiveData()

    private val cartProductsFlow = currentUserIdFlow.flatMapLatest { userId ->
        repository.getCartItems(userId).combine(repository.allProducts) { cartItems, allProducts ->
            cartItems.mapNotNull { cartItem ->
                val product = allProducts.find { it.id == cartItem.productId }
                product?.let { CartProduct(it, cartItem.quantity) }
            }
        }
    }

    val cartProducts: LiveData<List<CartProduct>> = cartProductsFlow.asLiveData()

    val totalAmount: LiveData<Double> = cartProductsFlow.map { products ->
        products.sumOf { it.product.price * it.quantity }
    }.asLiveData()

    private val _lastOrderId = MutableLiveData<Long?>()
    val lastOrderId: LiveData<Long?> = _lastOrderId

    fun increaseQuantity(cartProduct: CartProduct) {
        viewModelScope.launch {
            val item = CartItem(
                userId = app.currentUserId,
                productId = cartProduct.product.id,
                quantity = cartProduct.quantity + 1
            )
            repository.updateCartQuantity(item)
        }
    }

    fun decreaseQuantity(cartProduct: CartProduct) {
        viewModelScope.launch {
            if (cartProduct.quantity > 1) {
                val item = CartItem(
                    userId = app.currentUserId,
                    productId = cartProduct.product.id,
                    quantity = cartProduct.quantity - 1
                )
                repository.updateCartQuantity(item)
            } else {
                val item = CartItem(
                    userId = app.currentUserId,
                    productId = cartProduct.product.id,
                    quantity = cartProduct.quantity
                )
                repository.deleteFromCart(item)
            }
        }
    }

    fun clearAll() {
        viewModelScope.launch {
            repository.clearCart(app.currentUserId)
        }
    }

    fun checkout() {
        viewModelScope.launch {
            val userId = app.currentUserId
            val currentAmount = totalAmount.value ?: 0.0
            
            if (currentAmount > 0) {
                val order = Order(
                    userId = userId,
                    date = System.currentTimeMillis(),
                    totalAmount = currentAmount,
                    status = "В процессе"
                )
                val id = repository.insertOrder(order)
                _lastOrderId.value = id
            }
        }
    }

    fun clearLastOrderId() {
        _lastOrderId.value = null
    }
}

class CartViewModelFactory(private val application: Application, private val repository: MainRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CartViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CartViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
