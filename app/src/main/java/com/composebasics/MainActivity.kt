package com.composebasics

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.composebasics.ui.navigation.AppNavHost
import com.composebasics.ui.theme.ComposeBasicsTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Runs during the splash delay, well before the user can submit the login form.
        val container = (application as ComposeBasicsApp).container
        lifecycleScope.launch { container.authRepository.ensureDemoUser() }
        setContent {
            ComposeBasicsTheme {
                AppNavHost()
            }
        }
    }
}
