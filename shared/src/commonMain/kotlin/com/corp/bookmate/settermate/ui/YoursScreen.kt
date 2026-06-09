package com.corp.bookmate.settermate.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.corp.bookmate.settermate.helpers.BackHandlerWrapper
import com.corp.bookmate.settermate.helpers.fuzzyTeamMatch
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun YoursScreen(
    modifier: Modifier = Modifier,
    viewModel: YoursViewModel = koinViewModel(),
) {
    val favorites by viewModel.favorites.collectAsState()
    val navState by viewModel.navState.collectAsState()
    val scheduleState by viewModel.scheduleState.collectAsState()
    val selectedTeam by viewModel.selectedTeam.collectAsState()
    val leagueContext by viewModel.leagueContext.collectAsState()

    BackHandlerWrapper(enabled = navState is YoursNavState.Schedule) {
        viewModel.navigateToList()
    }

    when (navState) {
        is YoursNavState.Schedule -> {
            when (val state = scheduleState) {
                is ScheduleUiState.Loading -> {
                    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(60.dp))
                    }
                }
                is ScheduleUiState.Success -> {
                    val ctx = leagueContext
                    TeamScheduleScreen(
                        modifier = modifier,
                        teamId = state.leagueData.schedule
                            .find { fuzzyTeamMatch(it.teamName, selectedTeam) }
                            ?.teamId ?: 0,
                        teamName = selectedTeam,
                        leagueName = ctx?.leagueName ?: "",
                        dayName = ctx?.dayName ?: "",
                        dayId = ctx?.dayId ?: 0,
                        leagueId = ctx?.leagueId ?: 0,
                        schedules = state.leagueData.schedule,
                        teamRecord = state.leagueData.standings.find { it.name == selectedTeam }?.record.orEmpty(),
                        onBack = { viewModel.navigateToList() },
                    )
                }
                is ScheduleUiState.Error -> {
                    ErrorView(
                        modifier = modifier,
                        onBack = { viewModel.navigateToList() },
                    )
                }
                else -> {}
            }
        }
        is YoursNavState.FavoritesList -> {
            if (favorites.isEmpty()) {
                Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(text = "No favorites yet", color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.SemiBold)
                        Text(text = "Star a team schedule to save it here", color = MaterialTheme.colorScheme.onBackground, fontStyle = FontStyle.Italic)
                    }
                }
            } else {
                Column(modifier = modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        modifier = Modifier.padding(vertical = 8.dp),
                        text = "Your Teams",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontStyle = FontStyle.Italic,
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    LazyColumn(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(favorites, key = { it.id }) { favorite ->
                            Card(
                                modifier = Modifier.fillMaxWidth().clickable { viewModel.loadFavoriteSchedule(favorite) },
                                elevation = CardDefaults.cardElevation(4.dp),
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    verticalAlignment = Alignment.Bottom,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Column(modifier = Modifier.fillMaxHeight(), verticalArrangement = Arrangement.Center) {
                                        Text(
                                            text = "${favorite.dayName} ${favorite.leagueName}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onBackground,
                                            fontStyle = FontStyle.Italic,
                                        )
                                        Text(
                                            text = favorite.teamName,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onBackground,
                                            fontWeight = FontWeight.SemiBold,
                                        )
                                    }
                                    Text(
                                        text = "View Schedule",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                                        fontWeight = FontWeight.SemiBold,
                                        fontStyle = FontStyle.Italic,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
