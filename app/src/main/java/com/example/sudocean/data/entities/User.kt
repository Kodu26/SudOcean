package com.example.sudocean.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userType: String, // "PHYSICAL" или "LEGAL"
    val fullName: String, // ФИО или Название компании
    val phone: String,
    val password: String,
    // Поля только для Юр. лиц (могут быть null для физ. лиц)
    val inn: String? = null,
    val kpp: String? = null,
    val legalAddress: String? = null
)
