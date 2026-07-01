package com.corp.bookmate.settermate.helpers

import com.corp.bookmate.settermate.service.TeamStanding
import com.fleeksoft.ksoup.Ksoup

// Parses team standings from the schedule page HTML.
// The page may contain multiple level sections in one table, separated by header rows
// (rows with more than 2 columns). On multi-level pages we collect only rows that fall
// inside a named section, which naturally excludes the phantom "0 - 100" duplicate rows
// that appear after the last section on some pages.
fun parseTeamStandings(html: String): List<TeamStanding> {
    val document = Ksoup.parse(html)
    val recordRegex = Regex("""\d+\s-\s\d+""")
    val allRows = document.select("table tr")

    val hasLevelHeaders = allRows.any { it.select("td").size > 2 }

    if (!hasLevelHeaders) {
        // Single-level page — include all valid rows.
        return allRows.mapNotNull { row ->
            val cols = row.select("td")
            if (cols.size != 2) return@mapNotNull null
            val name = cols[0].text().trim()
            val record = cols[1].text().trim()
            if (name.equals("Team Name", ignoreCase = true) || name.isEmpty() || !recordRegex.matches(record)) return@mapNotNull null
            TeamStanding(name = name, record = record)
        }
    }

    // Multi-level page — collect from all sections, stopping at each new section header.
    // Rows that appear after the last section boundary (phantom duplicates) are excluded
    // because we only collect while inside a known section.
    val standings = mutableListOf<TeamStanding>()
    var inSection = false
    for (row in allRows) {
        val cols = row.select("td")
        if (cols.size > 2) { inSection = true; continue }
        if (!inSection) continue
        if (cols.size != 2) continue
        val name = cols[0].text().trim()
        val record = cols[1].text().trim()
        if (name.equals("Team Name", ignoreCase = true) || name.isEmpty() || !recordRegex.matches(record)) continue
        standings.add(TeamStanding(name = name, record = record))
    }
    return standings
}

fun extractSchedulePdfUrls(html: String): List<String> {
    val document = Ksoup.parse(html)
    return document.select("a:contains(View Schedule)").mapNotNull { link ->
        val relativeUrl = link.attr("href").takeIf { it.isNotEmpty() } ?: return@mapNotNull null
        "https://www.cherrygrovesportscenter.com$relativeUrl"
    }.distinct()
}
