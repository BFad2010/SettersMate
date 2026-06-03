package com.corp.bookmate.settermate

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FabPosition
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.corp.bookmate.settermate.helpers.fuzzyTeamMatch
import com.corp.bookmate.settermate.service.LeagueContext
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

    val context = LocalContext.current
    val showHelpDialog = remember { mutableStateOf(false) }

    if (showHelpDialog.value) {
        AlertDialog(
            onDismissRequest = { showHelpDialog.value = false },
            confirmButton = {
                TextButton(onClick = { showHelpDialog.value = false }) {
                    Text(stringResource(R.string.close))
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(
                        16.dp
                    )
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.for_more_information_on_leagues_visit),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(color = colorResource(R.color.Black).copy(alpha = 0.05f))
                                .clickable {
                                    val intent = Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse("https://www.cherrygrovesportscenter.com/")
                                    )
                                    context.startActivity(intent)
                                }
                                .padding(4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = stringResource(R.string.cherrygrovesportscenter_com),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Icon(
                                modifier = Modifier.size(24.dp),
                                painter = painterResource(R.drawable.chevron_right),
                                tint = MaterialTheme.colorScheme.onBackground,
                                contentDescription = null,
                            )
                        }
                    }

                    HorizontalDivider()

                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.for_app_assistance),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(color = colorResource(R.color.Black).copy(alpha = 0.05f))
                                .clickable {
                                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                                        data = Uri.parse("mailto:")
                                        putExtra(
                                            Intent.EXTRA_EMAIL,
                                            arrayOf("bookmatecorp@gmail.com")
                                        )
                                        putExtra(Intent.EXTRA_SUBJECT, "Setters Scheduler help")
                                    }
                                    context.startActivity(
                                        Intent.createChooser(
                                            intent,
                                            "Send email"
                                        )
                                    )
                                    showHelpDialog.value = false
                                }
                                .padding(4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = stringResource(R.string.contact_us_here),
                                style = MaterialTheme.typography.titleMedium,
                                fontStyle = FontStyle.Italic,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Icon(
                                modifier = Modifier.size(24.dp),
                                painter = painterResource(R.drawable.chevron_right),
                                tint = MaterialTheme.colorScheme.onBackground,
                                contentDescription = null,
                            )
                        }
                    }
                }
            }
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        floatingActionButton = {
            Box(
                modifier = Modifier
                    .padding(end = 8.dp)
                    .size(64.dp)
                    .graphicsLayer {
                        shadowElevation = 24.dp.toPx()
                        shape = CircleShape
                        clip = true
                    }
                    .clip(CircleShape)
                    .border(1.5.dp, MaterialTheme.colorScheme.outline, CircleShape)
                    .background(MaterialTheme.colorScheme.surface.copy(0.7f))
                    .clickable {
                        showHelpDialog.value = true
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.need_help),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        },
        floatingActionButtonPosition = FabPosition.End,
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
                    label = { Text(stringResource(R.string.all_leagues)) },
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
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            Image(
                painter = painterResource(id = R.drawable.volleyball_background_tropical),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(0.5f),
            )
            when (currentDest.value) {
                is BottomNavDest.AllLeagues -> HomeUi(modifier = Modifier.padding(innerPadding))
                is BottomNavDest.Yours -> YoursScreen(
                    modifier = Modifier.padding(innerPadding),
                )
            }
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
    val leagueContext = viewModel.leagueContext.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(top = 8.dp, bottom = 24.dp)
            .padding(horizontal = 16.dp)
    ) {
        if (navState.value !is NavUiState.Schedule) {
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
                text = stringResource(R.string.welcome_to_setters_mate),
                textAlign = TextAlign.Center,
                fontSize = dimensionResource(R.dimen.heading_extra_large).value.sp,
            )
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                text = stringResource(R.string.volleyball_scheduling_made_easy),
                textAlign = TextAlign.Center,
                fontSize = dimensionResource(R.dimen.heading_small).value.sp,
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
                is LeaguesUiState.Loading -> Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                }

                is LeaguesUiState.Success -> {
                    AnimatedVisibility(visible = ls.leagues.isNotEmpty()) {
                        DropDownList(
                            dropDownTitle = selectedLeague.value,
                            listOptions = ls.leagues.map { it.leagueName },
                        ) { league ->
                            selectedLeague.value = league
                            val leagueId =
                                ls.leagues.find { it.leagueName == league }?.leagueId ?: 0
                            viewModel.setLeagueContext(
                                LeagueContext(
                                    dayName = selectedDay.value.first,
                                    dayId = selectedDay.value.second,
                                    leagueName = league,
                                    leagueId = leagueId,
                                )
                            )
                            viewModel.fetchSchedule(selectedDay.value.second, leagueId)
                        }
                    }
                }

                is LeaguesUiState.Error -> Text(
                    text = stringResource(R.string.failed_to_load_leagues, ls.message),
                    color = MaterialTheme.colorScheme.onBackground,
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
                            fuzzyTeamMatch(it.teamName, selectedTeam.value)
                        }?.teamId ?: 0,
                        teamName = selectedTeam.value,
                        leagueName = leagueContext.value?.leagueName ?: "",
                        dayName = leagueContext.value?.dayName ?: "",
                        dayId = leagueContext.value?.dayId ?: 0,
                        leagueId = leagueContext.value?.leagueId ?: 0,
                        schedules = state.leagueData.schedule,
                        teamRecord = state.leagueData.standings.find { it.name == selectedTeam.value }?.record.orEmpty(),
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
                    color = MaterialTheme.colorScheme.onBackground,
                    fontStyle = FontStyle.Italic,
                    fontSize = dimensionResource(R.dimen.body_normal).value.sp
                )
            }

            is ScheduleUiState.Idle -> Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp),
                    textAlign = TextAlign.Center,
                    text = stringResource(R.string.select_a_day_and_league_above_to_view_league_data_and_schedules),
                    color = MaterialTheme.colorScheme.onBackground,
                    fontStyle = FontStyle.Italic,
                    fontSize = dimensionResource(R.dimen.body_normal).value.sp
                )
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    text = stringResource(R.string.and),
                    fontStyle = FontStyle.Italic,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = dimensionResource(R.dimen.body_large).value.sp
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.favorite_selected),
                        contentDescription = stringResource(R.string.yours),
                        modifier = Modifier.size(24.dp),
                    )
                    Text(
                        text = stringResource(R.string.a_team_for_quicker_schedule_viewing),
                        color = MaterialTheme.colorScheme.onBackground,
                        fontStyle = FontStyle.Italic,
                        fontSize = dimensionResource(R.dimen.body_normal).value.sp
                    )
                }
            }
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
                        .background(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.1f))
                        .padding(vertical = 12.dp, horizontal = 4.dp)
                ) {
                    Box(modifier = Modifier.weight(3f), contentAlignment = Alignment.CenterStart) {
                        Row {
                            Text(
                                text = standing.name,
                                color = MaterialTheme.colorScheme.onBackground,
                                fontSize = dimensionResource(R.dimen.body_large).value.sp
                            )
                            Text(text = " - ", color = MaterialTheme.colorScheme.onBackground)
                            Text(
                                text = standing.record,
                                color = MaterialTheme.colorScheme.onBackground,
                                fontStyle = FontStyle.Italic,
                                fontSize = dimensionResource(R.dimen.body_normal).value.sp
                            )
                        }
                    }
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                        Icon(
                            modifier = Modifier.size(24.dp),
                            painter = painterResource(R.drawable.chevron_right),
                            tint = MaterialTheme.colorScheme.onBackground,
                            contentDescription = null,
                        )
                    }
                }
                HorizontalDivider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant
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
