package com.example.carcatalogue

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.carcatalogue.data.api.RetrofitClient
import com.example.carcatalogue.data.preferences.TokenManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var navController: NavController
    private lateinit var bottomNavigation: BottomNavigationView
    private lateinit var tokenManager: TokenManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tokenManager = TokenManager(this)
        
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        bottomNavigation = findViewById(R.id.bottomNavigation)
        bottomNavigation.setupWithNavController(navController)

        // Check authentication and load token
        lifecycleScope.launch {
            val token = tokenManager.getToken().first()
            if (token != null) {
                RetrofitClient.authToken = token
            }
        }

        // Hide bottom navigation on certain screens
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.loginFragment,
                R.id.registerFragment,
                R.id.carDetailFragment,
                R.id.contractDetailFragment   -> {
                    bottomNavigation.visibility = View.GONE // тут детальная инфа, нам не нужна навигационная  панель
                }
                else -> {
                    bottomNavigation.visibility = View.VISIBLE // во  всех остальных нужна
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}