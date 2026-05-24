package com.example.sudocean.utils

object Validator {
    fun isValidPhone(phone: String): Boolean {
        val digits = phone.replace(Regex("[^\\d]"), "")
        return digits.length == 11
    }

    /**
     * Валидация ИНН.
     * @param inn Строка ИНН
     * @param context "LOGIN" или "REGISTER"
     * @param userType "PHYSICAL" (Обычный) или "LEGAL" (Бизнес)
     * @param legalForm Форма (для регистрации), например "ИП"
     */
    fun isValidInn(inn: String, userType: String, context: String = "REGISTER", legalForm: String? = null): Boolean {
        if (!inn.all { it.isDigit() }) return false
        
        return if (context == "LOGIN") {
            if (userType == "LEGAL") {
                // Для входа бизнес-пользователя: 10 (организация) или 12 (ИП) цифр
                inn.length == 10 || inn.length == 12
            } else {
                // Обычный пользователь входит по телефону, ИНН не проверяем здесь
                true
            }
        } else {
            // Для регистрации
            if (userType == "LEGAL") {
                if (legalForm == "ИП") inn.length == 12 else inn.length == 10
            } else {
                // Обычному пользователю ИНН не обязателен, но если есть - 12 цифр
                inn.isEmpty() || inn.length == 12
            }
        }
    }

    fun isValidKpp(kpp: String): Boolean {
        return kpp.length == 9 && kpp.all { it.isDigit() }
    }

    fun isValidPassword(password: String): Boolean {
        return password.length >= 6
    }
}
