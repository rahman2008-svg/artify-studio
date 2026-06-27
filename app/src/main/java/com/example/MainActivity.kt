package com.example

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.data.database.AppDatabase
import com.example.data.database.DesignRepository
import com.example.ui.screens.CanvasScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.DesignerViewModel
import com.example.ui.viewmodel.DesignerViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 1. Initialize SQLite database & repository offline
        val database = AppDatabase.getDatabase(this)
        val repository = DesignRepository(database.designDraftDao())

        // 2. Initialize ViewModel
        val factory = DesignerViewModelFactory(repository)
        val viewModel = ViewModelProvider(this, factory)[DesignerViewModel::class.java]

        // 3. Request Storage Permissions on older Android releases
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
            val requestPermissionLauncher = registerForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions()
            ) { /* permission callback */ }
            
            requestPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            )
        }

        setContent {
            MyApplicationTheme {
                val navController = rememberNavController()
                
                NavHost(
                    navController = navController,
                    startDestination = "home"
                ) {
                    composable("home") {
                        HomeScreen(
                            viewModel = viewModel,
                            onNavigateToCanvas = {
                                navController.navigate("canvas")
                            }
                        )
                    }
                    composable("canvas") {
                        CanvasScreen(
                            viewModel = viewModel,
                            onNavigateBack = {
                                navController.popBackStack()
                            }
                        )
                    }
                }
            }
        }
    }
}
