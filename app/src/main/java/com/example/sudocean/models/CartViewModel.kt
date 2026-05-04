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
    val cartItemId: Int,
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
                product?.let { CartProduct(cartItem.id, it, cartItem.quantity) }
            }
        }
    }

    val cartProducts: LiveData<List<CartProduct>> = cartProductsFlow.asLiveData()

    val totalAmount: LiveData<Double> = cartProductsFlow.map { products ->
        products.sumOf { it.product.price * it.quantity }
    }.asLiveData()

    private val _checkoutData = MutableLiveData<Pair<Long, Double>?>()
    val checkoutData: LiveData<Pair<Long, Double>?> = _checkoutData

    fun increaseQuantity(cartProduct: CartProduct) {
        viewModelScope.launch {
            val item = CartItem(
                id = cartProduct.cartItemId,
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
                    id = cartProduct.cartItemId,
                    userId = app.currentUserId,
                    productId = cartProduct.product.id,
                    quantity = cartProduct.quantity - 1
                )
                repository.updateCartQuantity(item)
            } else {
                val item = CartItem(
                    id = cartProduct.cartItemId,
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
        // Мы НЕ создаем заказ в БД сразу, чтобы избежать "пустых" заказов при нажатии кнопки Назад.
        // Вместо этого мы просто сигнализируем UI, что пора переходить к оплате.
        val currentAmount = totalAmount.value ?: 0.0
        if (currentAmount > 0) {
            // Передаем -1 как ID, сигнализируя, что заказ еще не создан в базе
            _checkoutData.value = Pair(-1L, currentAmount)
        }
    }

    fun clearCheckoutData() {
        _checkoutData.value = null
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
