package com.corp.bookmate.settermate.helpers

import androidx.compose.runtime.Composable

@Composable
actual fun BackHandlerWrapper(enabled: Boolean, onBack: () -> Unit) {
    // iOS uses swipe-to-go-back gesture natively; the back button in the UI handles this.
}
