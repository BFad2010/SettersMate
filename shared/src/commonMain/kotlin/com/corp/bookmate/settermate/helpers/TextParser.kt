package com.corp.bookmate.settermate.helpers

import com.corp.bookmate.settermate.service.LeagueSchedule
import com.corp.bookmate.settermate.service.PlayTime
import com.corp.bookmate.settermate.service.WeekSchedule
import kotlinx.datetime.LocalDate

/**
 * @param rawText       Position-sorted PDF text from the platform parser.
 * @param courtMap      Court-label map (Android only; empty on iOS).
 * @param standingsNames Team names from the HTML standings — used as a safety net
 *                       when the PDF roster section is missing, has no captain names,
 *                       or the iOS text extractor garbles spacing.
 */
fun parseLeagueScheduleText(
    rawText: String,
    courtMap: Map<String, String> = emptyMap(),
    standingsNames: List<String> = emptyList(),
): List<LeagueSchedule> {
    val lines = rawText.lines().map { it.trim() }.filter { it.isNotBlank() }

    // ── Pass 1: captain-based regexes ────────────────────────────────────────
    // Try most specific (most captain words) first so the lazy (.+?) captures
    // only the true team name and not the captain tokens.
    // [A-Z][a-zA-Z'-]+ accepts hyphenated / apostrophe names.
    val capWord = """[A-Z][a-zA-Z'-]+"""
    val teamRegex4 = Regex("""^(\d+)\s+(.+?)\s+$capWord\s+$capWord\s+$capWord\s+$capWord$""")
    val teamRegex3 = Regex("""^(\d+)\s+(.+?)\s+$capWord\s+$capWord\s+$capWord$""")
    val teamRegex2 = Regex("""^(\d+)\s+(.+?)\s+$capWord\s+$capWord$""")
    val teamMap = mutableMapOf<Int, String>()

    for (line in lines) {
        // Collect every regex candidate for this line (from most to fewest captain words)
        val candidates = listOfNotNull(
            teamRegex4.find(line),
            teamRegex3.find(line),
            teamRegex2.find(line),
        )
        if (candidates.isEmpty()) continue
        val id = candidates.first().groupValues[1].toIntOrNull() ?: continue

        // When standings names are available, pick the candidate whose captured name
        // best matches a known standings entry. This prevents the lazy (.+?) from
        // capturing only the first word of a multi-word team name (e.g. "Bad" instead
        // of "Bad News Bears") when the captain's name is also all-capitalized words.
        val best = if (standingsNames.isNotEmpty()) {
            candidates.maxByOrNull { m ->
                val norm = normalizeTeamName(m.groupValues[2].trim())
                standingsNames.maxOfOrNull { sName ->
                    val ns = normalizeTeamName(sName)
                    when {
                        norm == ns              -> 1000          // exact match
                        ns.startsWith(norm)     -> norm.length   // longer prefix = better
                        else                    -> 0
                    }
                } ?: 0
            } ?: candidates.first()
        } else {
            candidates.first()
        }

        val name = best.groupValues[2].trim()
        if (id > 0 && name.isNotBlank()) teamMap[id] = name
    }

    // ── Pass 2: standings-assisted rescue ────────────────────────────────────
    // Applies only when we have standings names and some of them are missing
    // from teamMap (PDF roster had no captains, iOS extraction was garbled, etc.).
    // Searches only lines before the first "Week" header to stay in the roster
    // section and avoid false matches on schedule lines.
    if (standingsNames.isNotEmpty()) {
        val firstWeekIdx = lines.indexOfFirst { it.startsWith("Week ") }
            .takeIf { it >= 0 } ?: lines.size
        val rosterLines = lines.take(firstWeekIdx)

        val missingNames = standingsNames.filter { sName ->
            teamMap.values.none { mapped -> fuzzyTeamMatch(mapped, sName) }
        }

        if (missingNames.isNotEmpty()) {
            val entryRegex = Regex("""^(\d{1,2})\s+(.+)$""")
            val matchupCheck = Regex("""\d+\s+v\s+\d+""")

            for (line in rosterLines) {
                if (matchupCheck.containsMatchIn(line)) continue
                if (line.matches(Regex("""\d{1,2}:\d{2}.*"""))) continue

                val m = entryRegex.find(line) ?: continue
                val id = m.groupValues[1].toIntOrNull() ?: continue
                if (id < 1 || id > 30 || teamMap.containsKey(id)) continue

                val rawName = m.groupValues[2].trim()
                val normRaw = normalizeTeamName(rawName)
                if (normRaw.length < 3) continue

                // Find the best matching missing standings name
                val best = missingNames.find { sName ->
                    val ns = normalizeTeamName(sName)
                    normRaw == ns ||
                    normRaw.startsWith(ns) ||
                    ns.startsWith(normRaw) ||
                    (ns.length >= 5 && normRaw.length >= 5 &&
                        levenshtein(normRaw, ns).toDouble() / maxOf(normRaw.length, ns.length) <= 0.45)
                }
                if (best != null) teamMap[id] = best
            }
        }
    }

    // ── Schedule parsing ─────────────────────────────────────────────────────
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
            if (currentWeek <= 7) weeks[currentWeek] = mutableListOf()
        }
        if (dateRegex.matches(line)) { collectedDates.add(line); continue }
        if (currentWeek == null || currentWeek!! > 7) continue
        val time = timeRegex.find(line)?.value ?: continue
        for (matchup in matchupRegex.findAll(line)) {
            val team1 = matchup.groupValues[1].toInt()
            val team2 = matchup.groupValues[2].toInt()
            val court = courtMap["${currentWeek}_${minOf(team1, team2)}_${maxOf(team1, team2)}"] ?: ""
            weeks[currentWeek!!]?.add(
                PlayTime(
                    time = time,
                    versusText = "${teamMap[team1] ?: "Unknown"} vs ${teamMap[team2] ?: "Unknown"}",
                    team1Id = team1,
                    team2Id = team2,
                    court = court,
                )
            )
        }
    }

    val sortedDates = collectedDates
        .mapNotNull { parseShortDate(it) }
        .sorted()
        .map { formatShortDate(it) }

    return teamMap.map { (teamId, teamName) ->
        val weekSchedules = weeks.entries.sortedBy { it.key }.map { (weekNumber, playTimes) ->
            WeekSchedule(
                date = sortedDates.getOrNull(weekNumber - 1) ?: "",
                weekNumber = weekNumber,
                versus = playTimes.filter { it.team1Id == teamId || it.team2Id == teamId },
            )
        }
        LeagueSchedule(teamId = teamId, teamName = teamName, weeks = weekSchedules)
    }.sortedBy { it.teamId }
}

