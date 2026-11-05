package com.example.semaphore.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.semaphore.presentation.sync.ContactSyncScreen
import com.example.semaphore.presentation.ui.theme.SemaphoreTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SemaphoreTheme {
                ContactSyncScreen()
            }
        }
    }
}
