package com.example.sudocean.ui

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import coil.load
import com.example.sudocean.R
import com.example.sudocean.SudOceanApplication
import com.example.sudocean.databinding.FragmentPaymentBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class PaymentFragment : Fragment() {

    private var _binding: FragmentPaymentBinding? = null
    private val binding get() = _binding!!

    private var paymentLink: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPaymentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val amount = arguments?.getDouble("total_amount") ?: 0.0
        paymentLink = arguments?.getString("payment_link")
        val paymentQr = arguments?.getString("payment_qr")
        
        binding.tvPaymentAmount.text = String.format(Locale.getDefault(), "К оплате: %.2f ₽", amount)

        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, 7)
        val deadlineFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        binding.tvPaymentDeadline.text = "Оплатите не позднее: ${deadlineFormat.format(calendar.time)}"

        if (paymentQr != null) {
            displayPaymentData(paymentQr)
        } else {
            refreshPaymentData(amount)
        }

        binding.btnPayBank.setOnClickListener {
            paymentLink?.let { link ->
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link))
                    startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Не удалось открыть ссылку", Toast.LENGTH_SHORT).show()
                }
            } ?: run {
                Toast.makeText(requireContext(), "Ссылка для оплаты отсутствует", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnDone.setOnClickListener {
            // ИСПРАВЛЕНИЕ: Используем popBackStack до главного экрана, чтобы не ломать меню
            if (!findNavController().popBackStack(R.id.itemFragment, false)) {
                findNavController().navigate(R.id.itemFragment)
            }
        }
    }

    private fun displayPaymentData(qrData: String) {
        binding.btnPayBank.isEnabled = true
        binding.tvQrHint.text = "Отсканируйте QR или нажмите кнопку выше"
        
        if (qrData.startsWith("http")) {
            binding.ivQrCode.load(qrData)
        } else {
            try {
                val decodedString = Base64.decode(qrData, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
                binding.ivQrCode.setImageBitmap(bitmap)
            } catch (e: Exception) {
                binding.ivQrCode.setImageResource(android.R.drawable.ic_dialog_alert)
            }
        }
    }

    private fun refreshPaymentData(amount: Double) {
        val app = requireActivity().application as SudOceanApplication
        val repository = app.repository

        binding.btnPayBank.isEnabled = false
        binding.tvQrHint.text = "Загрузка данных оплаты..."

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val user = repository.getUserById(app.currentUserId) ?: return@launch
                val response = repository.sendOrderTo1C(user, com.example.sudocean.data.entities.Order(userId = user.id, date = 0, totalAmount = amount, status = ""), emptyList(), emptyList())
                
                if (response != null) {
                    paymentLink = response.payment_link
                    response.payment_qr?.let { displayPaymentData(it) }
                } else {
                    binding.tvQrHint.text = "Ошибка загрузки QR"
                }
            } catch (e: Exception) {
                binding.tvQrHint.text = "Ошибка: ${e.message}"
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
