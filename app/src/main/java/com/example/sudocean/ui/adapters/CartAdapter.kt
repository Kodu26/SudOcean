package com.example.sudocean.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.sudocean.databinding.ItemCartBinding
import com.example.sudocean.models.CartProduct
import java.util.Locale

class CartAdapter(
    private val onPlusClick: (CartProduct) -> Unit,
    private val onMinusClick: (CartProduct) -> Unit
) : RecyclerView.Adapter<CartAdapter.CartViewHolder>() {

    private var items: List<CartProduct> = emptyList()

    fun submitList(newList: List<CartProduct>) {
        items = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartViewHolder {
        val binding = ItemCartBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CartViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CartViewHolder, position: Int) {
        holder.bind(items[position], onPlusClick, onMinusClick)
    }

    override fun getItemCount(): Int = items.size

    class CartViewHolder(private val binding: ItemCartBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(cartProduct: CartProduct, onPlusClick: (CartProduct) -> Unit, onMinusClick: (CartProduct) -> Unit) {
            binding.tvCartItemName.text = cartProduct.product.name
            binding.tvCartItemPrice.text = String.format(Locale.getDefault(), "%.2f ₽", cartProduct.product.price)
            binding.tvCartItemQuantity.text = cartProduct.quantity.toString()

            binding.btnPlus.setOnClickListener { onPlusClick(cartProduct) }
            binding.btnMinus.setOnClickListener { onMinusClick(cartProduct) }
        }
    }
}
