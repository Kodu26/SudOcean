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
    version = 4, // Подняли до 4, так как User изменился
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
                    Product(name = "Спасательный жилет", description = "Сертифицированный жилет для морских судов", price = 2500.0),
                    Product(name = "Якорь складной", description = "Вес 5кг, нержавеющая сталь", price = 8500.0),
                    Product(name = "Морской бинокль", description = "7x50, водонепроницаемый, с сеткой", price = 12000.0),
                    Product(name = "Трос капроновый", description = "Диаметр 12мм, длина 50м", price = 3200.0),
                    Product(name = "Навигатор Garmin Marine", description = "Карты глубин, GPS, экран 7 дюймов", price = 45000.0),
                    Product(name = "Огнетушитель морской", description = "Порошковый, объем 2л", price = 1800.0)
                )
                for (product in products) {
                    productDao.insertProduct(product)
                }
            }
        }
    }
}
