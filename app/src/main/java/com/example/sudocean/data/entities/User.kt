package com.example.sudocean.data.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "users",
    indices = [Index(value = ["remoteId"], unique = true)]
)
data class User(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val remoteId: String = "", 
    val userType: String, // "PHYSICAL" или "LEGAL"
    val fullName: String,
    val phone: String,
    val password: String,
    val inn: String? = null,
    val kpp: String? = null,
    val legalAddress: String? = null,
    val legalForm: String? = null // Новое поле: ООО, ИП и т.д.
)
