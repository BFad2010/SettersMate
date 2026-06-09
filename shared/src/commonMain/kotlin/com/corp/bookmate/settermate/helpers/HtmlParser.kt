package com.corp.bookmate.settermate.helpers

import com.corp.bookmate.settermate.service.TeamStanding
import com.fleeksoft.ksoup.Ksoup

fun parseTeamStandings(html: String): List<TeamStanding> {
    val document = Ksoup.parse(html)
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
                standings.add(TeamStanding(name = teamName, record = record))
            }
        }
    }
    return standings
}

fun extractSchedulePdfUrl(html: String): String? {
    val document = Ksoup.parse(html)
    val link = document.select("a:contains(View Schedule)").firstOrNull()
    val relativeUrl = link?.attr("href") ?: return null
    return "https://www.cherrygrovesportscenter.com$relativeUrl"
}
