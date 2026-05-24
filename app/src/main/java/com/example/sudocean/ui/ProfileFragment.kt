package com.example.sudocean.ui

import android.os.Bundle
import android.text.InputFilter
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
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
    private val legalForms = listOf("ООО", "НАО", "ЗАО", "ПАО", "АО", "ПК", "ИП")

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

        // Кнопка сохранения профиля больше не используется для общих данных
        binding.btnSaveProfile.visibility = View.GONE

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
            showDeleteAccountConfirmation()
        }

        setEditMode(false)
    }

    private fun setupUI() {
        // Поля включены (чтобы не были серыми), но не принимают фокус (нельзя печатать)
        val fields = listOf(
            binding.etProfilePhone, 
            binding.etProfileName, 
            binding.actvProfileLegalForm, 
            binding.etProfileInn, 
            binding.etProfileKpp, 
            binding.etProfileLegalAddress
        )
        
        fields.forEach { et ->
            et.isEnabled = true
            et.isFocusable = false
            et.isFocusableInTouchMode = false
            et.isCursorVisible = false
        }

        binding.btnToggleProfile.setOnClickListener {
            isProfileExpanded = !isProfileExpanded
            if (isProfileExpanded) {
                binding.layoutProfileExpandable.visibility = View.VISIBLE
                binding.ivProfileArrow.animate().rotation(180f).start()
            } else {
                binding.layoutProfileExpandable.visibility = View.GONE
                binding.ivProfileArrow.animate().rotation(0f).start()
                if (isPasswordSectionVisible) togglePasswordSection()
                binding.switchEditMode.isChecked = false
            }
        }

        binding.switchEditMode.setOnCheckedChangeListener { _, isChecked ->
            setEditMode(isChecked)
            if (!isChecked) {
                if (isPasswordSectionVisible) {
                    togglePasswordSection()
                }
            }
        }
    }

    private fun setEditMode(enabled: Boolean) {
        // Профильные данные ВСЕГДА включены визуально, но выключены функционально
        val layouts = listOf(
            binding.tilProfileName,
            binding.tilProfilePhone,
            binding.tilProfileLegalForm,
            binding.tilProfileInn,
            binding.tilProfileKpp,
            binding.tilProfileLegalAddress
        )

        layouts.forEach { til ->
            til.isEnabled = true // Текст будет белым/черным (обычным)
            til.alpha = 1.0f     // Убираем прозрачность
        }

        // Кнопка общего сохранения скрыта навсегда
        binding.btnSaveProfile.visibility = View.GONE
        
        // Кнопки действий видны только при включенном переключателе
        binding.btnShowChangePassword.visibility = if (enabled) View.VISIBLE else View.GONE
        binding.btnDeleteAccount.visibility = if (enabled) View.VISIBLE else View.GONE
        
        binding.btnLogout.visibility = View.VISIBLE

        binding.switchEditMode.text = if (enabled) getString(R.string.profile_edit_actions) else getString(R.string.profile_view_only)
        
        val strokeColor = if (enabled) resources.getColor(R.color.anchor_gold, null) 
                         else resources.getColor(R.color.ocean_primary, null)
        binding.cardProfile.strokeColor = strokeColor
    }

    private fun updateFields(user: User) {
        binding.etProfileName.setText(user.fullName)
        binding.etProfilePhone.setText(user.phone)
        
        if (user.userType == "LEGAL") {
            binding.layoutProfileLegal.visibility = View.VISIBLE
            binding.actvProfileLegalForm.setText(user.legalForm, false)
            binding.etProfileInn.setText(user.inn)
            binding.etProfileKpp.setText(user.kpp)
            binding.etProfileLegalAddress.setText(user.legalAddress)
            updateFieldsVisibility(user.legalForm ?: "")
        } else {
            binding.layoutProfileLegal.visibility = View.GONE
        }
    }

    private fun updateFieldsVisibility(form: String) {
        if (form == "ИП") {
            binding.tilProfileKpp.visibility = View.GONE
            binding.tilProfileLegalAddress.visibility = View.GONE
        } else {
            binding.tilProfileKpp.visibility = View.VISIBLE
            binding.tilProfileLegalAddress.visibility = View.VISIBLE
        }
    }

    private fun observeViewModel() {
        viewModel.currentUser.observe(viewLifecycleOwner) { user ->
            user?.let {
                currentUser = it
                updateFields(it)
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
        binding.btnShowChangePassword.text = if (isPasswordSectionVisible) getString(R.string.btn_hide_password_change) else getString(R.string.btn_change_password)
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

        if (oldPass.isEmpty() || newPass.isEmpty() || confirmPass.isEmpty()) {
            Toast.makeText(requireContext(), getString(R.string.error_fill_all_password_fields), Toast.LENGTH_SHORT).show()
            return
        }

        var isValid = true

        if (oldPass != user.password.trim()) {
            binding.tilOldPassword.error = getString(R.string.error_wrong_current_password)
            isValid = false
        }
        if (!Validator.isValidPassword(newPass)) {
            binding.tilNewPassword.error = getString(R.string.error_password_too_short)
            isValid = false
        }
        if (newPass != confirmPass) {
            binding.tilConfirmPassword.error = getString(R.string.error_passwords_dont_match)
            isValid = false
        }

        if (!isValid) return

        lifecycleScope.launch {
            try {
                val app = requireActivity().application as SudOceanApplication
                app.repository.changePassword(user, oldPass, newPass)
                Toast.makeText(requireContext(), getString(R.string.success_password_changed), Toast.LENGTH_SHORT).show()
                togglePasswordSection()
            } catch (e: Exception) {
                val msg = if (e.message?.contains("Unable to resolve host") == true) {
                    getString(R.string.error_no_internet_password)
                } else {
                    getString(R.string.error_service_unavailable)
                }
                Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showDeleteAccountConfirmation() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.delete_account_confirm_title))
            .setMessage(getString(R.string.delete_account_confirm_message))
            .setPositiveButton(getString(R.string.btn_delete)) { _, _ ->
                performAccountDeletion()
            }
            .setNegativeButton(getString(R.string.btn_cancel), null)
            .show()
    }

    private fun performAccountDeletion() {
        val user = currentUser ?: return
        lifecycleScope.launch {
            try {
                val app = requireActivity().application as SudOceanApplication
                app.repository.deleteAccount(user)
                Toast.makeText(requireContext(), getString(R.string.success_account_deleted), Toast.LENGTH_SHORT).show()
                app.clearUserSession()
                val navOptions = NavOptions.Builder().setPopUpTo(R.id.nav_graph, true).build()
                findNavController().navigate(R.id.loginFragment, null, navOptions)
            } catch (e: Exception) {
                val msg = if (e.message?.contains("Unable to resolve host") == true) {
                    getString(R.string.error_no_internet_delete)
                } else {
                    getString(R.string.error_service_unavailable)
                }
                Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun clearErrors() {
        binding.tilProfileName.error = null
        binding.tilProfilePhone.error = null
        binding.tilProfileLegalForm.error = null
        binding.tilProfileInn.error = null
        binding.tilProfileKpp.error = null
        binding.tilProfileLegalAddress.error = null
    }

    private fun saveProfileChanges() {
        // Отключено согласно требованиям
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
                val orderNumber = order.status.substringAfterLast("№", "").substringBefore(")")
                val bundle = bundleOf(
                    "order_id" to order.id,
                    "total_amount" to order.totalAmount,
                    "order_number" to orderNumber
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
