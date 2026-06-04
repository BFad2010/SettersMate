package com.corp.bookmate.settermate

import androidx.compose.ui.window.ComposeUIViewController
import com.corp.bookmate.settermate.ui.AppShell
import com.corp.bookmate.settermate.ui.theme.SetterMateTheme

fun MainViewController() = ComposeUIViewController {
    SetterMateTheme {
        AppShell()
    }
}
