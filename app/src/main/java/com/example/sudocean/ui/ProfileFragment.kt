package com.example.sudocean.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.example.sudocean.R
import com.example.sudocean.SudOceanApplication
import com.example.sudocean.data.entities.User
import com.example.sudocean.databinding.FragmentProfileBinding
import com.example.sudocean.models.OrderViewModel
import com.example.sudocean.models.OrderViewModelFactory
import com.example.sudocean.ui.adapters.OrderAdapter
import kotlinx.coroutines.launch

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val viewModel: OrderViewModel by viewModels {
        val app = requireActivity().application as SudOceanApplication
        OrderViewModelFactory(app, app.repository)
    }

    private lateinit var adapter: OrderAdapter
    private var currentUser: User? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        observeViewModel()
        
        // Настройка Swipe to Refresh
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refreshOrders()
        }
        // Настраиваем цвета индикатора в стиле приложения
        binding.swipeRefresh.setColorSchemeResources(R.color.ocean_primary, R.color.anchor_gold)

        binding.btnSaveProfile.setOnClickListener {
            saveProfileChanges()
        }

        binding.btnLogout.setOnClickListener {
            val app = requireActivity().application as SudOceanApplication
            app.clearUserSession()
            
            val navOptions = NavOptions.Builder()
                .setPopUpTo(R.id.nav_graph, true)
                .build()

            findNavController().navigate(R.id.loginFragment, null, navOptions)
        }
    }

    private fun observeViewModel() {
        viewModel.currentUser.observe(viewLifecycleOwner) { user ->
            user?.let {
                currentUser = it
                binding.etProfileName.setText(it.fullName)
                binding.etProfilePhone.setText(it.phone)
                
                if (it.userType == "LEGAL") {
                    binding.layoutProfileLegal.visibility = View.VISIBLE
                    binding.etProfileInn.setText(it.inn)
                    binding.etProfileKpp.setText(it.kpp)
                    binding.etProfileLegalAddress.setText(it.legalAddress)
                } else {
                    binding.layoutProfileLegal.visibility = View.GONE
                }
            }
        }

        viewModel.userOrders.observe(viewLifecycleOwner) { orders ->
            adapter.submitList(orders)
        }

        // Следим за состоянием загрузки, чтобы прятать индикатор
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.swipeRefresh.isRefreshing = isLoading
        }
    }

    private fun saveProfileChanges() {
        val user = currentUser ?: return
        val name = binding.etProfileName.text.toString()
        val phone = binding.etProfilePhone.text.toString()

        if (name.isEmpty() || phone.isEmpty()) {
            Toast.makeText(requireContext(), "Заполните обязательные поля", Toast.LENGTH_SHORT).show()
            return
        }

        val updatedUser = user.copy(
            fullName = name,
            phone = phone,
            inn = if (user.userType == "LEGAL") binding.etProfileInn.text.toString() else null,
            kpp = if (user.userType == "LEGAL") binding.etProfileKpp.text.toString() else null,
            legalAddress = if (user.userType == "LEGAL") binding.etProfileLegalAddress.text.toString() else null
        )

        lifecycleScope.launch {
            try {
                val app = requireActivity().application as SudOceanApplication
                app.repository.updateUser(updatedUser)
                Toast.makeText(requireContext(), "Данные сохранены и отправлены в 1С", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Ошибка при сохранении: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = OrderAdapter(onCancelClick = { order ->
            viewModel.cancelOrder(order)
        })
        binding.rvOrders.adapter = adapter
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
