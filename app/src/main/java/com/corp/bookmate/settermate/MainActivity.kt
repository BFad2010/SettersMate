package com.corp.bookmate.settermate

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.corp.bookmate.settermate.ui.AppShell
import com.corp.bookmate.settermate.ui.theme.SetterMateTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SetterMateTheme {
                AppShell()
            }
        }
    }
}
