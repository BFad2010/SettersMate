package com.corp.bookmate.settermate.helpers

import android.util.Log
import com.corp.bookmate.settermate.service.LeagueSchedule
import com.corp.bookmate.settermate.service.PlayTime
import com.corp.bookmate.settermate.service.TeamStanding
import com.corp.bookmate.settermate.service.WeekSchedule
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import com.tom_roush.pdfbox.text.TextPosition
import org.jsoup.Jsoup
import java.io.InputStream

fun parseTeamStandings(html: String): List<TeamStanding> {
    val document = Jsoup.parse(html)
    val standings = mutableListOf<TeamStanding>()

    val rows = document.select("table tr")

    for (row in rows) {
        val columns = row.select("td")

        if (columns.size == 2) {
            val teamName = columns[0].text().trim()
            val record = columns[1].text().trim()

            val isHeader = teamName.equals("Team Name", ignoreCase = true)
            val hasValidRecord = record.matches(Regex("\\d+\\s-\\s\\d+"))

            if (!isHeader && teamName.isNotEmpty() && hasValidRecord) {
                standings.add(
                    TeamStanding(
                        name = teamName,
                        record = record
                    )
                )
            }
        }
    }

    return standings
}

fun extractSchedulePdfUrl(html: String): String? {
    val document = Jsoup.parse(html)

    val link = document
        .select("a:contains(View Schedule)")
        .firstOrNull()

    val relativeUrl = link?.attr("href") ?: return null

    return "https://www.cherrygrovesportscenter.com$relativeUrl"
}

/**
 * Returns the flat PDF text (for team/week/game parsing) paired with a court map.
 * Court map keys: "weekNum_minTeamId_maxTeamId" → "Court N".
 * If position extraction fails, court map is empty and games still render without court labels.
 */
