package com.example.sudocean.ui.adapters

import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.sudocean.R
import com.example.sudocean.data.entities.CartItem
import com.example.sudocean.data.entities.Product
import com.example.sudocean.databinding.ItemProductBinding
import java.util.Locale

class ProductAdapter(
    private val onPlusClick: (Product) -> Unit,
    private val onMinusClick: (Product) -> Unit,
    private val onItemClick: (Product) -> Unit
) : ListAdapter<Product, ProductAdapter.ProductViewHolder>(ProductDiffCallback()) {

    private var cartItems: List<CartItem> = emptyList()

    fun updateCart(newCartItems: List<CartItem>) {
        cartItems = newCartItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val binding = ItemProductBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ProductViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val product = getItem(position)
        val cartItem = cartItems.find { it.productId == product.id }
        holder.bind(product, cartItem?.quantity ?: 0, onPlusClick, onMinusClick, onItemClick)
    }

    class ProductViewHolder(private val binding: ItemProductBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(product: Product, quantity: Int, onPlus: (Product) -> Unit, onMinus: (Product) -> Unit, onClick: (Product) -> Unit) {
            binding.productName.text = product.name
            binding.productDescription.text = product.description
            binding.productPrice.text = String.format(Locale.getDefault(), "%.2f ₽", product.price)
            
            // ЛОГИКА ОСТАТКОВ
            if (product.stock > 0) {
                binding.tvStockStatus.text = "В наличии: ${product.stock} шт."
                binding.tvStockStatus.setTextColor(binding.root.context.getColor(R.color.green))
                binding.btnAddFirst.isEnabled = true
                // Блокируем плюс, если в корзине уже максимум
                binding.btnPlus.isEnabled = quantity < product.stock
                binding.btnPlus.alpha = if (quantity < product.stock) 1.0f else 0.5f
            } else {
                binding.tvStockStatus.text = "Нет в наличии"
                binding.tvStockStatus.setTextColor(binding.root.context.getColor(R.color.signal_red))
                binding.btnAddFirst.isEnabled = false
                binding.btnPlus.isEnabled = false
            }

            // Адаптивная логика кнопок
            if (quantity > 0) {
                binding.btnAddFirst.visibility = View.GONE
                binding.layoutQuantityItem.visibility = View.VISIBLE
                binding.tvQuantity.text = quantity.toString()
            } else {
                binding.btnAddFirst.visibility = View.VISIBLE
                binding.layoutQuantityItem.visibility = View.GONE
            }

            // Картинка
            if (!product.imageUrl.isNullOrEmpty()) {
                try {
                    val cleanBase64 = if (product.imageUrl.contains(",")) product.imageUrl.substringAfter(",") else product.imageUrl
                    val imageBytes = Base64.decode(cleanBase64, Base64.DEFAULT)
                    val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                    binding.productImage.load(bitmap)
                    binding.productImage.imageTintList = null
                } catch (e: Exception) {
                    binding.productImage.setImageResource(R.drawable.ic_history)
                }
            } else {
                binding.productImage.setImageResource(R.drawable.ic_history)
            }

            binding.btnAddFirst.setOnClickListener { onPlus(product) }
            binding.btnPlus.setOnClickListener { onPlus(product) }
            binding.btnMinus.setOnClickListener { onMinus(product) }
            binding.root.setOnClickListener { onClick(product) }
        }
    }

    class ProductDiffCallback : DiffUtil.ItemCallback<Product>() {
        override fun areItemsTheSame(oldItem: Product, newItem: Product): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Product, newItem: Product): Boolean = oldItem == newItem
    }
}
