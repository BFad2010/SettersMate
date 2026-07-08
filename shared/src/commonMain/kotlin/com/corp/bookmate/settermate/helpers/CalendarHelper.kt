package com.corp.bookmate.settermate.helpers

import androidx.compose.runtime.Composable

data class CalendarEvent(
    val title: String,
    val description: String,
    val dateString: String, // "M/D/YY"
    val timeString: String, // "H:MM"
)

@Composable
expect fun rememberCalendarLauncher(events: List<CalendarEvent>, onComplete: (Int) -> Unit): () -> Unit
