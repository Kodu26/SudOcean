package com.example.sudocean.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.sudocean.data.entities.CartItem
import com.example.sudocean.data.entities.Product
import com.example.sudocean.databinding.ItemProductBinding

class ProductAdapter(
    private val onPlusClick: (Product) -> Unit,
    private val onMinusClick: (Product) -> Unit
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
        holder.bind(product, cartItem?.quantity ?: 0)
    }

    inner class ProductViewHolder(private val binding: ItemProductBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(product: Product, quantity: Int) {
            binding.productName.text = product.name
            binding.productDescription.text = product.description
            binding.productPrice.text = "${product.price} ₽"
            binding.tvQuantity.text = quantity.toString()

            binding.btnPlus.setOnClickListener { onPlusClick(product) }
            binding.btnMinus.setOnClickListener { onMinusClick(product) }
        }
    }

    class ProductDiffCallback : DiffUtil.ItemCallback<Product>() {
        override fun areItemsTheSame(oldItem: Product, newItem: Product): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Product, newItem: Product): Boolean = oldItem == newItem
    }
}
