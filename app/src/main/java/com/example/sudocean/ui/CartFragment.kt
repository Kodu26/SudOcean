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
            viewModel.checkout()
        }

        binding.btnClearCart.setOnClickListener {
            viewModel.clearAll()
        }
        
        viewModel.lastOrderId.observe(viewLifecycleOwner) { orderId ->
            if (orderId != null) {
                val bundle = bundleOf(
                    "order_id" to orderId,
                    "total_amount" to (viewModel.totalAmount.value ?: 0.0)
                )
                viewModel.clearLastOrderId()
                findNavController().navigate(R.id.action_cartFragment_to_paymentFragment, bundle)
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = CartAdapter(
            onPlusClick = { product -> viewModel.increaseQuantity(product) },
            onMinusClick = { product -> viewModel.decreaseQuantity(product) }
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
                binding.tvDisplayName.text = it.fullName
                binding.tvDisplayPhone.text = "Тел: ${it.phone}"
                
                if (it.userType == "LEGAL") {
                    binding.tvDisplayInn.text = "ИНН: ${it.inn}"
                    binding.tvDisplayInn.visibility = View.VISIBLE
                } else {
                    binding.tvDisplayInn.visibility = View.GONE
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
