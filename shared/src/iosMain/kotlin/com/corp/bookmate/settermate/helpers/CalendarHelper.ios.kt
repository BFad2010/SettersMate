package com.corp.bookmate.settermate.helpers

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.cinterop.ExperimentalForeignApi
import platform.EventKit.EKEntityType
import platform.EventKit.EKEvent
import platform.EventKit.EKEventStore
import platform.EventKit.EKSpan
import platform.Foundation.NSCalendar
import platform.Foundation.NSCalendarIdentifierGregorian
import platform.Foundation.NSDate
import platform.Foundation.NSDateComponents
import platform.Foundation.NSOperationQueue

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun rememberCalendarLauncher(events: List<CalendarEvent>, onComplete: (Int) -> Unit): () -> Unit {
    val store = remember { EKEventStore() }
    return remember(events) {
        {
            store.requestAccessToEntityType(EKEntityType.EKEntityTypeEvent) { granted, _ ->
                if (granted) {
                    val added = insertEvents(store, events)
                    NSOperationQueue.mainQueue.addOperationWithBlock { onComplete(added) }
                }
            }
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun insertEvents(store: EKEventStore, events: List<CalendarEvent>): Int {
    val cal = NSCalendar(calendarIdentifier = NSCalendarIdentifierGregorian)
    var count = 0
    events.forEach { event ->
        val startDate = parseNSDate(cal, event.dateString, event.timeString) ?: return@forEach
        val endDate = NSDate(timeIntervalSinceReferenceDate = startDate.timeIntervalSinceReferenceDate + 3600.0)
        val ekEvent = EKEvent.eventWithEventStore(store)
        ekEvent.title = event.title
        ekEvent.notes = event.description
        ekEvent.startDate = startDate
        ekEvent.endDate = endDate
        ekEvent.calendar = store.defaultCalendarForNewEvents ?: return@forEach
        try {
            store.saveEvent(ekEvent, span = EKSpan.EKSpanThisEvent, commit = false, error = null)
            count++
        } catch (_: Exception) {}
    }
    if (count > 0) try { store.commit(null) } catch (_: Exception) {}
    return count
}

@OptIn(ExperimentalForeignApi::class)
private fun parseNSDate(calendar: NSCalendar, dateString: String, timeString: String) = try {
    val dParts = dateString.split("/")
    val month = dParts[0].toLong()
    val day = dParts[1].toLong()
    val year = 2000L + dParts[2].toLong()
    val tParts = timeString.split(":")
    val hour = tParts[0].toLong().let { if (it < 12L) it + 12L else it }
    val minute = tParts[1].toLong()
    val components = NSDateComponents()
    components.year = year
    components.month = month
    components.day = day
    components.hour = hour
    components.minute = minute
    components.second = 0
    calendar.dateFromComponents(components)
} catch (_: Exception) {
    null
}
