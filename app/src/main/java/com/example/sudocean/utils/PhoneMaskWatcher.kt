package com.example.sudocean.utils

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText

class PhoneMaskWatcher(private val editText: EditText) : TextWatcher {
    private var isUpdating = false

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

    override fun afterTextChanged(s: Editable?) {
        if (isUpdating || s == null) return

        val str = s.toString()
        var digits = str.replace(Regex("[^\\d]"), "")
        
        if (digits.isEmpty()) {
            isUpdating = true
            editText.setText("")
            isUpdating = false
            return
        }

        // Если первая цифра 7 или 8, убираем её, так как +7 добавится маской
        if (digits.length >= 1 && (digits[0] == '7' || digits[0] == '8')) {
            digits = digits.substring(1)
        }
        
        if (digits.length > 10) digits = digits.substring(0, 10)

        val res = StringBuilder("+7")
        if (digits.isNotEmpty()) {
            res.append(" (")
            for (i in digits.indices) {
                res.append(digits[i])
                when (i) {
                    2 -> if (i < digits.length - 1) res.append(") ")
                    5 -> if (i < digits.length - 1) res.append("-")
                    7 -> if (i < digits.length - 1) res.append("-")
                }
            }
        }

        val result = res.toString()
        if (result != str) {
            isUpdating = true
            editText.setText(result)
            editText.setSelection(result.length)
            isUpdating = false
        }
    }
}
