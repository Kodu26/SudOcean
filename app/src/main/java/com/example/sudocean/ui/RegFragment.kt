package com.example.sudocean.ui

import android.os.Bundle
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
import com.example.sudocean.data.entities.User
import com.example.sudocean.databinding.FragmentRegisterBinding
import com.example.sudocean.models.RegViewModel
import com.example.sudocean.models.RegViewModelFactory
import kotlinx.coroutines.launch

class RegFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!

    private val viewModel: RegViewModel by viewModels {
        val app = requireActivity().application as SudOceanApplication
        RegViewModelFactory(app.repository)
    }

    private var currentUserType = "PHYSICAL"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()

        binding.registerButton.setOnClickListener {
            performRegistration()
        }

        binding.backToLogin.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupUI() {
        // Исправлено: добавлены явные типы для лямбды переключателя
        binding.toggleUserType.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    R.id.btn_type_physical -> {
                        currentUserType = "PHYSICAL"
                        binding.layoutLegalOnly.visibility = View.GONE
                    }
                    R.id.btn_type_legal -> {
                        currentUserType = "LEGAL"
                        binding.layoutLegalOnly.visibility = View.VISIBLE
                    }
                }
            }
        }
    }

    private fun performRegistration() {
        val name = binding.etName.text.toString()
        val phone = binding.etPhone.text.toString()
        val password = binding.etPassword.text.toString()
        
        if (name.isEmpty() || phone.isEmpty() || password.isEmpty()) {
            Toast.makeText(requireContext(), "Заполните основные поля", Toast.LENGTH_SHORT).show()
            return
        }

        val inn = binding.etInn.text.toString()
        val kpp = binding.etKpp.text.toString()
        val address = binding.etLegalAddress.text.toString()

        if (currentUserType == "LEGAL" && (inn.isEmpty() || kpp.isEmpty())) {
            Toast.makeText(requireContext(), "Для юр. лиц ИНН и КПП обязательны", Toast.LENGTH_SHORT).show()
            return
        }

        val newUser = User(
            userType = currentUserType,
            fullName = name,
            phone = phone,
            password = password,
            inn = if (currentUserType == "LEGAL") inn else null,
            kpp = if (currentUserType == "LEGAL") kpp else null,
            legalAddress = if (currentUserType == "LEGAL") address else null
        )

        lifecycleScope.launch {
            try {
                viewModel.register(newUser)
                Toast.makeText(requireContext(), "Регистрация успешна!", Toast.LENGTH_SHORT).show()
                findNavController().navigateUp()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
