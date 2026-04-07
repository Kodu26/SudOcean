package com.example.sudocean.ui

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.sudocean.R
import com.example.sudocean.SudOceanApplication
import com.example.sudocean.databinding.FragmentLoginBinding
import com.example.sudocean.models.LoginViewModel
import com.example.sudocean.models.LoginViewModelFactory

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val viewModel: LoginViewModel by viewModels {
        val app = requireActivity().application as SudOceanApplication
        LoginViewModelFactory(app.repository)
    }

    private var currentLoginType = "PHYSICAL"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()

        binding.LoginButton.setOnClickListener {
            performLogin()
        }

        binding.goToRegister.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_regFragment)
        }
    }

    private fun setupUI() {
        binding.toggleLoginType.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    R.id.btn_login_physical -> {
                        currentLoginType = "PHYSICAL"
                        binding.tilLoginIdentifier.hint = "Номер телефона"
                        binding.etLoginIdentifier.inputType = InputType.TYPE_CLASS_PHONE
                    }
                    R.id.btn_login_legal -> {
                        currentLoginType = "LEGAL"
                        binding.tilLoginIdentifier.hint = "ИНН"
                        binding.etLoginIdentifier.inputType = InputType.TYPE_CLASS_NUMBER
                    }
                }
                binding.etLoginIdentifier.text?.clear()
            }
        }
    }

    private fun performLogin() {
        val identifier = binding.etLoginIdentifier.text.toString()
        val password = binding.etLoginPassword.text.toString()

        if (identifier.isEmpty() || password.isEmpty()) {
            Toast.makeText(requireContext(), "Заполните все поля", Toast.LENGTH_SHORT).show()
            return
        }

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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
