package com.corp.bookmate.settermate

import android.os.Bundle
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
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
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
import com.corp.bookmate.settermate.helpers.fuzzyTeamMatch
import com.corp.bookmate.settermate.service.TeamStanding
import com.corp.bookmate.settermate.service.daysMap
import com.corp.bookmate.settermate.ui.LeaguesUiState
import com.corp.bookmate.settermate.ui.LeaguesViewModel
import com.corp.bookmate.settermate.ui.NavUiState
import com.corp.bookmate.settermate.ui.ScheduleUiState
import com.corp.bookmate.settermate.ui.TeamScheduleScreen
import com.corp.bookmate.settermate.ui.YoursScreen
import com.corp.bookmate.settermate.ui.components.DropDownList
import com.corp.bookmate.settermate.ui.theme.SetterMateTheme
import dagger.hilt.android.AndroidEntryPoint

sealed class BottomNavDest {
    object AllLeagues : BottomNavDest()
    object Yours : BottomNavDest()
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SetterMateTheme {
                AppShell()
            }
        }
    }
}

@Composable
fun AppShell() {
    val currentDest = remember { mutableStateOf<BottomNavDest>(BottomNavDest.AllLeagues) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = currentDest.value is BottomNavDest.AllLeagues,
                    onClick = { currentDest.value = BottomNavDest.AllLeagues },
                    icon = {
                        Icon(
                            painter = painterResource(R.drawable.volleyball_nav),
                            contentDescription = "All Leagues",
                            modifier = Modifier.size(24.dp),
                        )
                    },
                    label = { Text("All Leagues") },
                )
                NavigationBarItem(
                    selected = currentDest.value is BottomNavDest.Yours,
                    onClick = { currentDest.value = BottomNavDest.Yours },
                    icon = {
                        Icon(
                            painter = painterResource(R.drawable.favorite_unselected),
                            contentDescription = "Yours",
                            modifier = Modifier.size(24.dp),
                        )
                    },
                    label = { Text("Yours") },
                )
            }
        },
    ) { innerPadding ->
        when (currentDest.value) {
            is BottomNavDest.AllLeagues -> HomeUi(modifier = Modifier.padding(innerPadding))
            is BottomNavDest.Yours -> YoursScreen(modifier = Modifier.padding(innerPadding))
        }
    }
}

@Composable
fun HomeUi(
    modifier: Modifier = Modifier,
    viewModel: LeaguesViewModel = hiltViewModel(),
) {
    val selectedDay = remember { mutableStateOf(Pair("Select A Day", 0)) }
    val selectedLeague = remember { mutableStateOf("Select A League") }
    val uiState = viewModel.uiState.collectAsStateWithLifecycle()
    val navState = viewModel.navState.collectAsStateWithLifecycle()
    val leaguesState = viewModel.leaguesState.collectAsStateWithLifecycle()
    val selectedTeam = viewModel.selectedTeam.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
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
                viewModel.clearLeagues()
                selectedLeague.value = "Select A League"
                selectedDay.value =
                    daysMap.toList().find { it.first == day } ?: Pair("Select A Day", 0)
                if (selectedDay.value.second != 0) {
                    viewModel.fetchLeaguesByDay(selectedDay.value.second)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            when (val ls = leaguesState.value) {
                is LeaguesUiState.Loading -> CircularProgressIndicator(modifier = Modifier.size(24.dp))
                is LeaguesUiState.Success -> {
                    AnimatedVisibility(visible = ls.leagues.isNotEmpty()) {
                        DropDownList(
                            dropDownTitle = selectedLeague.value,
                            listOptions = ls.leagues.map { it.leagueName },
                        ) { league ->
                            selectedLeague.value = league
                            val leagueId =
                                ls.leagues.find { it.leagueName == league }?.leagueId ?: 0
                            viewModel.fetchSchedule(selectedDay.value.second, leagueId)
                        }
                    }
                }

                is LeaguesUiState.Error -> Text(
                    text = "Failed to load leagues: ${ls.message}",
                    color = colorResource(R.color.WhiteSmoke),
                    fontStyle = FontStyle.Italic,
                )

                else -> Unit
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
                        teamId = state.leagueData.schedule.find {
                            fuzzyTeamMatch(
                                it.teamName,
                                selectedTeam.value
                            )
                        }?.teamId ?: 0,
                        teamName = selectedTeam.value,
                        schedules = state.leagueData.schedule,
                    ) {
                        viewModel.navigate(NavUiState.Standings)
                        viewModel.setSelectedTeam("")
                    }

                    NavUiState.Standings -> TeamStandingUi(state.leagueData.standings) { teamName ->
                        viewModel.setSelectedTeam(teamName)
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
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                textAlign = TextAlign.Center,
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
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
        AppShell()
    }
}
