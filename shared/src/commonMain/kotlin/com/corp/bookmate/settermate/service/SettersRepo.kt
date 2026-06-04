package com.corp.bookmate.settermate.service

import com.corp.bookmate.settermate.helpers.extractSchedulePdfUrl
import com.corp.bookmate.settermate.helpers.parseSchedulePdf
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.forms.submitForm
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.readRawBytes
import io.ktor.http.Parameters
import io.ktor.http.isSuccess

private const val BASE_URL = "https://www.cherrygrovesportscenter.com"

class SettersRepo(private val client: HttpClient) {

    suspend fun fetchLeaguesByDay(dayId: Int): List<LeagueMapping> {
        val response = client.get("$BASE_URL/Leagues/GetLeaguesByDay") {
            parameter("dayid", dayId)
        }
        if (!response.status.isSuccess()) throw Exception("HTTP ${response.status.value}")
        val body = response.bodyAsText()
        return body.split(",").mapNotNull { entry ->
            val parts = entry.trim().split("|")
            if (parts.size == 2) {
                val id = parts[0].trim().toIntOrNull() ?: return@mapNotNull null
                LeagueMapping(dayId = dayId, leagueName = parts[1].trim(), leagueId = id)
            } else null
        }
    }

    suspend fun fetchSchedule(day: Int, leagueId: Int): ScheduleFetchResult {
        val leagueResponse = client.submitForm(
            url = "$BASE_URL/Leagues/Schedules",
            formParameters = Parameters.build {
                append("leagueday", day.toString())
                append("lid", leagueId.toString())
            }
        )
        if (!leagueResponse.status.isSuccess()) throw Exception("HTTP ${leagueResponse.status.value}")
        val html = leagueResponse.bodyAsText()

        val scheduleUrl = extractSchedulePdfUrl(html) ?: ""
        val (pdfText, courtMap) = if (scheduleUrl.isNotEmpty()) {
            val pdfResponse = client.get(scheduleUrl)
            if (pdfResponse.status.isSuccess()) {
                parseSchedulePdf(pdfResponse.readRawBytes())
            } else Pair("", emptyMap())
        } else Pair("", emptyMap())

        return ScheduleFetchResult(html = html, pdfText = pdfText, courtMap = courtMap)
    }
}
