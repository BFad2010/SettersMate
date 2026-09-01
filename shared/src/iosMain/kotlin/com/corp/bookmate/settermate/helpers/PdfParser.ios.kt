package com.corp.bookmate.settermate.helpers

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ULongVar
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import platform.zlib.Z_OK
import platform.zlib.uncompress

// ---------------------------------------------------------------------------
// iOS PDF text extractor
//
// PDFKit.page.string returns empty for this site's PDFs because they use
// TrueType fonts with WinAnsiEncoding but no ToUnicode CMaps.
//
// We directly parse the raw FlateDecode content streams:
//   1. Search for stream/endstream markers in raw ByteArray (binary-safe).
//   2. Decompress each stream with zlib.uncompress (direct C call, no ObjC).
//   3. Parse BT/ET blocks for Tm (position) + TJ/Tj (text) operators.
//   4. Group fragments by y-row, sort by x → reading-order lines.
// ---------------------------------------------------------------------------

private data class TextFragment(val text: String, val x: Double, val y: Double)

@OptIn(ExperimentalForeignApi::class)
actual fun parseSchedulePdf(bytes: ByteArray): Pair<String, Map<String, String>> {
    val allFragments = extractFragments(bytes)
    if (allFragments.isEmpty()) return Pair("", emptyMap())

    // Group into lines by Y bucket (y increases upward in PDF coords).
    // sortedByDescending gives reading order (top of page first).
    val lines = allFragments
        .groupBy { (it.y / 5.0).toInt() }
        .entries
        .sortedByDescending { it.key }
        .mapNotNull { (_, row) ->
            row.sortedBy { it.x }.takeIf { it.isNotEmpty() }
        }

    val text = lines.mapNotNull { row ->
        row.joinToString(" ") { it.text.trim() }.trim().takeIf { it.isNotBlank() }
    }.joinToString("\n")

    val courtMap = buildCourtMap(lines)
    return Pair(text, courtMap)
}

private fun buildCourtMap(lines: List<List<TextFragment>>): Map<String, String> {
    val courtRegex = Regex("""(?i)court\s*(\d+)""")

    data class CourtBound(val label: String, val x: Double)

    var courts = listOf<CourtBound>()
    for (row in lines) {
        val fullText = row.joinToString(" ") { it.text }
        if (courtRegex.findAll(fullText).count() >= 2) {
            val headers = row.mapNotNull { frag ->
                val m = courtRegex.find(frag.text) ?: return@mapNotNull null
                CourtBound("Court ${m.groupValues[1]}", frag.x)
            }
            if (headers.size >= 2) { courts = headers; break }
        }
    }
    if (courts.isEmpty()) return emptyMap()

    fun courtForX(x: Double): String {
        val sorted = courts.sortedBy { it.x }
        if (x < sorted.first().x) return sorted.first().label
        for (i in 0 until sorted.size - 1) {
            val midpoint = (sorted[i].x + sorted[i + 1].x) / 2.0
            if (x <= midpoint) return sorted[i].label
        }
        return sorted.last().label
    }

    val weekRegex = Regex("""^Week\s+(\d+)""", RegexOption.IGNORE_CASE)
    val matchInline = Regex("""(\d+)\s*v\s*(\d+)""")
    val courtMap = mutableMapOf<String, String>()
    var currentWeek = 0

    for (row in lines) {
        val lineText = row.joinToString(" ") { it.text }
        val weekMatch = weekRegex.find(lineText)
        if (weekMatch != null) {
            currentWeek = weekMatch.groupValues[1].toIntOrNull() ?: currentWeek
            continue
        }
        if (currentWeek == 0 || currentWeek > MAX_SCHEDULE_WEEKS) continue

        for (frag in row) {
            val m = matchInline.find(frag.text) ?: continue
            val t1 = m.groupValues[1].toInt()
            val t2 = m.groupValues[2].toInt()
            courtMap["${currentWeek}_${minOf(t1, t2)}_${maxOf(t1, t2)}"] = courtForX(frag.x)
        }
    }

    return courtMap
}

private fun extractFragments(bytes: ByteArray): List<TextFragment> {
    val allFragments = mutableListOf<TextFragment>()
    val endCRLF = "\r\nendstream".encodeToByteArray()
    val endLF   = "\nendstream".encodeToByteArray()

    var pos = 0
    while (pos < bytes.size) {
        val streamResult = bytes.nextStreamStart(pos) ?: break
        val dataStart = streamResult

        val endCrlfIdx = bytes.indexOf(endCRLF, dataStart)
        val endLfIdx   = bytes.indexOf(endLF,   dataStart)
        val dataEnd = when {
            endCrlfIdx >= 0 && (endLfIdx < 0 || endCrlfIdx <= endLfIdx) -> endCrlfIdx
            endLfIdx >= 0 -> endLfIdx
            else -> break
        }
        pos = dataEnd + 1

        val streamBytes  = bytes.copyOfRange(dataStart, dataEnd)
        val decompressed = decompressZlib(streamBytes) ?: continue
        val content      = decompressed.decodeToString(throwOnInvalidSequence = false)

        if ("TJ" !in content && "Tj" !in content) continue
        allFragments += parseContentStream(content)
    }
    return allFragments
}

