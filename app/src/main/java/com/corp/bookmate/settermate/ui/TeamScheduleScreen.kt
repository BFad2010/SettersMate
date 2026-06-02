package com.corp.bookmate.settermate.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.corp.bookmate.settermate.R
import com.corp.bookmate.settermate.helpers.extractOpponent
import com.corp.bookmate.settermate.service.LeagueSchedule

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TeamScheduleScreen(
    teamId: Int,
    teamName: String,
    schedules: List<LeagueSchedule>,
    onBack: () -> Unit,
) {
    val teamSchedule = schedules.firstOrNull {
        it.teamId == teamId
    }
    val isFavorite = remember { mutableStateOf(false) }
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
                        tint = colorResource(R.color.WhiteSmoke),
                        contentDescription = null,
                    )
                }
                Text(
                    modifier = Modifier.weight(3f),
                    text = "${teamName} Schedule",
                    textAlign = TextAlign.Center,
                    color = colorResource(R.color.WhiteSmoke),
                )
            }
            Text(
                text = "No schedule found...",
                color = colorResource(R.color.WhiteSmoke),
                modifier = Modifier.padding(16.dp)
            )
        }
        return
    }


    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        stickyHeader {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(color = colorResource(R.color.Black))
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    Icon(
                        modifier = Modifier
                            .size(24.dp)
                            .clickable { onBack() },
                        painter = painterResource(R.drawable.back_arrow),
                        tint = colorResource(R.color.WhiteSmoke),
                        contentDescription = null,
                    )
                }
                Text(
                    modifier = Modifier.weight(2f),
                    text = "${teamName} Schedule",
                    textAlign = TextAlign.Center,
                    color = colorResource(R.color.WhiteSmoke),
                )
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                    Icon(
                        modifier = Modifier
                            .size(24.dp)
                            .clickable {
                                // change this to set favorite through viewmodel
                                isFavorite.value = !isFavorite.value
                            },
                        painter = if (isFavorite.value) painterResource(R.drawable.favorite_selected) else painterResource(
                            R.drawable.favorite_unselected
                        ),
                        tint = colorResource(R.color.WhiteSmoke),
                        contentDescription = null,
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
                            color = colorResource(R.color.WhiteSmoke),
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
                                    color = colorResource(R.color.WhiteSmoke),
                                    overflow = TextOverflow.Ellipsis,
                                )
                                if (playTime.court.isNotEmpty()) {
                                    Text(
                                        text = playTime.court,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = colorResource(R.color.WhiteSmoke),
                                        fontStyle = FontStyle.Italic,
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                Text(
                    text = "No Game Data",
                    style = MaterialTheme.typography.bodyLarge,
                    color = colorResource(R.color.WhiteSmoke),
                )
            }
        }
    }
}