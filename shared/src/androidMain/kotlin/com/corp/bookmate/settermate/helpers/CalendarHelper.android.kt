package com.corp.bookmate.settermate.helpers

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import java.util.TimeZone

private val CALENDAR_PERMISSIONS = arrayOf(
    Manifest.permission.READ_CALENDAR,
    Manifest.permission.WRITE_CALENDAR,
)

@Composable
actual fun rememberCalendarLauncher(events: List<CalendarEvent>, onComplete: (Int) -> Unit): () -> Unit {
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (results.values.all { it }) {
            onComplete(insertEvents(context.contentResolver, events))
        }
    }

    return remember(events) {
        {
            val hasPermissions = CALENDAR_PERMISSIONS.all {
                ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
            }
            if (hasPermissions) {
                onComplete(insertEvents(context.contentResolver, events))
            } else {
                permissionLauncher.launch(CALENDAR_PERMISSIONS)
            }
        }
    }
}

private fun insertEvents(resolver: android.content.ContentResolver, events: List<CalendarEvent>): Int {
    val calendarId = queryPrimaryCalendarId(resolver) ?: return 0
    val tz = TimeZone.getDefault()
    var count = 0
    events.forEach { event ->
        val startMillis = parseEventMillis(event.dateString, event.timeString, tz) ?: return@forEach
        val values = ContentValues().apply {
            put(CalendarContract.Events.CALENDAR_ID, calendarId)
            put(CalendarContract.Events.TITLE, event.title)
            put(CalendarContract.Events.DESCRIPTION, event.description)
            put(CalendarContract.Events.DTSTART, startMillis)
            put(CalendarContract.Events.DTEND, startMillis + 3_600_000L)
            put(CalendarContract.Events.EVENT_TIMEZONE, tz.id)
        }
        resolver.insert(CalendarContract.Events.CONTENT_URI, values)
        count++
    }
    return count
}

private fun queryPrimaryCalendarId(resolver: android.content.ContentResolver): Long? {
    val projection = arrayOf(CalendarContract.Calendars._ID, CalendarContract.Calendars.IS_PRIMARY)
    // Prefer a Google account calendar so events show in Google Calendar
    resolver.query(
        CalendarContract.Calendars.CONTENT_URI, projection,
        "${CalendarContract.Calendars.VISIBLE} = 1 AND ${CalendarContract.Calendars.ACCOUNT_TYPE} = 'com.google'",
        null,
        "${CalendarContract.Calendars.IS_PRIMARY} DESC",
    )?.use { cursor ->
        if (cursor.moveToFirst()) {
            return cursor.getLong(cursor.getColumnIndexOrThrow(CalendarContract.Calendars._ID))
        }
    }
    // Fall back to any visible calendar
    resolver.query(
        CalendarContract.Calendars.CONTENT_URI, projection,
        "${CalendarContract.Calendars.VISIBLE} = 1", null,
        "${CalendarContract.Calendars.IS_PRIMARY} DESC",
    )?.use { cursor ->
        if (cursor.moveToFirst()) {
            return cursor.getLong(cursor.getColumnIndexOrThrow(CalendarContract.Calendars._ID))
        }
    }
    return null
}

private fun parseEventMillis(dateString: String, timeString: String, tz: TimeZone): Long? {
    return try {
        val dParts = dateString.split("/")
        val month = dParts[0].toInt()
        val day = dParts[1].toInt()
        val year = 2000 + dParts[2].toInt()
        val tParts = timeString.split(":")
        val hour = tParts[0].toInt().let { if (it < 12) it + 12 else it }
        val minute = tParts[1].toInt()
        val cal = java.util.Calendar.getInstance(tz).apply {
            set(year, month - 1, day, hour, minute, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        cal.timeInMillis
    } catch (_: Exception) {
        null
    }
}
