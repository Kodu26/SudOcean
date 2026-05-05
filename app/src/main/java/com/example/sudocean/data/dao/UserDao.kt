package com.example.sudocean.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.sudocean.data.entities.User
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE phone = :phone AND password = :password AND userType = 'PHYSICAL' LIMIT 1")
    suspend fun loginPhysical(phone: String, password: String): User?

    @Query("SELECT * FROM users WHERE inn = :inn AND password = :password AND userType = 'LEGAL' LIMIT 1")
    suspend fun loginLegal(inn: String, password: String): User?

    // Поиск по remoteId (уникальный идентификатор: ИНН или чистый телефон)
    @Query("SELECT * FROM users WHERE remoteId = :login LIMIT 1")
    suspend fun getUserByLogin(login: String): User?

    // Поиск просто по телефону (для проверки уникальности)
    @Query("SELECT * FROM users WHERE phone = :phone LIMIT 1")
    suspend fun getUserByPhone(phone: String): User?

    @Insert(onConflict = OnConflictStrategy.ABORT) // Используем ABORT, чтобы Room выдавал ошибку при дубликате
    suspend fun register(user: User): Long

    @Update
    suspend fun updateUser(user: User)

    @Delete
    suspend fun deleteUser(user: User)

    @Query("DELETE FROM users WHERE id = :userId")
    suspend fun deleteUserById(userId: Int)

    @Query("DELETE FROM users")
    suspend fun deleteAllUsers()

    @Query("SELECT * FROM users WHERE id = :userId")
    fun getUserById(userId: Int): Flow<User?>

    @Query("SELECT * FROM users WHERE id = :userId")
    suspend fun getUserByIdDirect(userId: Int): User?
}
