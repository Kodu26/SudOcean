package com.example.sudocean.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.sudocean.R
import com.example.sudocean.SudOceanApplication
import com.example.sudocean.data.entities.User
import com.example.sudocean.databinding.FragmentCartBinding
import com.example.sudocean.models.CartViewModel
import com.example.sudocean.models.CartViewModelFactory
import com.example.sudocean.ui.adapters.CartAdapter

class CartFragment : Fragment() {

    private var _binding: FragmentCartBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CartViewModel by viewModels {
        val app = requireActivity().application as SudOceanApplication
        CartViewModelFactory(app, app.repository)
    }

    private lateinit var adapter: CartAdapter
    private var currentUser: User? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCartBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        observeViewModel()

        binding.btnPay.setOnClickListener {
            if (viewModel.cartProducts.value.isNullOrEmpty()) {
                Toast.makeText(requireContext(), "Корзина пуста", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            viewModel.checkout()
        }

        binding.btnClearCart.setOnClickListener {
            viewModel.clearAll()
        }
        
        viewModel.checkoutData.observe(viewLifecycleOwner) { data ->
            if (data != null) {
                val (orderId, amount) = data
                val bundle = bundleOf(
                    "order_id" to orderId,
                    "total_amount" to amount
                )
                viewModel.clearCheckoutData()
                
                // Теперь ВСЕ типы аккаунтов идут сначала на подтверждение (PaymentProcessFragment)
                findNavController().navigate(R.id.action_cartFragment_to_paymentProcessFragment, bundle)
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = CartAdapter(
            onPlusClick = { cartProduct -> viewModel.increaseQuantity(cartProduct) },
            onMinusClick = { cartProduct -> viewModel.decreaseQuantity(cartProduct) }
        )
        binding.rvCartItems.adapter = adapter
    }

    private fun observeViewModel() {
        viewModel.cartProducts.observe(viewLifecycleOwner) { products ->
            adapter.submitList(products)
            binding.btnPay.isEnabled = products.isNotEmpty()
        }

        viewModel.totalAmount.observe(viewLifecycleOwner) { total ->
            binding.tvTotalPrice.text = getString(R.string.total_amount, total)
        }

        viewModel.currentUser.observe(viewLifecycleOwner) { user ->
            user?.let {
                currentUser = it
                binding.tvDisplayName.text = it.fullName
                binding.tvDisplayPhone.text = "Тел: ${it.phone}"
                
                if (it.userType == "LEGAL") {
                    binding.tvDisplayInn.text = "ИНН: ${it.inn}"
                    binding.tvDisplayInn.visibility = View.VISIBLE
                    binding.btnPay.text = "Оформить заказ"
                } else {
                    binding.tvDisplayInn.visibility = View.GONE
                    binding.btnPay.text = "К оплате"
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
