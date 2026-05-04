package com.example.sudocean

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.sudocean.databinding.ActivityMainBinding
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        
        val app = application as SudOceanApplication
        val navGraph = navController.navInflater.inflate(R.navigation.nav_graph)
        
        if (app.currentUserId != -1) {
            navGraph.setStartDestination(R.id.itemFragment)
            lifecycleScope.launch {
                val user = app.repository.getUserById(app.currentUserId)
                if (user != null) {
                    val exists = app.repository.verifyUserRemote(user)
                    if (!exists) {
                        app.clearUserSession()
                        navController.navigate(R.id.loginFragment)
                    }
                }
            }
        } else {
            navGraph.setStartDestination(R.id.loginFragment)
        }
        navController.graph = navGraph

        binding.bottomNav.setupWithNavController(navController)

        // Только управление видимостью. Синхронизацию иконок NavigationUI сделает сам.
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.loginFragment, 
                R.id.regFragment, 
                R.id.paymentProcessFragment, 
                R.id.paymentFragment -> {
                    binding.bottomNav.visibility = View.GONE
                }
                else -> {
                    binding.bottomNav.visibility = View.VISIBLE
                }
            }
        }
    }
}
