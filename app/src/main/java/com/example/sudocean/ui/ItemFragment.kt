package com.example.sudocean.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.sudocean.R
import com.example.sudocean.SudOceanApplication
import com.example.sudocean.databinding.FragmentItemBinding
import com.example.sudocean.models.ItemViewModel
import com.example.sudocean.models.ItemViewModelFactory
import com.example.sudocean.ui.adapters.ProductAdapter

class ItemFragment : Fragment() {

    private var _binding: FragmentItemBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ItemViewModel by viewModels {
        val app = requireActivity().application as SudOceanApplication
        ItemViewModelFactory(app, app.repository)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentItemBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = ProductAdapter(
            onPlusClick = { product -> viewModel.addToCart(product.id) },
            onMinusClick = { product -> viewModel.removeFromCart(product.id) }
        )
        binding.rvProducts.adapter = adapter

        // Настройка Swipe to Refresh
        binding.swipeRefreshItems.setOnRefreshListener {
            viewModel.refreshProducts()
        }
        binding.swipeRefreshItems.setColorSchemeResources(R.color.ocean_primary, R.color.anchor_gold)

        viewModel.allProducts.observe(viewLifecycleOwner) { products ->
            adapter.submitList(products)
        }

        viewModel.cartItems.observe(viewLifecycleOwner) { cartItems ->
            adapter.updateCart(cartItems)
        }

        // Синхронизация индикатора загрузки
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.swipeRefreshItems.isRefreshing = isLoading
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
