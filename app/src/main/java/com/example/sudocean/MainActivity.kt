package com.example.sudocean

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.sudocean.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        
        // Настраиваем граф навигации программно для авто-входа
        val navGraph = navController.navInflater.inflate(R.navigation.nav_graph)
        val app = application as SudOceanApplication
        
        if (app.currentUserId != -1) {
            // Если пользователь авторизован, меняем точку входа на каталог
            navGraph.setStartDestination(R.id.itemFragment)
        } else {
            navGraph.setStartDestination(R.id.loginFragment)
        }
        navController.graph = navGraph

        // Привязываем нижнее меню
        binding.bottomNav.setupWithNavController(navController)

        // Скрываем/показываем меню
        navController.addOnDestinationChangedListener { _, destination, _ ->
            if (destination.id == R.id.loginFragment || destination.id == R.id.regFragment || destination.id == R.id.paymentProcessFragment) {
                binding.bottomNav.visibility = View.GONE
            } else {
                binding.bottomNav.visibility = View.VISIBLE
            }
        }
    }

    fun showBottomNav() {
        binding.bottomNav.visibility = View.VISIBLE
    }
}