fun parseSchedulePdf(inputStream: InputStream): Pair<String, Map<String, String>> {
    val document = PDDocument.load(inputStream)

    // Pass 1 – flat text sorted by position so text elements on the same visual row
    // (e.g. team number, team name, captain) are guaranteed to appear on the same line
    // even when they live in separate PDF content streams.
    val text = PDFTextStripper().apply { setSortByPosition(true) }.getText(document)

    // Pass 2 – character positions for court column detection
    data class RawChar(val ch: String, val x: Float, val y: Float, val width: Float)

    val rawChars = mutableListOf<RawChar>()

    val posStripper = object : PDFTextStripper() {
        override fun writeString(text: String, textPositions: List<TextPosition>) {
            // Include spaces so "4 v 6" stays as one token with internal whitespace intact.
            // Spaces are trimmed from token edges later; only truly large column gaps split tokens.
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
    Log.d("CourtParse", "rawChars captured: ${rawChars.size}")

    // Group chars into lines (3 pt Y tolerance). Display space: small Y = top of page.
    data class Token(val text: String, val x: Float)
    data class Line(val tokens: List<Token>)

    val lines: List<Line> = rawChars
        .groupBy { (it.y / 3f).toInt() }
        .entries
        .sortedBy { it.key }           // ascending = top-to-bottom
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

            tokens.filter { it.text.isNotBlank() }
                .takeIf { it.isNotEmpty() }
                ?.let { Line(it) }
        }

    // Locate the court-header line ("Court 2  Court 4  Court 7  Court 8")
    val courtRegex = Regex("""(?i)court\s*(\d+)""")

    data class CourtBound(val label: String, val x: Float)

    var courts = listOf<CourtBound>()

    for (line in lines) {
        val fullText = line.tokens.joinToString(" ") { it.text }
        if (courtRegex.findAll(fullText).count() >= 2) {
            val headers = mutableListOf<CourtBound>()
            // Case A: "Court 8" is a single token
            for (tok in line.tokens) {
                val m = courtRegex.find(tok.text)
                if (m != null) headers.add(CourtBound("Court ${m.groupValues[1]}", tok.x))
            }
            // Case B: "Court" and "8" are separate tokens
            if (headers.size < 2) {
                headers.clear()
                var i = 0
                while (i < line.tokens.size) {
                    val tok = line.tokens[i]
                    if (tok.text.equals("Court", ignoreCase = true)
                        && i + 1 < line.tokens.size
                        && line.tokens[i + 1].text.matches(Regex("\\d+"))
                    ) {
                        headers.add(CourtBound("Court ${line.tokens[i + 1].text}", tok.x))
                        i += 2
                    } else i++
                }
            }
            if (headers.size >= 2) {
                courts = headers; break
            }
        }
    }
    Log.d("CourtParse", "courts detected: ${courts.map { "${it.label}@x=${it.x}" }}")

    fun courtForX(x: Float): String {
        if (courts.isEmpty()) return ""
        val sorted = courts.sortedBy { it.x }
        // Anything left of the first header belongs to the first court
        if (x < sorted.first().x) return sorted.first().label
        // Walk pairs: each court owns up to the midpoint between it and the next
        for (i in 0 until sorted.size - 1) {
            val midpoint = (sorted[i].x + sorted[i + 1].x) / 2f
            if (x <= midpoint) return sorted[i].label
        }
        // Right of all midpoints → last court
        return sorted.last().label
    }

    // Walk the positioned lines to build week → matchup → court
    val weekRegex2 = Regex("""^Week\s+(\d+)""", RegexOption.IGNORE_CASE)
    val timeRegex2 = Regex("""^\d{1,2}:\d{2}$""")
    val matchInline = Regex("""(\d+)\s*v\s*(\d+)""")
    val courtMap = mutableMapOf<String, String>()
    var currentWeek = 0

    for (line in lines) {
        val lineText = line.tokens.joinToString(" ") { it.text }
        val weekMatch = weekRegex2.find(lineText)
        if (weekMatch != null) {
            currentWeek = weekMatch.groupValues[1].toIntOrNull() ?: currentWeek
        }
        if (currentWeek == 0 || currentWeek > 7) continue

        var i = 0
        while (i < line.tokens.size) {
            val tok = line.tokens[i]
            // Matchup within a single token: "4 v 6"
            val inm = matchInline.find(tok.text)
            if (inm != null) {
                val t1 = inm.groupValues[1].toInt()
                val t2 = inm.groupValues[2].toInt()
                val court = courtForX(tok.x)
                Log.d("CourtParse", "week=$currentWeek match=${t1}v${t2} x=${tok.x} → $court")
                courtMap["${currentWeek}_${minOf(t1, t2)}_${maxOf(t1, t2)}"] = court
                i++; continue
            }
            // Matchup across three tokens: "4", "v", "6"
            if (i + 2 < line.tokens.size
                && tok.text.matches(Regex("\\d+"))
                && line.tokens[i + 1].text == "v"
                && line.tokens[i + 2].text.matches(Regex("\\d+"))
            ) {
                val t1 = tok.text.toInt()
                val t2 = line.tokens[i + 2].text.toInt()
                val court = courtForX(tok.x)
                Log.d("CourtParse", "week=$currentWeek match=${t1}v${t2} x=${tok.x} → $court")
                courtMap["${currentWeek}_${minOf(t1, t2)}_${maxOf(t1, t2)}"] = court
                i += 3; continue
            }
            i++
        }
    }

    Log.d("CourtParse", "courtMap final: $courtMap")
    return Pair(text, courtMap)
}

fun parseLeagueScheduleText(
    rawText: String,
    courtMap: Map<String, String> = emptyMap()
): List<LeagueSchedule> {

    val lines = rawText
        .lines()
        .map { it.trim() }
        .filter { it.isNotBlank() }

    // --- 1. Parse Teams ---

    val teamRegex = Regex("""^(\d+)\s+(.+?)\s+[A-Z][a-zA-Z]+\s+[A-Z][a-zA-Z]+$""")

    val teamMap = mutableMapOf<Int, String>()

    for (line in lines) {
        val match = teamRegex.find(line)
        if (match != null) {
            val id = match.groupValues[1].toInt()
            val name = match.groupValues[2].trim()
            teamMap[id] = name
        }
    }

    // --- 2. Parse Weeks + Matchups ---

    val weekRegex = Regex("""^Week\s+(\d+)""")
    val timeRegex = Regex("""\d{1,2}:\d{2}""")
    val matchupRegex = Regex("""(\d+)\s+v\s+(\d+)""")
    val dateRegex = Regex("""\d{1,2}/\d{1,2}/\d{2}""")

    val weeks = mutableMapOf<Int, MutableList<PlayTime>>()
    val collectedDates = mutableListOf<String>()

    var currentWeek: Int? = null

    for (line in lines) {

        val weekMatch = weekRegex.find(line)
        if (weekMatch != null) {
            currentWeek = weekMatch.groupValues[1].toInt()
            if (currentWeek <= 7) {
                weeks[currentWeek] = mutableListOf()
            }
        }

        if (dateRegex.matches(line)) {
            collectedDates.add(line)
            continue
        }

        if (currentWeek == null || currentWeek!! > 7) continue

        val time = timeRegex.find(line)?.value ?: continue

        // Use findAll so multiple matchups on the same line (e.g. "4 v 6  2 v 5") are all captured
        for (matchup in matchupRegex.findAll(line)) {
            val team1 = matchup.groupValues[1].toInt()
            val team2 = matchup.groupValues[2].toInt()
            val team1Name = teamMap[team1] ?: "Unknown"
            val team2Name = teamMap[team2] ?: "Unknown"

            val court =
                courtMap["${currentWeek}_${minOf(team1, team2)}_${maxOf(team1, team2)}"] ?: ""
            weeks[currentWeek!!]?.add(
                PlayTime(
                    time = time,
                    versusText = "$team1Name vs $team2Name",
                    team1Id = team1,
                    team2Id = team2,
                    court = court
                )
            )
        }

    }

    // --- Sort Dates Chronologically ---

    val formatter = java.time.format.DateTimeFormatter.ofPattern("M/d/yy")

    val sortedDates = collectedDates
        .mapNotNull {
            try {
                java.time.LocalDate.parse(it, formatter)
            } catch (e: Exception) {
                null
            }
        }
        .sorted()
        .map { it.format(formatter) }

    // --- 3. Build LeagueSchedule per Team ---

    return teamMap.map { (teamId, teamName) ->

        val weekSchedules = weeks
            .toSortedMap()
            .map { (weekNumber, playTimes) ->

                val date = sortedDates.getOrNull(weekNumber - 1) ?: ""

                val gamesForTeam = playTimes.filter {
                    it.team1Id == teamId || it.team2Id == teamId
                }

                WeekSchedule(
                    date = date,
                    weekNumber = weekNumber,
                    versus = gamesForTeam
                )
            }

        LeagueSchedule(
            teamId = teamId,
            teamName = teamName,
            weeks = weekSchedules
        )
    }.sortedBy { it.teamId }
}

fun normalizeTeamName(name: String): String =
    name.uppercase().replace(Regex("[^A-Z0-9]"), "")

fun levenshtein(a: String, b: String): Int {
    val dp = Array(a.length + 1) { IntArray(b.length + 1) }
    for (i in 0..a.length) dp[i][0] = i
    for (j in 0..b.length) dp[0][j] = j
    for (i in 1..a.length) {
        for (j in 1..b.length) {
            dp[i][j] = if (a[i - 1] == b[j - 1]) dp[i - 1][j - 1]
            else 1 + minOf(dp[i - 1][j], dp[i][j - 1], dp[i - 1][j - 1])
        }
    }
    return dp[a.length][b.length]
}

/**
 * Returns true if two team names are considered the same after normalization,
 * allowing for minor differences (e.g. "WATERRRRR!!!" vs "WATERRR???").
 * Uses Levenshtein similarity with a 35% edit-distance threshold on the
 * normalized (alphanumeric-only, uppercase) forms.
 */
fun fuzzyTeamMatch(name1: String, name2: String): Boolean {
    val a = normalizeTeamName(name1)
    val b = normalizeTeamName(name2)
    if (a == b) return true
    val maxLen = maxOf(a.length, b.length).coerceAtLeast(1)
    return levenshtein(a, b).toDouble() / maxLen <= 0.35
}

fun extractOpponent(teamsString: String, teamName: String): String {
    val parts = teamsString.split("vs").map { it.trim() }
    if (parts.size != 2) return teamsString
    val (team1, team2) = parts
    return if (fuzzyTeamMatch(team1, teamName)) team2 else team1
}
