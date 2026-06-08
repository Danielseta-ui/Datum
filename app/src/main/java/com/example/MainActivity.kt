package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import com.example.data.DatumDatabase
import com.example.data.DatumRepository
import com.example.ui.screens.DatumAppScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.DatumViewModel
import com.example.ui.viewmodel.DatumViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Core Infrastructure Setup
        val database = DatumDatabase.getDatabase(applicationContext)
        val repository = DatumRepository(database)
        val factory = DatumViewModelFactory(application, repository)
        val viewModel = ViewModelProvider(this, factory)[DatumViewModel::class.java]

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    DatumAppScreen(viewModel = viewModel)
                }
            }
        }
    }
}

