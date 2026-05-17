package com.example.sudocean.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.example.sudocean.R
import com.example.sudocean.SudOceanApplication
import com.example.sudocean.data.entities.Order
import com.example.sudocean.data.entities.OrderItem
import com.example.sudocean.databinding.FragmentPaymentProcessBinding
import com.example.sudocean.models.PaymentViewModel
import com.example.sudocean.models.PaymentViewModelFactory
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.random.Random

class PaymentProcessFragment : Fragment() {

    private var _binding: FragmentPaymentProcessBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PaymentViewModel by viewModels {
        val app = requireActivity().application as SudOceanApplication
        PaymentViewModelFactory(app.repository)
    }

    private var generatedCode: String = ""
    private var isLegalEntity: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPaymentProcessBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ЗАЩИТА: Запрещаем скриншоты на странице оплаты
        requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)

        val amount = arguments?.getDouble("total_amount") ?: 0.0

        setupUI()

        binding.btnConfirmPayment.setOnClickListener {
            val enteredValue = binding.etConfirmInput.text.toString().trim()
            if (enteredValue == generatedCode) {
                processOrder(amount)
            } else {
                Toast.makeText(requireContext(), "Оплата не завершена. Повторите оплату или проверьте данные заказа.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setupUI() {
        val app = requireActivity().application as SudOceanApplication
        
        viewLifecycleOwner.lifecycleScope.launch {
            val user = app.repository.getUserById(app.currentUserId)
            isLegalEntity = user?.userType == "LEGAL"

            binding.tvHeaderTitle.text = "Подтверждение"
            binding.layoutLegalConfirm.visibility = View.VISIBLE
            binding.tvRequiredAmount.visibility = View.GONE
            binding.tilAmount.hint = "Введите код подтверждения"
            
            generatedCode = Random.nextInt(100, 999).toString()
            binding.tvConfirmCodeDisplay.text = generatedCode
        }
    }

    private fun processOrder(amount: Double) {
        binding.layoutLegalConfirm.visibility = View.GONE
        binding.tilAmount.visibility = View.GONE
        binding.btnConfirmPayment.visibility = View.GONE
        
        binding.paymentProgress.visibility = View.VISIBLE
        binding.tvPaymentStatus.visibility = View.VISIBLE
        
        val app = requireActivity().application as SudOceanApplication
        val repository = app.repository

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                binding.tvPaymentStatus.text = "Установка защищенного TLS-соединения..."
                delay(800)
                binding.tvPaymentStatus.text = "Проверка целостности данных..."
                delay(600)
                binding.tvPaymentStatus.text = "Синхронизация с 1С..."

                val userId = app.currentUserId
                val cartItems = repository.getCartItems(userId).first()
                val products = repository.allProducts.first()
                val user = repository.getUserById(userId)

                if (user != null && cartItems.isNotEmpty()) {
                    val newOrder = Order(
                        userId = userId,
                        date = System.currentTimeMillis(),
                        totalAmount = amount,
                        status = "В процессе"
                    )
                    val orderId = repository.insertOrder(newOrder)

                    val orderItems = cartItems.map { cartItem ->
                        val product = products.find { it.id == cartItem.productId }
                        OrderItem(
                            orderId = orderId.toInt(),
                            productId = cartItem.productId,
                            productName = product?.name ?: "Товар",
                            quantity = cartItem.quantity,
                            price = product?.price ?: 0.0
                        )
                    }
                    repository.insertOrderItems(orderItems)

                    val response = repository.sendOrderTo1C(user, newOrder.copy(id = orderId.toInt()), cartItems, products)
                    
                    if (response != null) {
                        val statusFrom1C = response.status ?: "Не оплачен"
                        val finalStatus = "$statusFrom1C (№${response.order_number})"
                        
                        viewModel.updateOrderStatus(orderId, finalStatus)
                        repository.clearCart(userId)
                        repository.syncProducts()
                        
                        binding.paymentProgress.visibility = View.GONE
                        binding.ivSuccessCheck.visibility = View.VISIBLE
                        binding.tvPaymentStatus.text = "Заказ успешно создан!"
                        binding.tvPaymentStatus.setTextColor(resources.getColor(R.color.green, null))

                        delay(1500)

                        if (isLegalEntity) {
                            val navOptions = NavOptions.Builder()
                                .setPopUpTo(R.id.nav_graph, true)
                                .build()
                            findNavController().navigate(R.id.itemFragment, null, navOptions)
                        } else {
                            val bundle = bundleOf(
                                "order_id" to orderId.toInt(),
                                "total_amount" to amount,
                                "order_number" to response.order_number,
                                "payment_link" to response.payment_link,
                                "payment_qr" to response.payment_qr
                            )
                            findNavController().navigate(R.id.action_paymentProcessFragment_to_paymentFragment, bundle)
                        }
                    } else {
                        throw Exception("1C_UNAVAILABLE")
                    }
                }
            } catch (e: Exception) {
                binding.paymentProgress.visibility = View.GONE
                val message = when (e.message) {
                    "NO_NETWORK" -> "Заказ не отправлен. Проверьте подключение к сети и повторите оформление."
                    "1C_UNAVAILABLE" -> "Сервис временно недоступен. Повторите операцию позднее."
                    else -> "Оплата не завершена. Повторите оплату или проверьте данные заказа."
                }
                binding.tvPaymentStatus.text = message
                Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        requireActivity().window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        _binding = null
    }
}
