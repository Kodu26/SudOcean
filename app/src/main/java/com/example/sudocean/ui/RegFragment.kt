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
import com.example.sudocean.utils.PhoneMaskWatcher
import com.example.sudocean.utils.Validator
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
        binding.etPhone.addTextChangedListener(PhoneMaskWatcher(binding.etPhone))

        binding.toggleUserType.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                clearErrors()
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

    private fun clearErrors() {
        binding.tilName.error = null
        binding.tilPhone.error = null
        binding.tilInn.error = null
        binding.tilKpp.error = null
        binding.tilLegalAddress.error = null
        binding.tilPassword.error = null
    }

    private fun performRegistration() {
        val name = binding.etName.text.toString().trim()
        val phone = binding.etPhone.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()
        val inn = binding.etInn.text.toString().trim()
        val kpp = binding.etKpp.text.toString().trim()
        val address = binding.etLegalAddress.text.toString().trim()

        clearErrors()
        var isValid = true

        if (name.isEmpty()) {
            binding.tilName.error = "Введите ФИО или наименование"
            isValid = false
        }

        if (!Validator.isValidPhone(phone)) {
            binding.tilPhone.error = "Некорректный номер телефона"
            isValid = false
        }

        if (password.length < 4) {
            binding.tilPassword.error = "Пароль слишком короткий (мин. 4 символа)"
            isValid = false
        }

        if (currentUserType == "LEGAL") {
            if (!Validator.isValidInn(inn, "LEGAL")) {
                binding.tilInn.error = "ИНН юрлица должен содержать 10 цифр"
                isValid = false
            }
            if (!Validator.isValidKpp(kpp)) {
                binding.tilKpp.error = "КПП должен содержать 9 цифр"
                isValid = false
            }
            if (address.isEmpty()) {
                binding.tilLegalAddress.error = "Введите юридический адрес"
                isValid = false
            }
        }

        if (!isValid) return

        val cleanPhone = phone.replace(Regex("[^\\d]"), "")
        val remoteIdValue = if (currentUserType == "LEGAL") inn else cleanPhone

        val newUser = User(
            remoteId = remoteIdValue,
            userType = currentUserType,
            fullName = name,
            phone = phone,
            password = password,
            inn = if (currentUserType == "LEGAL" || inn.isNotEmpty()) inn else null,
            kpp = if (currentUserType == "LEGAL") kpp else null,
            legalAddress = if (currentUserType == "LEGAL") address else null
        )

        lifecycleScope.launch {
            try {
                // 1. Проверка уникальности локально
                if (viewModel.checkUserExists(remoteIdValue)) {
                    val label = if (currentUserType == "LEGAL") "ИНН" else "Номер телефона"
                    Toast.makeText(requireContext(), "$label уже зарегистрирован в приложении", Toast.LENGTH_LONG).show()
                    return@launch
                }

                // 2. Попытка регистрации (включает проверку в 1С)
                viewModel.register(newUser)
                Toast.makeText(requireContext(), "Регистрация успешна!", Toast.LENGTH_SHORT).show()
                findNavController().navigateUp()
            } catch (e: Exception) {
                // Если 1С вернет ошибку "Клиент уже существует", она отобразится здесь
                val cleanMessage = e.message?.replace("java.lang.Exception:", "") ?: "Ошибка регистрации"
                Toast.makeText(requireContext(), cleanMessage, Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
