package com.corp.bookmate.settermate

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.window.ComposeUIViewController
import com.corp.bookmate.settermate.ui.AppShell
import com.corp.bookmate.settermate.ui.theme.SetterMateTheme

// openUrl is supplied by the Swift layer, which calls the modern
// UIApplication.shared.open(_:options:completionHandler:) directly.
// This avoids the Kotlin/Native binding ambiguity with the deprecated
// UIApplication.openURL(_:) selector that iOS 18 force-returns false on.
fun MainViewController(openUrl: (String) -> Unit) = ComposeUIViewController {
    val iosUriHandler = object : UriHandler {
        override fun openUri(uri: String) = openUrl(uri)
    }
    SetterMateTheme {
        CompositionLocalProvider(LocalUriHandler provides iosUriHandler) {
            AppShell()
        }
    }
}
