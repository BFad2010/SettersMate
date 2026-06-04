package com.corp.bookmate.settermate.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.corp.bookmate.settermate.helpers.parseLeagueScheduleText
import com.corp.bookmate.settermate.helpers.parseTeamStandings
import com.corp.bookmate.settermate.service.LeagueContext
import com.corp.bookmate.settermate.service.LeagueData
import com.corp.bookmate.settermate.service.LeagueMapping
import com.corp.bookmate.settermate.service.SettersRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LeaguesViewModel(private val repo: SettersRepo) : ViewModel() {

    private val _uiState = MutableStateFlow<ScheduleUiState>(ScheduleUiState.Idle)
    val uiState: StateFlow<ScheduleUiState> = _uiState.asStateFlow()

    private val _navState = MutableStateFlow<NavUiState>(NavUiState.Standings)
    val navState: StateFlow<NavUiState> = _navState.asStateFlow()

    private val _leaguesState = MutableStateFlow<LeaguesUiState>(LeaguesUiState.Idle)
    val leaguesState: StateFlow<LeaguesUiState> = _leaguesState.asStateFlow()

    private val _selectedTeam = MutableStateFlow("")
    val selectedTeam: StateFlow<String> = _selectedTeam.asStateFlow()

    private val _leagueContext = MutableStateFlow<LeagueContext?>(null)
    val leagueContext: StateFlow<LeagueContext?> = _leagueContext.asStateFlow()

    fun setSelectedTeam(name: String) { _selectedTeam.value = name }
    fun setLeagueContext(ctx: LeagueContext) { _leagueContext.value = ctx }
    fun navigate(navItem: NavUiState) { _navState.value = navItem }
    fun clearLeagues() { _leaguesState.value = LeaguesUiState.Idle }
    fun clearLeagueData() { _uiState.value = ScheduleUiState.Idle }

    fun fetchLeaguesByDay(dayId: Int) {
        viewModelScope.launch(Dispatchers.Default) {
            _leaguesState.value = LeaguesUiState.Loading
            try {
                _leaguesState.value = LeaguesUiState.Success(repo.fetchLeaguesByDay(dayId))
            } catch (e: Exception) {
                _leaguesState.value = LeaguesUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun fetchSchedule(leagueDay: Int, leagueId: Int) {
        viewModelScope.launch(Dispatchers.Default) {
            _uiState.value = ScheduleUiState.Loading
            try {
                val response = repo.fetchSchedule(day = leagueDay, leagueId = leagueId)
                _uiState.value = ScheduleUiState.Success(
                    LeagueData(
                        standings = parseTeamStandings(response.html),
                        schedule = parseLeagueScheduleText(response.pdfText, response.courtMap),
                    )
                )
            } catch (e: Exception) {
                _uiState.value = ScheduleUiState.Error(e.message ?: "Unknown error")
            }
        }
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
