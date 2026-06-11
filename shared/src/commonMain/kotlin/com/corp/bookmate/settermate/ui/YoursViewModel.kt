package com.corp.bookmate.settermate.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.corp.bookmate.settermate.data.FavoriteTeam
import com.corp.bookmate.settermate.data.FavoritesRepo
import com.corp.bookmate.settermate.helpers.parseLeagueScheduleText
import com.corp.bookmate.settermate.helpers.parseTeamStandings
import com.corp.bookmate.settermate.service.LeagueContext
import com.corp.bookmate.settermate.service.LeagueData
import com.corp.bookmate.settermate.service.SettersRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface YoursNavState {
    object FavoritesList : YoursNavState
    object Schedule : YoursNavState
}

class YoursViewModel(
    private val favoritesRepo: FavoritesRepo,
    private val scheduleRepo: SettersRepo,
) : ViewModel() {

    val favorites: StateFlow<List<FavoriteTeam>> = favoritesRepo.getAllFavorites()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _navState = MutableStateFlow<YoursNavState>(YoursNavState.FavoritesList)
    val navState: StateFlow<YoursNavState> = _navState.asStateFlow()

    private val _scheduleState = MutableStateFlow<ScheduleUiState>(ScheduleUiState.Idle)
    val scheduleState: StateFlow<ScheduleUiState> = _scheduleState.asStateFlow()

    private val _selectedTeam = MutableStateFlow("")
    val selectedTeam: StateFlow<String> = _selectedTeam.asStateFlow()

    private val _leagueContext = MutableStateFlow<LeagueContext?>(null)
    val leagueContext: StateFlow<LeagueContext?> = _leagueContext.asStateFlow()

    fun loadFavoriteSchedule(favorite: FavoriteTeam) {
        _selectedTeam.value = favorite.teamName
        _leagueContext.value = LeagueContext(
            dayName = favorite.dayName,
            dayId = favorite.dayId.toInt(),
            leagueName = favorite.leagueName,
            leagueId = favorite.leagueId.toInt(),
        )
        _navState.value = YoursNavState.Schedule
        viewModelScope.launch(Dispatchers.Default) {
            _scheduleState.value = ScheduleUiState.Loading
            try {
                val result = scheduleRepo.fetchSchedule(favorite.dayId.toInt(), favorite.leagueId.toInt())
                val standings = parseTeamStandings(result.html)
                val standingsNames = standings.map { it.name }
                val schedule = result.pdfResults.flatMap { (pdfText, courtMap) ->
                    parseLeagueScheduleText(
                        rawText = pdfText,
                        courtMap = courtMap,
                        standingsNames = standingsNames,
                    )
                }
                _scheduleState.value = ScheduleUiState.Success(
                    LeagueData(standings = standings, schedule = schedule)
                )
            } catch (e: Exception) {
                _scheduleState.value = ScheduleUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    private val _tourneyUrl = MutableStateFlow<String?>(null)
    val tourneyUrl: StateFlow<String?> = _tourneyUrl.asStateFlow()

    fun clearTourneyUrl() { _tourneyUrl.value = null }

    fun fetchTourneyScheduleUrl(day: Int, leagueId: Int) {
        viewModelScope.launch(Dispatchers.Default) {
            try {
                _tourneyUrl.value = scheduleRepo.fetchFirstPdfUrl(day, leagueId)
            } catch (_: Exception) {}
        }
    }

    fun navigateToList() {
        _navState.value = YoursNavState.FavoritesList
        _scheduleState.value = ScheduleUiState.Idle
    }

    fun toggleFavorite(
        teamName: String,
        leagueName: String,
        dayName: String,
        dayId: Int,
        leagueId: Int,
    ) {
        viewModelScope.launch(Dispatchers.Default) {
            favoritesRepo.toggleFavorite(teamName, leagueName, dayName, dayId, leagueId)
        }
    }
}
