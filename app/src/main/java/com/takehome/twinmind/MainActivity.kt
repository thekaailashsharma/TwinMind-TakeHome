package com.takehome.twinmind

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.google.firebase.auth.FirebaseAuth
import com.takehome.twinmind.core.designsystem.theme.TwinMindTheme
import com.takehome.twinmind.navigation.TwinMindNavHost
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TwinMindTheme {
                TwinMindNavHost(
                    isLoggedIn = FirebaseAuth.getInstance().currentUser != null,
                )
            }
        }
    }
}
