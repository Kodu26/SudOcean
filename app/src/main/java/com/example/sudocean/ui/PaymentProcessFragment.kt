package com.example.sudocean.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.sudocean.R
import com.example.sudocean.SudOceanApplication
import com.example.sudocean.data.entities.OrderItem
import com.example.sudocean.databinding.FragmentPaymentProcessBinding
import com.example.sudocean.models.PaymentViewModel
import com.example.sudocean.models.PaymentViewModelFactory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Locale

class PaymentProcessFragment : Fragment() {

    private var _binding: FragmentPaymentProcessBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PaymentViewModel by viewModels {
        val app = requireActivity().application as SudOceanApplication
        PaymentViewModelFactory(app.repository)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPaymentProcessBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val orderId = arguments?.getLong("order_id") ?: -1L
        val amount = arguments?.getDouble("total_amount") ?: 0.0

        binding.tvRequiredAmount.text = String.format(Locale.getDefault(), "Сумма: %.2f ₽", amount)

        binding.btnConfirmPayment.setOnClickListener {
            val enteredAmountStr = binding.etConfirmAmount.text.toString().replace(",", ".")
            val enteredAmount = enteredAmountStr.toDoubleOrNull()

            if (enteredAmount != null && Math.abs(enteredAmount - amount) < 1.0) {
                processPaymentAndSendTo1C(orderId)
            } else {
                Toast.makeText(requireContext(), "Введите верную сумму: $amount", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun processPaymentAndSendTo1C(orderId: Long) {
        binding.tilAmount.visibility = View.GONE
        binding.btnConfirmPayment.visibility = View.GONE
        binding.paymentProgress.visibility = View.VISIBLE
        binding.tvPaymentStatus.visibility = View.VISIBLE
        binding.tvPaymentStatus.text = "Синхронизация с 1С..."

        val app = requireActivity().application as SudOceanApplication
        val repository = app.repository

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val userId = app.currentUserId
                val cartItems = repository.getCartItems(userId).first()
                val products = repository.allProducts.first()
                val user = repository.getUserById(userId)
                val order = repository.getOrderById(orderId.toInt())

                if (cartItems.isEmpty()) {
                    binding.tvPaymentStatus.text = "Ошибка: корзина пуста"
                    binding.paymentProgress.visibility = View.GONE
                    return@launch
                }

                // 1. Сохраняем детали локально
                val orderItems = cartItems.map { cartItem ->
                    val product = products.find { it.id == cartItem.productId }
                    OrderItem(
                        orderId = orderId.toInt(),
                        productId = cartItem.productId,
                        productName = product?.name ?: "Неизвестный товар",
                        quantity = cartItem.quantity,
                        price = product?.price ?: 0.0
                    )
                }
                repository.insertOrderItems(orderItems)

                // 2. Отправляем в 1С и получаем реальный номер
                if (user != null && order != null) {
                    val remoteOrderNumber = repository.sendOrderTo1C(user, order, cartItems, products)
                    
                    if (remoteOrderNumber != null) {
                        // Обновляем статус и сохраняем номер из 1С (временно в поле статуса для простоты)
                        viewModel.updateOrderStatus(orderId, "Оплачен (№$remoteOrderNumber)")
                        repository.clearCart(userId)
                        
                        binding.paymentProgress.visibility = View.GONE
                        binding.ivSuccessCheck.visibility = View.VISIBLE
                        binding.tvPaymentStatus.text = "Заказ №$remoteOrderNumber оформлен!"
                        binding.tvPaymentStatus.setTextColor(resources.getColor(R.color.green, null))
                    } else {
                        binding.tvPaymentStatus.text = "Ошибка связи с 1С"
                        binding.paymentProgress.visibility = View.GONE
                    }
                }

                kotlinx.coroutines.delay(3000)
                findNavController().navigate(R.id.action_paymentProcessFragment_to_itemFragment)

            } catch (e: Exception) {
                binding.tvPaymentStatus.text = "Критическая ошибка"
                binding.paymentProgress.visibility = View.GONE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