private fun parseShortDate(input: String): LocalDate? {
    val parts = input.split("/")
    if (parts.size != 3) return null
    val month = parts[0].toIntOrNull() ?: return null
    val day = parts[1].toIntOrNull() ?: return null
    val rawYear = parts[2].toIntOrNull() ?: return null
    val year = if (rawYear < 100) 2000 + rawYear else rawYear
    return try { LocalDate(year, month, day) } catch (_: Exception) { null }
}

private fun formatShortDate(date: LocalDate): String =
    "${date.monthNumber}/${date.dayOfMonth}/${(date.year % 100).toString().padStart(2, '0')}"

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

fun fuzzyTeamMatch(name1: String, name2: String): Boolean {
    val a = normalizeTeamName(name1)
    val b = normalizeTeamName(name2)
    if (a == b) return true
    // Prefix check: handles PDFs where captain names leaked into the extracted
    // team name (e.g. "FOURPLAY" vs "FOURPLAYJOHNSMITH").
    if (a.length >= 3 && b.startsWith(a)) return true
    if (b.length >= 3 && a.startsWith(b)) return true
    val maxLen = maxOf(a.length, b.length).coerceAtLeast(1)
    return levenshtein(a, b).toDouble() / maxLen <= 0.35
}

fun extractOpponent(teamsString: String, teamName: String): String {
    val parts = teamsString.split("vs").map { it.trim() }
    if (parts.size != 2) return teamsString
    val (team1, team2) = parts
    return if (fuzzyTeamMatch(team1, teamName)) team2 else team1
}
