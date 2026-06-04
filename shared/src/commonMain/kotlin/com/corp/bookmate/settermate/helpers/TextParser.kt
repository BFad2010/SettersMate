package com.corp.bookmate.settermate.helpers

import com.corp.bookmate.settermate.service.LeagueSchedule
import com.corp.bookmate.settermate.service.PlayTime
import com.corp.bookmate.settermate.service.WeekSchedule
import kotlinx.datetime.LocalDate

fun parseLeagueScheduleText(
    rawText: String,
    courtMap: Map<String, String> = emptyMap()
): List<LeagueSchedule> {
    val lines = rawText.lines().map { it.trim() }.filter { it.isNotBlank() }

    val teamRegex = Regex("""^(\d+)\s+(.+?)\s+[A-Z][a-zA-Z]+\s+[A-Z][a-zA-Z]+$""")
    val teamMap = mutableMapOf<Int, String>()
    for (line in lines) {
        val match = teamRegex.find(line) ?: continue
        val id = match.groupValues[1].toInt()
        val name = match.groupValues[2].trim()
        teamMap[id] = name
    }

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
            val team1Name = teamMap[team1] ?: "Unknown"
            val team2Name = teamMap[team2] ?: "Unknown"
            val court = courtMap["${currentWeek}_${minOf(team1, team2)}_${maxOf(team1, team2)}"] ?: ""
            weeks[currentWeek!!]?.add(
                PlayTime(
                    time = time,
                    versusText = "$team1Name vs $team2Name",
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
    val maxLen = maxOf(a.length, b.length).coerceAtLeast(1)
    return levenshtein(a, b).toDouble() / maxLen <= 0.35
}

fun extractOpponent(teamsString: String, teamName: String): String {
    val parts = teamsString.split("vs").map { it.trim() }
    if (parts.size != 2) return teamsString
    val (team1, team2) = parts
    return if (fuzzyTeamMatch(team1, teamName)) team2 else team1
}
