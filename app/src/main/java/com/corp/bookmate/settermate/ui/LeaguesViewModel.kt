package com.corp.bookmate.settermate.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.corp.bookmate.settermate.helpers.parseLeagueScheduleText
import com.corp.bookmate.settermate.helpers.parseTeamStandings
import com.corp.bookmate.settermate.service.LeagueData
import com.corp.bookmate.settermate.service.LeagueMapping
import com.corp.bookmate.settermate.service.SettersRepo
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LeaguesViewModel @Inject constructor(
    val context: Context,
    val repo: SettersRepo,
) : ViewModel() {
    private val _uiState = MutableStateFlow<ScheduleUiState>(ScheduleUiState.Idle)
    val uiState: StateFlow<ScheduleUiState> = _uiState.asStateFlow()

    private val _navState = MutableStateFlow<NavUiState>(NavUiState.Standings)
    val navState: StateFlow<NavUiState> = _navState.asStateFlow()

    fun navigate(navItem: NavUiState) {
        _navState.value = navItem
    }

    fun fetchSchedule(
        leagueDay: Int,
        leagueId: Int
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = ScheduleUiState.Loading

            try {
                val response = repo.fetchSchedule(
                    day = leagueDay,
                    leagueId = leagueId
                )

                val html = response.first()
                val standings = parseTeamStandings(html ?: "")
                val schedule = parseLeagueScheduleText(response.last().orEmpty())
                val leagueData = LeagueData(
                    standings = standings,
                    schedule = schedule
                )

                _uiState.value = ScheduleUiState.Success(leagueData)

            } catch (e: Exception) {
                _uiState.value = ScheduleUiState.Error(
                    e.localizedMessage ?: "Unknown error"
                )
            }
        }
    }

    fun clearLeagueData() {
        _uiState.value = ScheduleUiState.Idle
    }

    fun getLeagueOptionsByDay(dayId: Int): List<String> {
        val leagueMap = loadLeagueMappings()
        return leagueMap.filter { it.dayId == dayId }.map { it.leagueName }
    }

    fun getLeagueId(dayId: Int, league: String): Int {
        val leagueMap = loadLeagueMappings()
        return leagueMap.find { it.dayId == dayId && it.leagueName == league }?.leagueId ?: 0
    }

    private fun loadLeagueMappings(): List<LeagueMapping> {
        val json = context.assets
            .open("leagues.json")
            .bufferedReader()
            .use { it.readText() }

        return Gson().fromJson(
            json,
            object : TypeToken<List<LeagueMapping>>() {}.type
        )
    }
}

sealed interface ScheduleUiState {
    object Idle : ScheduleUiState
    object Loading : ScheduleUiState
    data class Success(val leagueData: LeagueData) : ScheduleUiState
    data class Error(val message: String) : ScheduleUiState
}

sealed interface NavUiState {
    object Standings : NavUiState
    object Schedule : NavUiState
}