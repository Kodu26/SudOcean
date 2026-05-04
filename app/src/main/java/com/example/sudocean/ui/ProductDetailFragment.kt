package com.example.sudocean.ui

import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import coil.load
import com.example.sudocean.R
import com.example.sudocean.SudOceanApplication
import com.example.sudocean.databinding.FragmentProductDetailBinding
import com.example.sudocean.models.ItemViewModel
import com.example.sudocean.models.ItemViewModelFactory
import kotlinx.coroutines.launch
import java.util.Locale

class ProductDetailFragment : Fragment() {

    private var _binding: FragmentProductDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ItemViewModel by viewModels {
        val app = requireActivity().application as SudOceanApplication
        ItemViewModelFactory(app, app.repository)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProductDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val productId = arguments?.getInt("product_id") ?: -1
        
        setupToolbar()
        loadProductDetails(productId)
        observeCart(productId)
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material)
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun loadProductDetails(productId: Int) {
        val app = requireActivity().application as SudOceanApplication
        
        viewLifecycleOwner.lifecycleScope.launch {
            val product = app.repository.getProductById(productId)
            product?.let { p ->
                binding.tvDetailName.text = p.name
                binding.tvDetailCategory.text = p.category
                binding.tvDetailPrice.text = String.format(Locale.getDefault(), "%.2f ₽", p.price)
                binding.tvDetailDescription.text = p.description

                // ЛОГИКА ОСТАТКОВ
                if (p.stock > 0) {
                    binding.tvDetailStockStatus.text = "В наличии: ${p.stock} шт."
                    binding.tvDetailStockStatus.setTextColor(resources.getColor(R.color.green, null))
                    binding.btnInitialAdd.isEnabled = true
                    binding.btnDetailPlus.isEnabled = true
                } else {
                    binding.tvDetailStockStatus.text = "Нет в наличии"
                    binding.tvDetailStockStatus.setTextColor(resources.getColor(R.color.signal_red, null))
                    binding.btnInitialAdd.isEnabled = false
                    binding.btnDetailPlus.isEnabled = false
                }

                // Картинка
                if (!p.imageUrl.isNullOrEmpty()) {
                    try {
                        val cleanBase64 = if (p.imageUrl.contains(",")) p.imageUrl.substringAfter(",") else p.imageUrl
                        val imageBytes = Base64.decode(cleanBase64, Base64.DEFAULT)
                        val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                        binding.ivProductDetail.load(bitmap)
                    } catch (e: Exception) {
                        binding.ivProductDetail.setImageResource(R.drawable.ic_history)
                    }
                } else {
                    binding.ivProductDetail.setImageResource(R.drawable.ic_history)
                }

                // Нажатия
                binding.btnInitialAdd.setOnClickListener { viewModel.addToCart(p.id) }
                binding.btnDetailPlus.setOnClickListener { viewModel.addToCart(p.id) }
                binding.btnDetailMinus.setOnClickListener { viewModel.removeFromCart(p.id) }
            }
        }
    }

    private fun observeCart(productId: Int) {
        viewModel.cartItems.observe(viewLifecycleOwner) { items ->
            val cartItem = items.find { it.productId == productId }
            val quantity = cartItem?.quantity ?: 0

            if (quantity > 0) {
                binding.btnInitialAdd.visibility = View.GONE
                binding.layoutQuantityControl.visibility = View.VISIBLE
                binding.tvDetailQuantity.text = quantity.toString()
            } else {
                binding.btnInitialAdd.visibility = View.VISIBLE
                binding.layoutQuantityControl.visibility = View.GONE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
