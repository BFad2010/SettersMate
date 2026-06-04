package com.corp.bookmate.settermate.helpers

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.dataWithBytes
import platform.PDFKit.PDFDocument

@OptIn(ExperimentalForeignApi::class)
actual fun parseSchedulePdf(bytes: ByteArray): Pair<String, Map<String, String>> {
    val nsData: NSData = bytes.usePinned { pinned ->
        NSData.dataWithBytes(pinned.addressOf(0), bytes.size.toULong())
    }

    val pdfDoc = PDFDocument(data = nsData)
        ?: return Pair("", emptyMap())

    val sb = StringBuilder()
    for (i in 0 until pdfDoc.pageCount.toInt()) {
        val page = pdfDoc.pageAtIndex(i.toULong()) ?: continue
        sb.append(page.string ?: "")
        sb.append("\n")
    }

    // Court position detection is not implemented on iOS (PDFKit lacks character-level
    // positioning APIs as straightforward as PDFBox). Schedules display without court labels.
    return Pair(sb.toString(), emptyMap())
}
