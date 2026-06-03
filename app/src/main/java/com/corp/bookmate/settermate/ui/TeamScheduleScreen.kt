package com.corp.bookmate.settermate.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.corp.bookmate.settermate.R
import com.corp.bookmate.settermate.helpers.extractOpponent
import com.corp.bookmate.settermate.service.LeagueSchedule

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TeamScheduleScreen(
    teamId: Int,
    teamName: String,
    leagueName: String,
    dayName: String,
    dayId: Int,
    leagueId: Int,
    schedules: List<LeagueSchedule>,
    teamRecord: String,
    modifier: Modifier = Modifier,
    yoursViewModel: YoursViewModel = hiltViewModel(),
    onBack: () -> Unit,
) {
    val favorites = yoursViewModel.favorites.collectAsStateWithLifecycle()
    val isFavorite = favorites.value.any { it.leagueId == leagueId && it.teamName == teamName }

    val teamSchedule = schedules.firstOrNull { it.teamId == teamId }
    BackHandler { onBack() }

    if (teamSchedule == null) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    Icon(
                        modifier = Modifier
                            .size(24.dp)
                            .clickable { onBack() },
                        painter = painterResource(R.drawable.back_arrow),
                        tint = MaterialTheme.colorScheme.onBackground,
                        contentDescription = null,
                    )
                }
                Text(
                    modifier = Modifier.weight(3f),
                    text = stringResource(R.string.schedule, teamName),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
            Text(
                text = stringResource(R.string.no_schedule_found),
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(16.dp)
            )
        }
        return
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        stickyHeader {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    Icon(
                        modifier = Modifier
                            .size(24.dp)
                            .clickable { onBack() },
                        painter = painterResource(R.drawable.back_arrow),
                        tint = MaterialTheme.colorScheme.onBackground,
                        contentDescription = null,
                    )
                }
                Column(modifier = Modifier.weight(3f)) {
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = stringResource(R.string.schedule, teamName),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = teamRecord,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                        fontStyle = FontStyle.Italic,
                    )
                }
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                    Icon(
                        modifier = Modifier
                            .size(24.dp)
                            .clickable {
                                yoursViewModel.toggleFavorite(
                                    teamName = teamName,
                                    leagueName = leagueName,
                                    dayName = dayName,
                                    dayId = dayId,
                                    leagueId = leagueId,
                                )
                            },
                        painter = if (isFavorite)
                            painterResource(R.drawable.favorite_selected)
                        else
                            painterResource(R.drawable.favorite_unselected),
                        tint = MaterialTheme.colorScheme.onBackground,
                        contentDescription = if (isFavorite) "Remove from Yours" else "Add to Yours",
                    )
                }
            }
        }
        items(teamSchedule.weeks) { week ->

            val teamGames = week.versus.filter { playTime ->
                playTime.team1Id == teamSchedule.teamId || playTime.team2Id == teamSchedule.teamId
            }

            if (teamGames.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {

                        Text(
                            text = "Week ${week.weekNumber} - ${week.date}",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        teamGames.forEach { playTime ->
                            val opponent = extractOpponent(
                                playTime.versusText,
                                teamName,
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = "${playTime.time} vs $opponent",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onBackground,
                                )
                                if (playTime.court.isNotEmpty()) {
                                    Text(
                                        text = playTime.court,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onBackground,
                                        fontStyle = FontStyle.Italic,
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                Text(
                    text = stringResource(R.string.no_game_data),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
        }
    }
}
