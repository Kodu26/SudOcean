package com.example.sudocean.ui

import android.content.ContentValues
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.text.InputFilter
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.text.style.UnderlineSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.sudocean.R
import com.example.sudocean.SudOceanApplication
import com.example.sudocean.data.entities.User
import com.example.sudocean.databinding.FragmentRegBinding
import com.example.sudocean.models.RegViewModel
import com.example.sudocean.models.RegViewModelFactory
import com.example.sudocean.utils.PhoneMaskWatcher
import com.example.sudocean.utils.Validator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream

class RegFragment : Fragment() {

    private var _binding: FragmentRegBinding? = null
    private val binding get() = _binding!!

    private val viewModel: RegViewModel by viewModels {
        val app = requireActivity().application as SudOceanApplication
        RegViewModelFactory(app.repository)
    }

    private var currentUserType = "PHYSICAL"
    private val legalForms = listOf("ООО", "НАО", "ЗАО", "ПАО", "АО", "ПК", "ИП")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegBinding.inflate(inflater, container, false)
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
        
        binding.btnSwitchToBusiness.setOnClickListener {
            toggleUserType()
        }
    }

    private fun setupUI() {
        binding.etPhone.addTextChangedListener(PhoneMaskWatcher(binding.etPhone))

        // Настройка выпадающего списка форм
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, legalForms)
        binding.actvLegalForm.setAdapter(adapter)
        
        binding.actvLegalForm.setOnItemClickListener { _, _, position, _ ->
            val selectedForm = legalForms[position]
            updateFieldsVisibility(selectedForm)
        }
        
        setupPrivacyPolicyLink()
        updateUIForType()
    }

    private fun setupPrivacyPolicyLink() {
        val part1 = getString(R.string.privacy_policy_consent_part1)
        val linkPart = getString(R.string.privacy_policy_link)
        val fullText = part1 + linkPart
        val spannableString = SpannableString(fullText)
        
        val start = fullText.indexOf(linkPart)
        val end = start + linkPart.length
        
        if (start != -1) {
            val clickableSpan = object : ClickableSpan() {
                override fun onClick(widget: View) {
                    downloadPrivacyPolicy()
                }
            }
            spannableString.setSpan(clickableSpan, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            spannableString.setSpan(UnderlineSpan(), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            spannableString.setSpan(ForegroundColorSpan(resources.getColor(R.color.ocean_primary, null)), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        
        binding.tvPrivacyPolicy.text = spannableString
        binding.tvPrivacyPolicy.movementMethod = LinkMovementMethod.getInstance()
        binding.tvPrivacyPolicy.highlightColor = Color.TRANSPARENT
    }

    private fun downloadPrivacyPolicy() {
        lifecycleScope.launch {
            val uri = withContext(Dispatchers.IO) {
                try {
                    val fileName = "Politika_konfidencialnosti_SudOcean.pdf"
                    val inputStream: InputStream = requireContext().assets.open(fileName)
                    saveFileToDownloads(inputStream, fileName)
                } catch (e: Exception) {
                    null
                }
            }
            
            if (uri != null) {
                Toast.makeText(requireContext(), getString(R.string.privacy_policy_downloaded), Toast.LENGTH_SHORT).show()
                openPdf(uri)
            } else {
                Toast.makeText(requireContext(), getString(R.string.privacy_policy_download_failed), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveFileToDownloads(inputStream: InputStream, fileName: String): Uri? {
        val resolver = requireContext().contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
        }
        
        return try {
            val uri: Uri? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            } else {
                val directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val file = java.io.File(directory, fileName)
                Uri.fromFile(file)
            }

            if (uri == null) return null
            
            resolver.openOutputStream(uri)?.use { outputStream ->
                inputStream.use { it.copyTo(outputStream) }
            }
            uri
        } catch (e: Exception) {
            null
        }
    }

    private fun openPdf(uri: Uri) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Нет приложения для открытия PDF", Toast.LENGTH_LONG).show()
        }
    }

    private fun toggleUserType() {
        currentUserType = if (currentUserType == "PHYSICAL") "LEGAL" else "PHYSICAL"
        updateUIForType()
        clearErrors()
    }

    private fun updateUIForType() {
        val isBusiness = currentUserType == "LEGAL"
        if (isBusiness) {
            binding.tvRegSubtitle.text = getString(R.string.user_type_business)
            binding.btnSwitchToBusiness.text = getString(R.string.btn_for_regular)
            binding.layoutLegalOnly.visibility = View.VISIBLE
        } else {
            binding.tvRegSubtitle.text = getString(R.string.user_type_regular)
            binding.btnSwitchToBusiness.text = getString(R.string.btn_for_business)
            binding.layoutLegalOnly.visibility = View.GONE
        }
    }

    private fun updateFieldsVisibility(form: String) {
        if (form == "ИП") {
            binding.tilKpp.visibility = View.GONE
            binding.tilLegalAddress.visibility = View.GONE
            binding.etInn.filters = arrayOf(InputFilter.LengthFilter(12))
            // Очищаем скрытые поля
            binding.etKpp.text?.clear()
            binding.etLegalAddress.text?.clear()
        } else {
            binding.tilKpp.visibility = View.VISIBLE
            binding.tilLegalAddress.visibility = View.VISIBLE
            binding.etInn.filters = arrayOf(InputFilter.LengthFilter(10))
        }
        binding.tilInn.error = null
    }

    private fun clearErrors() {
        binding.tilName.error = null
        binding.tilPhone.error = null
        binding.tilInn.error = null
        binding.tilKpp.error = null
        binding.tilLegalAddress.error = null
        binding.tilPassword.error = null
        binding.tilLegalForm.error = null
    }

    private fun performRegistration() {
        val name = binding.etName.text.toString().trim()
        val phone = binding.etPhone.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()
        val legalForm = binding.actvLegalForm.text.toString().trim()
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
            if (legalForm.isEmpty()) {
                binding.tilLegalForm.error = getString(R.string.error_select_form)
                isValid = false
            }
            
            val isIp = legalForm == "ИП"
            
            if (!Validator.isValidInn(inn, "LEGAL", context = "REGISTER", legalForm = legalForm)) {
                binding.tilInn.error = if (isIp) getString(R.string.error_inn_ip) else getString(R.string.error_inn_org)
                isValid = false
            }
            
            if (!isIp) {
                if (!Validator.isValidKpp(kpp)) {
                    binding.tilKpp.error = getString(R.string.error_fill_kpp)
                    isValid = false
                }
                if (address.isEmpty()) {
                    binding.tilLegalAddress.error = getString(R.string.error_fill_address)
                    isValid = false
                }
            }
        }

        if (!binding.cbPrivacyPolicy.isChecked) {
            Toast.makeText(requireContext(), getString(R.string.error_privacy_policy_required), Toast.LENGTH_LONG).show()
            isValid = false
        }

        if (!isValid) {
            if (binding.cbPrivacyPolicy.isChecked) {
                Toast.makeText(requireContext(), "Регистрация не завершена. Проверьте ошибки в полях.", Toast.LENGTH_LONG).show()
            }
            return
        }

        val cleanPhone = phone.replace(Regex("[^\\d]"), "")
        val remoteIdValue = if (currentUserType == "LEGAL") inn else cleanPhone

        val newUser = User(
            remoteId = remoteIdValue,
            userType = currentUserType,
            fullName = name,
            phone = phone,
            password = password,
            inn = if (inn.isNotEmpty()) inn else null,
            kpp = if (currentUserType == "LEGAL" && legalForm != "ИП") kpp else null,
            legalAddress = if (currentUserType == "LEGAL" && legalForm != "ИП") address else null,
            legalForm = if (currentUserType == "LEGAL") legalForm else null
        )

        lifecycleScope.launch {
            try {
                viewModel.register(newUser)
                Toast.makeText(requireContext(), "Регистрация успешна!", Toast.LENGTH_SHORT).show()
                findNavController().navigateUp()
            } catch (e: Exception) {
                val message = if (e.message?.contains("уже зарегистрирован", ignoreCase = true) == true) {
                    "Пользователь с такими данными уже существует."
                } else {
                    e.message ?: "Сервис временно недоступен. Повторите операцию позднее."
                }
                Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
