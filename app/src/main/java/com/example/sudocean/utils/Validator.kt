package com.example.sudocean.utils

object Validator {
    fun isValidPhone(phone: String): Boolean {
        val digits = phone.replace(Regex("[^\\d]"), "")
        return digits.length == 11
    }

    fun isValidInn(inn: String, userType: String): Boolean {
        return when (userType) {
            "LEGAL" -> inn.length == 10 && inn.all { it.isDigit() }
            "PHYSICAL" -> inn.length == 12 && inn.all { it.isDigit() }
            else -> false
        }
    }

    fun isValidKpp(kpp: String): Boolean {
        return kpp.length == 9 && kpp.all { it.isDigit() }
    }

    fun isValidPassword(password: String): Boolean {
        return password.length >= 4
    }
}
