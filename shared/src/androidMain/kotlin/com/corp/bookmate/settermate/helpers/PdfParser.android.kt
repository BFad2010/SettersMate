package com.corp.bookmate.settermate.helpers

import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import com.tom_roush.pdfbox.text.TextPosition

actual fun parseSchedulePdf(bytes: ByteArray): Pair<String, Map<String, String>> {
    val document = PDDocument.load(bytes.inputStream())

    val text = PDFTextStripper().apply { setSortByPosition(true) }.getText(document)

    data class RawChar(val ch: String, val x: Float, val y: Float, val width: Float)
    val rawChars = mutableListOf<RawChar>()

    val posStripper = object : PDFTextStripper() {
        override fun writeString(text: String, textPositions: List<TextPosition>) {
            for (tp in textPositions) {
                val ch = tp.unicode ?: continue
                rawChars.add(RawChar(ch, tp.x, tp.y, tp.width))
            }
            super.writeString(text, textPositions)
        }
    }
    posStripper.setSortByPosition(true)
    posStripper.getText(document)
    document.close()

    data class Token(val text: String, val x: Float)
    data class Line(val tokens: List<Token>)

    val lines: List<Line> = rawChars
        .groupBy { (it.y / 3f).toInt() }
        .entries
        .sortedBy { it.key }
        .mapNotNull { (_, chars) ->
            val sorted = chars.sortedBy { it.x }
            val tokens = mutableListOf<Token>()
            val sb = StringBuilder()
            var tokenX = sorted.first().x
            var prevEndX = sorted.first().x - 1f
            for (rc in sorted) {
                val gap = rc.x - prevEndX
                if (gap > 8f && sb.isNotEmpty()) {
                    val t = sb.toString().trim()
                    if (t.isNotBlank()) tokens.add(Token(t, tokenX))
                    sb.clear()
                    tokenX = rc.x
                }
                sb.append(rc.ch)
                prevEndX = rc.x + rc.width
            }
            val last = sb.toString().trim()
            if (last.isNotBlank()) tokens.add(Token(last, tokenX))
            tokens.filter { it.text.isNotBlank() }.takeIf { it.isNotEmpty() }?.let { Line(it) }
        }

    val courtRegex = Regex("""(?i)court\s*(\d+)""")
    data class CourtBound(val label: String, val x: Float)
    var courts = listOf<CourtBound>()

    for (line in lines) {
        val fullText = line.tokens.joinToString(" ") { it.text }
        if (courtRegex.findAll(fullText).count() >= 2) {
            val headers = mutableListOf<CourtBound>()
            for (tok in line.tokens) {
                val m = courtRegex.find(tok.text)
                if (m != null) headers.add(CourtBound("Court ${m.groupValues[1]}", tok.x))
            }
            if (headers.size < 2) {
                headers.clear()
                var i = 0
                while (i < line.tokens.size) {
                    val tok = line.tokens[i]
                    if (tok.text.equals("Court", ignoreCase = true) && i + 1 < line.tokens.size
                        && line.tokens[i + 1].text.matches(Regex("\\d+"))
                    ) {
                        headers.add(CourtBound("Court ${line.tokens[i + 1].text}", tok.x))
                        i += 2
                    } else i++
                }
            }
            if (headers.size >= 2) { courts = headers; break }
        }
    }

    fun courtForX(x: Float): String {
        if (courts.isEmpty()) return ""
        val sorted = courts.sortedBy { it.x }
        if (x < sorted.first().x) return sorted.first().label
        for (i in 0 until sorted.size - 1) {
            val midpoint = (sorted[i].x + sorted[i + 1].x) / 2f
            if (x <= midpoint) return sorted[i].label
        }
        return sorted.last().label
    }

    val weekRegex2 = Regex("""^Week\s+(\d+)""", RegexOption.IGNORE_CASE)
    val matchInline = Regex("""(\d+)\s*v\s*(\d+)""")
    val courtMap = mutableMapOf<String, String>()
    var currentWeek = 0

    for (line in lines) {
        val lineText = line.tokens.joinToString(" ") { it.text }
        val weekMatch = weekRegex2.find(lineText)
        if (weekMatch != null) currentWeek = weekMatch.groupValues[1].toIntOrNull() ?: currentWeek
        if (currentWeek == 0 || currentWeek > 7) continue

        var i = 0
        while (i < line.tokens.size) {
            val tok = line.tokens[i]
            val inm = matchInline.find(tok.text)
            if (inm != null) {
                val t1 = inm.groupValues[1].toInt()
                val t2 = inm.groupValues[2].toInt()
                courtMap["${currentWeek}_${minOf(t1, t2)}_${maxOf(t1, t2)}"] = courtForX(tok.x)
                i++; continue
            }
            if (i + 2 < line.tokens.size
                && tok.text.matches(Regex("\\d+"))
                && line.tokens[i + 1].text == "v"
                && line.tokens[i + 2].text.matches(Regex("\\d+"))
            ) {
                val t1 = tok.text.toInt()
                val t2 = line.tokens[i + 2].text.toInt()
                courtMap["${currentWeek}_${minOf(t1, t2)}_${maxOf(t1, t2)}"] = courtForX(tok.x)
                i += 3; continue
            }
            i++
        }
    }

    return Pair(text, courtMap)
}
