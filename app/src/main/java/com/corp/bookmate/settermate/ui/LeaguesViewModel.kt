package com.corp.bookmate.settermate.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.corp.bookmate.settermate.helpers.parseLeagueScheduleText
import com.corp.bookmate.settermate.helpers.parseTeamStandings
import com.corp.bookmate.settermate.service.LeagueData
import com.corp.bookmate.settermate.service.LeagueMapping
import com.corp.bookmate.settermate.service.SettersRepo
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

    private val _leaguesState = MutableStateFlow<LeaguesUiState>(LeaguesUiState.Idle)
    val leaguesState: StateFlow<LeaguesUiState> = _leaguesState.asStateFlow()

    private val _selectedTeam = MutableStateFlow("")
    val selectedTeam: StateFlow<String> = _selectedTeam.asStateFlow()

    fun setSelectedTeam(name: String) {
        _selectedTeam.value = name
    }

    fun navigate(navItem: NavUiState) {
        _navState.value = navItem
    }

    fun fetchLeaguesByDay(dayId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            _leaguesState.value = LeaguesUiState.Loading
            try {
                val leagues = repo.fetchLeaguesByDay(dayId)
                _leaguesState.value = LeaguesUiState.Success(leagues)
            } catch (e: Exception) {
                _leaguesState.value = LeaguesUiState.Error(e.localizedMessage ?: "Unknown error")
            }
        }
    }

    fun clearLeagues() {
        _leaguesState.value = LeaguesUiState.Idle
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

                val standings = parseTeamStandings(response.html)
                val schedule = parseLeagueScheduleText(response.pdfText, response.courtMap)
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
}

sealed interface LeaguesUiState {
    object Idle : LeaguesUiState
    object Loading : LeaguesUiState
    data class Success(val leagues: List<LeagueMapping>) : LeaguesUiState
    data class Error(val message: String) : LeaguesUiState
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