package com.corp.bookmate.settermate.helpers

import androidx.compose.runtime.Composable

@Composable
expect fun BackHandlerWrapper(enabled: Boolean = true, onBack: () -> Unit)
