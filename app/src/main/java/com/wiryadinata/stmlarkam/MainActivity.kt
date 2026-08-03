package com.wiryadinata.stmlarkam

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.wiryadinata.stmlarkam.ui.LarkamApp
import com.wiryadinata.stmlarkam.ui.theme.LarkamTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LarkamTheme {
                LarkamApp()
            }
        }
    }
}
