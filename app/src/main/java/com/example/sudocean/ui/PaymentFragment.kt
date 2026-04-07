package com.example.sudocean.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.sudocean.R
import com.example.sudocean.databinding.FragmentPaymentBinding
import java.util.Locale

class PaymentFragment : Fragment() {

    private var _binding: FragmentPaymentBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPaymentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val orderId = arguments?.getLong("order_id") ?: -1L
        val amount = arguments?.getDouble("total_amount") ?: 0.0
        
        binding.tvPaymentAmount.text = String.format(Locale.getDefault(), "К оплате: %.2f ₽", amount)

        // Переход в "Банк" (PaymentProcessFragment)
        binding.btnPayBank.setOnClickListener {
            val bundle = bundleOf(
                "order_id" to orderId,
                "total_amount" to amount
            )
            findNavController().navigate(R.id.action_paymentFragment_to_paymentProcessFragment, bundle)
        }

        // Возврат назад (статус заказа остается "В процессе")
        binding.btnDone.setOnClickListener {
            findNavController().navigate(R.id.action_paymentFragment_to_itemFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
