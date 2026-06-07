package com.example.sudocean.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.sudocean.data.dao.CartDao
import com.example.sudocean.data.dao.OrderDao
import com.example.sudocean.data.dao.OrderItemDao
import com.example.sudocean.data.dao.ProductDao
import com.example.sudocean.data.dao.UserDao
import com.example.sudocean.data.entities.CartItem
import com.example.sudocean.data.entities.Order
import com.example.sudocean.data.entities.OrderItem
import com.example.sudocean.data.entities.Product
import com.example.sudocean.data.entities.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [User::class, Product::class, CartItem::class, Order::class, OrderItem::class],
    version = 11, // Увеличено до 10 для добавления поля legalForm в User
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun productDao(): ProductDao
    abstract fun cartDao(): CartDao
    abstract fun orderDao(): OrderDao
    abstract fun orderItemDao(): OrderItemDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "sudocean_database"
                )
                .fallbackToDestructiveMigration()
                .addCallback(DatabaseCallback(context))
                .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val context: Context
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        populateDatabase(database.productDao())
                    }
                }
            }

            suspend fun populateDatabase(productDao: ProductDao) {
                val products = listOf(
                    Product(id = 1, name = "Спасательный жилет", description = "Сертифицированный жилет", price = 2500.0, category = "Безопасность", stock = 15),
                    Product(id = 2, name = "Якорь складной", description = "Вес 5кг", price = 8500.0, category = "Снаряжение", stock = 5),
                    Product(id = 3, name = "Морской бинокль", description = "7x50, водонепроницаемый", price = 12000.0, category = "Оптика", stock = 0),
                    Product(id = 4, name = "Трос капроновый", description = "Диаметр 12мм", price = 3200.0, category = "Снаряжение", stock = 50),
                    Product(id = 5, name = "Навигатор Garmin Marine", description = "GPS, экран 7 дюймов", price = 45000.0, category = "Электроника", stock = 3),
                    Product(id = 6, name = "Огнетушитель морской", description = "Порошковый, объем 2л", price = 1800.0, category = "Безопасность", stock = 10)
                )
                for (product in products) {
                    productDao.insertProduct(product)
                }
            }
        }
    }
}
