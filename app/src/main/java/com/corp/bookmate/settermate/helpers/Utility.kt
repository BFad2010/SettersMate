package com.corp.bookmate.settermate.helpers

import com.corp.bookmate.settermate.service.LeagueSchedule
import com.corp.bookmate.settermate.service.PlayTime
import com.corp.bookmate.settermate.service.TeamStanding
import com.corp.bookmate.settermate.service.WeekSchedule
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
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

fun parseSchedulePdf(inputStream: InputStream): String {
    val document = PDDocument.load(inputStream)
    val stripper = PDFTextStripper()
    val text = stripper.getText(document)
    document.close()
    return text
}

fun parseLeagueScheduleText(rawText: String): List<LeagueSchedule> {

    val lines = rawText
        .lines()
        .map { it.trim() }
        .filter { it.isNotBlank() }

    // ----------------------------
    // 1️⃣ Parse Teams
    // ----------------------------

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

    // ----------------------------
    // 2️⃣ Parse Weeks + Matchups
    // ----------------------------

    val weekRegex = Regex("""^Week\s+(\d+)""")
    val matchRegex = Regex("""(\d{1,2}:\d{2}).*?(\d+)\s+v\s+(\d+)""")
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

        val match = matchRegex.find(line)
        if (match != null && currentWeek != null && currentWeek <= 7) {

            val time = match.groupValues[1]
            val team1 = match.groupValues[2].toInt()
            val team2 = match.groupValues[3].toInt()
            val team1Name = teamMap[team1] ?: "Unknown"
            val team2Name = teamMap[team2] ?: "Unknown"

            weeks[currentWeek]?.add(
                PlayTime(
                    time = time,
                    versusText = "$team1Name vs $team2Name",
                    team1Id = team1,
                    team2Id = team2
                )
            )
        }

        if (dateRegex.matches(line)) {
            collectedDates.add(line)
        }
    }

    // ----------------------------
    // ✅ FIX: Sort Dates Chronologically
    // ----------------------------

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

    // ----------------------------
    // 3️⃣ Build LeagueSchedule per Team
    // ----------------------------

    return teamMap.map { (teamId, teamName) ->

        val weekSchedules = weeks
            .toSortedMap() // ensure week order
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

fun extractOpponent(teamsString: String, teamName: String): String {
    val parts = teamsString.split("vs")
        .map { it.trim() }

    if (parts.size != 2) return teamsString

    val team1 = parts[0]
    val team2 = parts[1]

    return when (team1) {
        teamName -> team2
        else -> team1
    }
}