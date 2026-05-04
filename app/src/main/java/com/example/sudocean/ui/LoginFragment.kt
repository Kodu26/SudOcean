package com.example.sudocean.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.InputFilter
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.sudocean.R
import com.example.sudocean.SudOceanApplication
import com.example.sudocean.databinding.FragmentLoginBinding
import com.example.sudocean.models.LoginViewModel
import com.example.sudocean.models.LoginViewModelFactory
import com.example.sudocean.utils.PhoneMaskWatcher
import com.example.sudocean.utils.Validator

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val viewModel: LoginViewModel by viewModels {
        val app = requireActivity().application as SudOceanApplication
        LoginViewModelFactory(app.repository)
    }

    private var currentLoginType = "PHYSICAL"
    private var phoneMaskWatcher: PhoneMaskWatcher? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Инициализируем состояние при каждом создании вью
        resetToDefaultState()
        
        setupListeners()
    }

    private fun resetToDefaultState() {
        currentLoginType = "PHYSICAL"
        binding.toggleLoginType.check(R.id.btn_login_physical)
        updateInputType(true)
        clearErrors()
        binding.etLoginIdentifier.text?.clear()
        binding.etLoginPassword.text?.clear()
    }

    private fun setupListeners() {
        binding.toggleLoginType.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                clearErrors()
                binding.etLoginIdentifier.text?.clear()
                currentLoginType = if (checkedId == R.id.btn_login_physical) "PHYSICAL" else "LEGAL"
                updateInputType(currentLoginType == "PHYSICAL")
            }
        }

        binding.LoginButton.setOnClickListener {
            performLogin()
        }

        binding.goToRegister.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_regFragment)
        }

        binding.tvForgotPassword.setOnClickListener {
            showResetPasswordDialog()
        }
    }

    private fun updateInputType(isPhone: Boolean) {
        if (isPhone) {
            binding.tilLoginIdentifier.hint = "Номер телефона"
            binding.etLoginIdentifier.inputType = InputType.TYPE_CLASS_PHONE
            binding.etLoginIdentifier.filters = arrayOf()
            applyPhoneMask(true)
        } else {
            binding.tilLoginIdentifier.hint = "ИНН"
            binding.etLoginIdentifier.inputType = InputType.TYPE_CLASS_NUMBER
            binding.etLoginIdentifier.filters = arrayOf(InputFilter.LengthFilter(10))
            applyPhoneMask(false)
        }
    }

    private fun applyPhoneMask(enabled: Boolean) {
        // Удаляем старый экземпляр, если он был
        phoneMaskWatcher?.let {
            binding.etLoginIdentifier.removeTextChangedListener(it)
        }
        
        if (enabled) {
            // Создаем маску заново для нового EditText
            phoneMaskWatcher = PhoneMaskWatcher(binding.etLoginIdentifier)
            binding.etLoginIdentifier.addTextChangedListener(phoneMaskWatcher)
        } else {
            phoneMaskWatcher = null
        }
    }

    private fun performLogin() {
        val identifier = binding.etLoginIdentifier.text.toString().trim()
        val password = binding.etLoginPassword.text.toString().trim()

        clearErrors()
        var isValid = true

        if (password.isEmpty()) {
            binding.tilLoginPassword.error = "Введите пароль"
            isValid = false
        }

        if (currentLoginType == "PHYSICAL") {
            if (!Validator.isValidPhone(identifier)) {
                binding.tilLoginIdentifier.error = "Введите полный номер телефона"
                isValid = false
            }
        } else {
            if (!Validator.isValidInn(identifier, "LEGAL")) {
                binding.tilLoginIdentifier.error = "ИНН должен содержать 10 цифр"
                isValid = false
            }
        }

        if (!isValid) return

        val app = requireActivity().application as SudOceanApplication
        viewModel.login(currentLoginType, identifier, password) { user ->
            if (user != null) {
                app.saveUserSession(user.id)
                findNavController().navigate(R.id.action_loginFragment_to_itemFragment)
            } else {
                val errorMsg = if (currentLoginType == "PHYSICAL") "Неверный телефон или пароль" else "Неверный ИНН или пароль"
                Toast.makeText(requireContext(), errorMsg, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showResetPasswordDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Восстановление пароля")
            .setMessage("Для сброса пароля свяжитесь с нашей службой поддержки:\n\n📞 Тел: +7 (999) 123-45-67\n💬 MAX: +7 (999) 123-45-67")
            .setPositiveButton("Понятно", null)
            .show()
    }

    private fun clearErrors() {
        binding.tilLoginIdentifier.error = null
        binding.tilLoginPassword.error = null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        phoneMaskWatcher = null
        _binding = null
    }
}
