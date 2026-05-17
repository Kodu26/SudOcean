package com.example.sudocean.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.sudocean.R
import com.example.sudocean.SudOceanApplication
import com.example.sudocean.databinding.FragmentItemBinding
import com.example.sudocean.models.ItemViewModel
import com.example.sudocean.models.ItemViewModelFactory
import com.example.sudocean.ui.adapters.ProductAdapter
import com.google.android.material.chip.Chip

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

        setupUI()
        setupObservers()
    }

    private fun setupUI() {
        // Настройка адаптера
        val adapter = ProductAdapter(
            onPlusClick = { product -> viewModel.addToCart(product.id) },
            onMinusClick = { product -> viewModel.removeFromCart(product.id) },
            onItemClick = { product ->
                val bundle = Bundle().apply {
                    putInt("product_id", product.id)
                }
                findNavController().navigate(R.id.action_itemFragment_to_productDetailFragment, bundle)
            }
        )
        binding.rvProducts.adapter = adapter

        // Настройка Поиска
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                viewModel.setSearchQuery(query ?: "")
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                viewModel.setSearchQuery(newText ?: "")
                return true
            }
        })

        // Настройка Swipe to Refresh
        binding.swipeRefreshItems.setOnRefreshListener {
            viewModel.refreshProducts()
        }
        binding.swipeRefreshItems.setColorSchemeResources(R.color.ocean_primary, R.color.anchor_gold)
    }

    private fun setupObservers() {
        val adapter = binding.rvProducts.adapter as ProductAdapter

        // Наблюдаем за отфильтрованным списком товаров
        viewModel.filteredProducts.observe(viewLifecycleOwner) { products ->
            adapter.submitList(products)
        }

        // Обновление состояния корзины в списке
        viewModel.cartItems.observe(viewLifecycleOwner) { cartItems ->
            adapter.updateCart(cartItems)
        }

        // Динамическое создание кнопок категорий (Chips)
        viewModel.categories.observe(viewLifecycleOwner) { categories ->
            binding.chipGroupCategories.removeAllViews()
            categories.forEach { category ->
                val chip = Chip(requireContext()).apply {
                    text = category
                    isCheckable = true
                    setChipBackgroundColorResource(R.color.ocean_card)
                    setTextColor(resources.getColor(R.color.white, null))
                    if (category == "Все") isChecked = true
                    
                    setOnCheckedChangeListener { _, isChecked ->
                        if (isChecked) viewModel.setCategory(category)
                    }
                }
                binding.chipGroupCategories.addView(chip)
            }
        }

        // Индикатор загрузки
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.swipeRefreshItems.isRefreshing = isLoading
        }

        // Обработка ошибок
        viewModel.errorMessage.observe(viewLifecycleOwner) { message ->
            message?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
