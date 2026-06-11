package com.corp.bookmate.settermate.service

data class TeamStanding(
    val name: String,
    val record: String
)

data class LeagueMapping(
    val dayId: Int,
    val leagueName: String,
    val leagueId: Int
)

data class LeagueData(
    val standings: List<TeamStanding>,
    val schedule: List<LeagueSchedule>,
)

data class LeagueSchedule(
    val teamId: Int,
    val teamName: String,
    val weeks: List<WeekSchedule>,
)

data class WeekSchedule(
    val date: String,
    val weekNumber: Int,
    val versus: List<PlayTime>,
)

data class PlayTime(
    val time: String,
    val versusText: String,
    val team1Id: Int,
    val team2Id: Int,
    val court: String = "",
)

data class ScheduleFetchResult(
    val html: String,
    val pdfResults: List<Pair<String, Map<String, String>>>,
)

data class LeagueContext(
    val dayName: String,
    val dayId: Int,
    val leagueName: String,
    val leagueId: Int,
)

val daysMap = mapOf(
    "Sunday" to 7,
    "Monday" to 1,
    "Tuesday" to 2,
    "Wednesday" to 3,
    "Thursday" to 4,
    "Friday" to 5,
)
