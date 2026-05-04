package com.example.sudocean.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
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
import com.example.sudocean.utils.PhoneMaskWatcher
import com.example.sudocean.utils.Validator
import com.google.android.material.dialog.MaterialAlertDialogBuilder
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
    private var isProfileExpanded = false
    private var isPasswordSectionVisible = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        setupRecyclerView()
        observeViewModel()
        
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refreshOrders()
        }
        binding.swipeRefresh.setColorSchemeResources(R.color.ocean_primary, R.color.anchor_gold)

        binding.btnSaveProfile.setOnClickListener {
            saveProfileChanges()
        }

        binding.btnLogout.setOnClickListener {
            val app = requireActivity().application as SudOceanApplication
            app.clearUserSession()
            val navOptions = NavOptions.Builder().setPopUpTo(R.id.nav_graph, true).build()
            findNavController().navigate(R.id.loginFragment, null, navOptions)
        }

        binding.btnShowChangePassword.setOnClickListener {
            togglePasswordSection()
        }

        binding.btnConfirmPasswordChange.setOnClickListener {
            performPasswordChange()
        }

        binding.btnDeleteAccount.setOnClickListener {
            Log.d("1C_DEBUG", "Delete account button clicked")
            showDeleteAccountConfirmation()
        }

        // Устанавливаем начальное состояние режима редактирования (ВЫКЛ)
        setEditMode(false)
    }

    private fun setupUI() {
        binding.etProfilePhone.addTextChangedListener(PhoneMaskWatcher(binding.etProfilePhone))

        binding.btnToggleProfile.setOnClickListener {
            isProfileExpanded = !isProfileExpanded
            if (isProfileExpanded) {
                binding.layoutProfileExpandable.visibility = View.VISIBLE
                binding.ivProfileArrow.animate().rotation(180f).start()
            } else {
                binding.layoutProfileExpandable.visibility = View.GONE
                binding.ivProfileArrow.animate().rotation(0f).start()
                if (isPasswordSectionVisible) togglePasswordSection()
                // При закрытии профиля всегда выключаем редактирование
                binding.switchEditMode.isChecked = false
            }
        }

        binding.switchEditMode.setOnCheckedChangeListener { _, isChecked ->
            setEditMode(isChecked)
            if (!isChecked) {
                // Если выключили - возвращаем старые данные
                currentUser?.let { updateFields(it) }
                clearErrors()
                // Скрываем секцию смены пароля, если она была открыта
                if (isPasswordSectionVisible) {
                    togglePasswordSection()
                }
            }
        }
    }

    private fun setEditMode(enabled: Boolean) {
        // Делаем поля доступными или только для чтения
        binding.tilProfileName.isEnabled = enabled
        binding.tilProfilePhone.isEnabled = enabled
        binding.tilProfileInn.isEnabled = enabled
        binding.tilProfileKpp.isEnabled = enabled
        binding.tilProfileLegalAddress.isEnabled = enabled

        // Показываем/скрываем кнопки действия
        binding.btnSaveProfile.visibility = if (enabled) View.VISIBLE else View.GONE
        binding.btnShowChangePassword.visibility = if (enabled) View.VISIBLE else View.GONE
        binding.btnDeleteAccount.visibility = if (enabled) View.VISIBLE else View.GONE
        
        // Кнопка выхода видна ВСЕГДА, когда профиль развернут
        binding.btnLogout.visibility = View.VISIBLE
        
        // Визуальная подсказка: меняем прозрачность полей или цвет карточки
        val alpha = if (enabled) 1.0f else 0.7f
        binding.tilProfileName.alpha = alpha
        binding.tilProfilePhone.alpha = alpha
        binding.tilProfileInn.alpha = alpha
        binding.tilProfileKpp.alpha = alpha
        binding.tilProfileLegalAddress.alpha = alpha

        // Меняем текст переключателя для наглядности
        binding.switchEditMode.text = if (enabled) "Редактирование: ВКЛ" else "Только просмотр"
        
        // Цвет рамки
        val strokeColor = if (enabled) resources.getColor(R.color.anchor_gold, null) 
                         else resources.getColor(R.color.ocean_primary, null)
        binding.cardProfile.strokeColor = strokeColor
    }

    private fun updateFields(user: User) {
        binding.etProfileName.setText(user.fullName)
        binding.etProfilePhone.setText(user.phone)
        
        if (user.userType == "LEGAL") {
            binding.layoutProfileLegal.visibility = View.VISIBLE
            binding.etProfileInn.setText(user.inn)
            binding.etProfileKpp.setText(user.kpp)
            binding.etProfileLegalAddress.setText(user.legalAddress)
        } else {
            binding.layoutProfileLegal.visibility = View.GONE
        }
    }

    private fun observeViewModel() {
        viewModel.currentUser.observe(viewLifecycleOwner) { user ->
            user?.let {
                currentUser = it
                if (!binding.switchEditMode.isChecked) {
                    updateFields(it)
                }
            }
        }

        viewModel.userOrders.observe(viewLifecycleOwner) { orders ->
            adapter.submitList(orders, currentUser)
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.swipeRefresh.isRefreshing = isLoading
        }
    }

    private fun togglePasswordSection() {
        isPasswordSectionVisible = !isPasswordSectionVisible
        binding.layoutChangePassword.visibility = if (isPasswordSectionVisible) View.VISIBLE else View.GONE
        binding.btnShowChangePassword.text = if (isPasswordSectionVisible) "Отмена" else "Сменить пароль"
        if (!isPasswordSectionVisible) clearPasswordFields()
    }

    private fun clearPasswordFields() {
        binding.etOldPassword.text?.clear()
        binding.etNewPassword.text?.clear()
        binding.etConfirmPassword.text?.clear()
        binding.tilOldPassword.error = null
        binding.tilNewPassword.error = null
        binding.tilConfirmPassword.error = null
    }

    private fun performPasswordChange() {
        val user = currentUser ?: return
        val oldPass = binding.etOldPassword.text.toString().trim()
        val newPass = binding.etNewPassword.text.toString().trim()
        val confirmPass = binding.etConfirmPassword.text.toString().trim()

        binding.tilOldPassword.error = null
        binding.tilNewPassword.error = null
        binding.tilConfirmPassword.error = null

        var isValid = true

        if (oldPass != user.password.trim()) {
            binding.tilOldPassword.error = "Неверный текущий пароль"
            isValid = false
        }
        if (newPass.length < 4) {
            binding.tilNewPassword.error = "Минимум 4 символа"
            isValid = false
        }
        if (newPass != confirmPass) {
            binding.tilConfirmPassword.error = "Пароли не совпадают"
            isValid = false
        }

        if (!isValid) return

        lifecycleScope.launch {
            try {
                val app = requireActivity().application as SudOceanApplication
                app.repository.changePassword(user, oldPass, newPass)
                Toast.makeText(requireContext(), "Пароль успешно изменен", Toast.LENGTH_SHORT).show()
                togglePasswordSection()
            } catch (e: Exception) {
                val msg = e.message?.replace("java.lang.Exception:", "") ?: "Ошибка смены пароля"
                Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showDeleteAccountConfirmation() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Удаление аккаунта")
            .setMessage("Вы уверены, что хотите полностью удалить свой аккаунт? Все данные будут стерты без возможности восстановления.")
            .setPositiveButton("Удалить") { _, _ ->
                performAccountDeletion()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun performAccountDeletion() {
        val user = currentUser ?: return
        lifecycleScope.launch {
            try {
                val app = requireActivity().application as SudOceanApplication
                app.repository.deleteAccount(user)
                Toast.makeText(requireContext(), "Аккаунт успешно удален", Toast.LENGTH_SHORT).show()
                app.clearUserSession()
                val navOptions = NavOptions.Builder().setPopUpTo(R.id.nav_graph, true).build()
                findNavController().navigate(R.id.loginFragment, null, navOptions)
            } catch (e: Exception) {
                val msg = e.message?.replace("java.lang.Exception:", "") ?: "Ошибка удаления"
                Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun clearErrors() {
        binding.tilProfileName.error = null
        binding.tilProfilePhone.error = null
        binding.tilProfileInn.error = null
        binding.tilProfileKpp.error = null
        binding.tilProfileLegalAddress.error = null
    }

    private fun saveProfileChanges() {
        val user = currentUser ?: return
        
        val name = binding.etProfileName.text.toString().trim()
        val phone = binding.etProfilePhone.text.toString().trim()
        val inn = binding.etProfileInn.text.toString().trim()
        val kpp = binding.etProfileKpp.text.toString().trim()
        val address = binding.etProfileLegalAddress.text.toString().trim()

        clearErrors()
        var isValid = true

        if (name.isEmpty()) {
            binding.tilProfileName.error = "Поле обязательно"
            isValid = false
        }

        if (!Validator.isValidPhone(phone)) {
            binding.tilProfilePhone.error = "Некорректный номер"
            isValid = false
        }

        if (user.userType == "LEGAL") {
            if (!Validator.isValidInn(inn, "LEGAL")) {
                binding.tilProfileInn.error = "ИНН — 10 цифр"
                isValid = false
            }
            if (!Validator.isValidKpp(kpp)) {
                binding.tilProfileKpp.error = "КПП — 9 цифр"
                isValid = false
            }
            if (address.isEmpty()) {
                binding.tilProfileLegalAddress.error = "Укажите адрес"
                isValid = false
            }
        }

        if (!isValid) return

        val updatedUser = user.copy(
            fullName = name,
            phone = phone,
            inn = if (user.userType == "LEGAL") inn else null,
            kpp = if (user.userType == "LEGAL") kpp else null,
            legalAddress = if (user.userType == "LEGAL") address else null
        )

        lifecycleScope.launch {
            try {
                val app = requireActivity().application as SudOceanApplication
                app.repository.updateUser(updatedUser)
                Toast.makeText(requireContext(), "Данные успешно синхронизированы", Toast.LENGTH_SHORT).show()
                binding.switchEditMode.isChecked = false
            } catch (e: Exception) {
                val cleanMsg = e.message?.replace("java.lang.Exception:", "") ?: "Ошибка синхронизации"
                Toast.makeText(requireContext(), cleanMsg, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setupRecyclerView() {
        val app = requireActivity().application as SudOceanApplication
        adapter = OrderAdapter(
            onCancelClick = { order -> viewModel.cancelOrder(order) },
            onDownloadClick = { order ->
                lifecycleScope.launch {
                    Toast.makeText(requireContext(), "Загрузка счета...", Toast.LENGTH_SHORT).show()
                    app.repository.downloadAndOpenInvoice(requireContext(), order)
                }
            },
            onPayClick = { order ->
                val bundle = bundleOf(
                    "order_id" to order.id,
                    "total_amount" to order.totalAmount
                )
                findNavController().navigate(R.id.action_orderFragment_to_paymentFragment, bundle)
            }
        )
        binding.rvOrders.adapter = adapter
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