// Returns the index right after "stream\r\n" / "stream\n", skipping any
// "stream" that is part of "endstream".
private fun ByteArray.nextStreamStart(from: Int): Int? {
    val crlfMarker = "stream\r\n".encodeToByteArray()
    val lfMarker   = "stream\n".encodeToByteArray()
    val endTag     = "end".encodeToByteArray()
    var pos = from
    while (pos < size) {
        val ci = indexOf(crlfMarker, pos)
        val li = indexOf(lfMarker,   pos)
        val (idx, len) = when {
            ci >= 0 && (li < 0 || ci <= li) -> Pair(ci, crlfMarker.size)
            li >= 0                          -> Pair(li, lfMarker.size)
            else                             -> return null
        }
        // Skip if this "stream" is the tail of "endstream"
        val isEndStream = idx >= 3 &&
            this[idx - 3] == endTag[0] &&
            this[idx - 2] == endTag[1] &&
            this[idx - 1] == endTag[2]
        if (!isEndStream) return idx + len
        pos = idx + 1
    }
    return null
}

private fun ByteArray.indexOf(needle: ByteArray, from: Int = 0): Int {
    if (needle.isEmpty() || from + needle.size > size) return -1
    outer@ for (i in from..size - needle.size) {
        for (j in needle.indices) if (this[i + j] != needle[j]) continue@outer
        return i
    }
    return -1
}

// ---------------------------------------------------------------------------
// Parse a decompressed PDF content stream into positioned text fragments.
// ---------------------------------------------------------------------------
private fun parseContentStream(content: String): List<TextFragment> {
    val result = mutableListOf<TextFragment>()
    val ops = Regex(
        """([\d.+-]+)\s+([\d.+-]+)\s+([\d.+-]+)\s+([\d.+-]+)\s+([\d.+-]+)\s+([\d.+-]+)\s+Tm""" +
        """|\[(.*?)]\s*TJ""" +
        """|\(((?:[^\\)]|\\.)*)\)\s*Tj""",
        setOf(RegexOption.DOT_MATCHES_ALL)
    )
    val btEt = Regex("""BT\b(.*?)\bET\b""", setOf(RegexOption.DOT_MATCHES_ALL))
    for (block in btEt.findAll(content)) {
        var curX = 0.0; var curY = 0.0
        for (op in ops.findAll(block.groupValues[1])) {
            when {
                op.groupValues[1].isNotEmpty() -> {
                    curX = op.groupValues[5].toDoubleOrNull() ?: curX
                    curY = op.groupValues[6].toDoubleOrNull() ?: curY
                }
                op.groupValues[7].isNotEmpty() -> {
                    val text = parseTjArray(op.groupValues[7])
                    if (text.isNotBlank()) result += TextFragment(text, curX, curY)
                }
                op.groupValues[8].isNotEmpty() -> {
                    val text = decodePdfString(op.groupValues[8])
                    if (text.isNotBlank()) result += TextFragment(text, curX, curY)
                }
            }
        }
    }
    return result
}

private fun parseTjArray(array: String): String {
    val sb = StringBuilder()
    for (m in Regex("""\(((?:[^\\)]|\\.)*)\)""").findAll(array))
        sb.append(decodePdfString(m.groupValues[1]))
    return sb.toString()
}

private fun decodePdfString(raw: String): String {
    val sb = StringBuilder()
    var i = 0
    while (i < raw.length) {
        if (raw[i] == '\\' && i + 1 < raw.length) {
            when (raw[i + 1]) {
                'n'  -> { sb.append('\n'); i += 2 }
                'r'  -> { sb.append('\r'); i += 2 }
                't'  -> { sb.append('\t'); i += 2 }
                '\\' -> { sb.append('\\'); i += 2 }
                '('  -> { sb.append('(');  i += 2 }
                ')'  -> { sb.append(')');  i += 2 }
                else -> {
                    val octal = raw.drop(i + 1).takeWhile { it in '0'..'7' }.take(3)
                    if (octal.isNotEmpty()) {
                        sb.append(octal.toInt(8).toChar()); i += 1 + octal.length
                    } else { sb.append(raw[i + 1]); i += 2 }
                }
            }
        } else { sb.append(raw[i]); i++ }
    }
    return sb.toString()
}

// ---------------------------------------------------------------------------
// Decompress a zlib (FlateDecode) stream using the system zlib C API.
// NSData.decompressedDataUsingAlgorithm silently fails for this PDF type;
// uncompress() is a direct C call with no ObjC bridging overhead.
// ---------------------------------------------------------------------------
@OptIn(ExperimentalForeignApi::class)
private fun decompressZlib(input: ByteArray): ByteArray? {
    if (input.isEmpty()) return null
    // Schedule PDFs are small — 10× headroom is plenty; cap at 10 MB.
    val outSize = minOf(maxOf(input.size * 10, 65_536), 10 * 1024 * 1024)
    val output = ByteArray(outSize)
    val actualSize = memScoped {
        val destLen = alloc<ULongVar>()
        destLen.value = outSize.toULong()
        val ret = input.usePinned { inp ->
            output.usePinned { out ->
                uncompress(
                    out.addressOf(0).reinterpret(),
                    destLen.ptr,
                    inp.addressOf(0).reinterpret(),
                    input.size.toULong()
                )
            }
        }
        if (ret == Z_OK) destLen.value.toInt() else -1
    }
    return if (actualSize > 0) output.copyOf(actualSize) else null
}
