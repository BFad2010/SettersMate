package com.corp.bookmate.settermate.service

import com.corp.bookmate.settermate.helpers.extractSchedulePdfUrl
import com.corp.bookmate.settermate.helpers.parseSchedulePdf

class SettersRepo(
    private val api: SettersApi
) {

    suspend fun fetchSchedule(day: Int, leagueId: Int): List<String?> {
        val leagueInfo = api.getLeagueInfo(
            leagueDay = day,
            leagueId = leagueId,
        )

        if (leagueInfo.isSuccessful) {
            return leagueInfo.body()?.let { info ->
                val scheduleUrl = extractSchedulePdfUrl(info) ?: ""
                val schedulePdf = api.downloadSchedule(scheduleUrl)
                if (schedulePdf.isSuccessful) {
                    val inputStream = schedulePdf.body()?.byteStream()
                    inputStream?.let {
                        val parsePdf = parseSchedulePdf(inputStream)
                        listOf(leagueInfo.body(), parsePdf)
                    }
                } else {
                    listOf(leagueInfo.body())
                }
            } ?: listOf("")
        } else {
            throw retrofit2.HttpException(leagueInfo)
        }
    }
}