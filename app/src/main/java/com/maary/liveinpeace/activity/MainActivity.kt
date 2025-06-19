package com.maary.liveinpeace.activity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.maary.liveinpeace.ui.screen.SettingsScreen
import com.maary.liveinpeace.ui.theme.LiveInPeaceTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LiveInPeaceTheme {
                SettingsScreen()
            }
        }
    }
}