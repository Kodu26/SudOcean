package com.example.sudocean.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.sudocean.R
import com.example.sudocean.data.entities.Order
import com.example.sudocean.data.entities.OrderWithItems
import com.example.sudocean.databinding.ItemOrderBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class OrderAdapter(
    private val onCancelClick: (Order) -> Unit
) : RecyclerView.Adapter<OrderAdapter.OrderViewHolder>() {

    private var items: List<OrderWithItems> = emptyList()
    private var expandedOrderId: Int = -1

    fun submitList(newList: List<OrderWithItems>) {
        items = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val binding = ItemOrderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return OrderViewHolder(binding)
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        val orderWithItems = items[position]
        val isExpanded = orderWithItems.order.id == expandedOrderId
        
        holder.bind(orderWithItems, isExpanded, onCancelClick) { clickedOrder ->
            expandedOrderId = if (expandedOrderId == clickedOrder.id) -1 else clickedOrder.id
            notifyDataSetChanged()
        }
    }

    override fun getItemCount(): Int = items.size

    class OrderViewHolder(private val binding: ItemOrderBinding) : RecyclerView.ViewHolder(binding.root) {
        private val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())

        fun bind(orderWithItems: OrderWithItems, isExpanded: Boolean, onCancelClick: (Order) -> Unit, onItemClick: (Order) -> Unit) {
            val order = orderWithItems.order
            
            // Если в статусе сохранен номер из 1С (через скобки), выводим его красиво
            binding.tvOrderId.text = "Заказ ${order.status.substringAfterLast("(", "").substringBefore(")") .ifEmpty { "№" + order.id }}"
            
            // Чистый статус без номера
            binding.tvOrderStatus.text = order.status.substringBefore(" (").ifEmpty { order.status }
            
            binding.tvOrderDate.text = "Дата: ${dateFormat.format(Date(order.date))}"
            binding.tvOrderTotal.text = String.format(Locale.getDefault(), "Сумма: %.2f ₽", order.totalAmount)

            if (isExpanded) {
                binding.itemsContainer.visibility = View.VISIBLE
                binding.itemsContainer.removeAllViews()
                
                val header = TextView(binding.root.context).apply {
                    text = "Состав заказа:"
                    textSize = 14f
                    setPadding(0, 0, 0, 8)
                    setTextColor(resources.getColor(R.color.ocean_text_secondary, null))
                }
                binding.itemsContainer.addView(header)

                orderWithItems.items.forEach { item ->
                    val itemTextView = TextView(binding.root.context).apply {
                        text = "• ${item.productName} (${item.quantity} шт.) — ${String.format(Locale.getDefault(), "%.2f", item.price * item.quantity)} ₽"
                        textSize = 14f
                        setTextColor(resources.getColor(R.color.white, null))
                        setPadding(16, 4, 0, 4)
                    }
                    binding.itemsContainer.addView(itemTextView)
                }
            } else {
                binding.itemsContainer.visibility = View.GONE
            }

            if (order.status.startsWith("Не оплачен") && isExpanded) {
                binding.btnCancelOrder.visibility = View.VISIBLE
                binding.btnCancelOrder.setOnClickListener { onCancelClick(order) }
            } else {
                binding.btnCancelOrder.visibility = View.GONE
            }

            binding.root.setOnClickListener { onItemClick(order) }
        }
    }
}
