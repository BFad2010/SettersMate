package com.corp.bookmate.settermate

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.corp.bookmate.settermate.service.TeamStanding
import com.corp.bookmate.settermate.service.daysMap
import com.corp.bookmate.settermate.ui.LeaguesViewModel
import com.corp.bookmate.settermate.ui.NavUiState
import com.corp.bookmate.settermate.ui.ScheduleUiState
import com.corp.bookmate.settermate.ui.TeamScheduleScreen
import com.corp.bookmate.settermate.ui.components.DropDownList
import com.corp.bookmate.settermate.ui.theme.SetterMateTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SetterMateTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    HomeUi()
                }
            }
        }
    }
}

@Composable
fun HomeUi(
    viewModel: LeaguesViewModel = hiltViewModel(),
) {
    val selectedDay = remember { mutableStateOf(Pair("Select A Day", 0)) }
    val selectedLeague = remember { mutableStateOf(Pair("Select A League", 0)) }
    val showLeagues = remember { mutableStateOf(false) }
    val leagueOptions = remember { mutableStateOf(emptyList<String>()) }
    val selectedTeam = remember { mutableStateOf("") }
    val uiState = viewModel.uiState.collectAsStateWithLifecycle()
    val navState = viewModel.navState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 24.dp, horizontal = 16.dp)
    ) {
        if (navState.value !is NavUiState.Schedule) {
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
                text = "Welcome to your \n \n Setters Mate!",
                textAlign = TextAlign.Center,
                fontSize = dimensionResource(R.dimen.heading_extra_large).value.sp,
            )
            Spacer(modifier = Modifier.height(16.dp))
            DropDownList(
                dropDownTitle = selectedDay.value.first,
                listOptions = daysMap.keys.toList()
            ) { day ->
                viewModel.clearLeagueData()
                selectedDay.value = daysMap.toList().find { it.first == day } ?: Pair("Select A Day", 0)
                leagueOptions.value = viewModel.getLeagueOptionsByDay(selectedDay.value.second)
                showLeagues.value = true
            }
            Spacer(modifier = Modifier.height(12.dp))
            AnimatedVisibility(visible = showLeagues.value) {
                DropDownList(
                    dropDownTitle = selectedLeague.value.first,
                    listOptions = leagueOptions.value,
                ) { league ->
                    selectedLeague.value = Pair(league, 0)
                    val leagueId = viewModel.getLeagueId(selectedDay.value.second, league)
                    viewModel.fetchSchedule(selectedDay.value.second, leagueId)
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        when (val state = uiState.value) {
            is ScheduleUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.size(60.dp))
                }
            }

            is ScheduleUiState.Success -> {
                when (navState.value) {
                    NavUiState.Schedule -> TeamScheduleScreen(
                        teamId = state.leagueData.schedule.find,
                        teamName = selectedTeam.value,
                        schedules = state.leagueData.schedule,
                    ) {
                        viewModel.navigate(NavUiState.Standings)
                    }

                    NavUiState.Standings -> TeamStandingUi(state.leagueData.standings) { teamName ->
                        selectedTeam.value = teamName
                        viewModel.navigate(NavUiState.Schedule)
                    }
                }
            }

            is ScheduleUiState.Error -> {
                Text(
                    text = state.message,
                    color = colorResource(R.color.WhiteSmoke),
                    fontStyle = FontStyle.Italic,
                    fontSize = dimensionResource(R.dimen.body_normal).value.sp
                )
            }

            is ScheduleUiState.Idle -> Text(
                text = "League Data will display here",
                color = colorResource(R.color.WhiteSmoke),
                fontStyle = FontStyle.Italic,
                fontSize = dimensionResource(R.dimen.body_normal).value.sp
            )
        }
    }
}

@Composable
fun TeamStandingUi(teamStandings: List<TeamStanding>, onSelectTeam: (String) -> Unit) {
    LazyColumn {
        items(teamStandings) { standing ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onSelectTeam(standing.name)
                    }) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(color = colorResource(R.color.LightSlateGray).copy(alpha = 0.1f))
                        .padding(vertical = 12.dp, horizontal = 4.dp)
                ) {
                    Box(modifier = Modifier.weight(3f), contentAlignment = Alignment.CenterStart) {
                        Row {
                            Text(
                                text = standing.name,
                                color = colorResource(R.color.WhiteSmoke),
                                fontSize = dimensionResource(R.dimen.body_large).value.sp
                            )
                            Text(text = " - ", color = colorResource(R.color.WhiteSmoke))
                            Text(
                                text = standing.record,
                                color = colorResource(R.color.WhiteSmoke),
                                fontStyle = FontStyle.Italic,
                                fontSize = dimensionResource(R.dimen.body_normal).value.sp
                            )
                        }
                    }
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                        Icon(
                            modifier = Modifier.size(24.dp),
                            painter = painterResource(R.drawable.chevron_right),
                            tint = colorResource(R.color.WhiteSmoke),
                            contentDescription = null,
                        )
                    }
                }
                HorizontalDivider(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    thickness = 1.dp,
                    color = colorResource(R.color.LightSlateGray)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    SetterMateTheme {
        HomeUi()
    }
}