package com.corp.bookmate.settermate.service

import com.corp.bookmate.settermate.helpers.extractSchedulePdfUrl
import com.corp.bookmate.settermate.helpers.parseSchedulePdf

class SettersRepo(
    private val api: SettersApi
) {

    suspend fun fetchLeaguesByDay(dayId: Int): List<LeagueMapping> {
        val response = api.getLeaguesByDay(dayId)
        if (!response.isSuccessful) throw retrofit2.HttpException(response)
        val body = response.body() ?: return emptyList()
        // Format: "1848|Coed 2 C,1844|Coed 4 B,..."
        return body.split(",").mapNotNull { entry ->
            val parts = entry.trim().split("|")
            if (parts.size == 2) {
                val id = parts[0].trim().toIntOrNull() ?: return@mapNotNull null
                LeagueMapping(dayId = dayId, leagueName = parts[1].trim(), leagueId = id)
            } else null
        }
    }

    suspend fun fetchSchedule(day: Int, leagueId: Int): ScheduleFetchResult {
        val leagueInfo = api.getLeagueInfo(leagueDay = day, leagueId = leagueId)
        if (!leagueInfo.isSuccessful) throw retrofit2.HttpException(leagueInfo)

        val html = leagueInfo.body() ?: ""
        val scheduleUrl = extractSchedulePdfUrl(html) ?: ""
        val schedulePdf = api.downloadSchedule(scheduleUrl)

        val (pdfText, courtMap) = if (schedulePdf.isSuccessful) {
            schedulePdf.body()?.byteStream()?.let { parseSchedulePdf(it) } ?: Pair("", emptyMap())
        } else Pair("", emptyMap())

        return ScheduleFetchResult(html = html, pdfText = pdfText, courtMap = courtMap)
    }
}