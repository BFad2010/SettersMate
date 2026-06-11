package com.corp.bookmate.settermate.ui

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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.corp.bookmate.settermate.helpers.fuzzyTeamMatch
import com.corp.bookmate.settermate.service.LeagueContext
import com.corp.bookmate.settermate.service.TeamStanding
import com.corp.bookmate.settermate.service.daysMap
import com.corp.bookmate.settermate.ui.components.DropDownList
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel
import settermate.shared.generated.resources.Res
import settermate.shared.generated.resources.back_arrow
import settermate.shared.generated.resources.chevron_right
import settermate.shared.generated.resources.favorite_selected
import settermate.shared.generated.resources.favorite_unselected
import settermate.shared.generated.resources.volleyball_background_tropical
import settermate.shared.generated.resources.volleyball_nav

private const val FETCH_ERROR_MESSAGE =
    "Sorry, but there was an error fetching Schedule data...\nPlease check your connection and try again."

@Composable
fun ErrorView(
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
    ) {
        if (onBack != null) {
            Icon(
                modifier = Modifier
                    .size(24.dp)
                    .clickable { onBack() },
                painter = painterResource(Res.drawable.back_arrow),
                tint = MaterialTheme.colorScheme.onBackground,
                contentDescription = "Back",
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = FETCH_ERROR_MESSAGE,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

sealed class BottomNavDest {
    object AllLeagues : BottomNavDest()
    object Yours : BottomNavDest()
}

@Composable
fun AppShell() {
    val currentDest = remember { mutableStateOf<BottomNavDest>(BottomNavDest.AllLeagues) }
    val showHelpDialog = remember { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current

    if (showHelpDialog.value) {
        AlertDialog(
            onDismissRequest = { showHelpDialog.value = false },
            confirmButton = {
                TextButton(onClick = { showHelpDialog.value = false }) { Text("Close") }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "For more information on Leagues visit - ",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(color = Color.Black.copy(alpha = 0.05f))
                                .clickable {
                                    uriHandler.openUri("https://www.cherrygrovesportscenter.com/")
                                }
                                .padding(4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = "cherrygrovesportscenter.com",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Icon(
                                modifier = Modifier.size(24.dp),
                                painter = painterResource(Res.drawable.chevron_right),
                                tint = MaterialTheme.colorScheme.onBackground,
                                contentDescription = null,
                            )
                        }
                    }
                    HorizontalDivider()
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(text = "For app assistance -", style = MaterialTheme.typography.titleMedium)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(color = Color.Black.copy(alpha = 0.05f))
                                .clickable {
                                    uriHandler.openUri("mailto:bookmatecorp@gmail.com?subject=Setters%20Scheduler%20help")
                                    showHelpDialog.value = false
                                }
                                .padding(4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = "Contact Us Here",
                                style = MaterialTheme.typography.titleMedium,
                                fontStyle = FontStyle.Italic,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Icon(
                                modifier = Modifier.size(24.dp),
                                painter = painterResource(Res.drawable.chevron_right),
                                tint = MaterialTheme.colorScheme.onBackground,
                                contentDescription = null,
                            )
                        }
                    }
                    HorizontalDivider()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(color = Color.Black.copy(alpha = 0.05f))
                            .clickable {
                                uriHandler.openUri("https://www.termsfeed.com/live/09569d93-92b2-4d79-a065-fbc9f9975dd2")
                                showHelpDialog.value = false
                            }
                            .padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "Privacy Policy",
                            style = MaterialTheme.typography.titleMedium,
                            fontStyle = FontStyle.Italic,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Icon(
                            modifier = Modifier.size(24.dp),
                            painter = painterResource(Res.drawable.chevron_right),
                            tint = MaterialTheme.colorScheme.onBackground,
                            contentDescription = null,
                        )
                    }
                }
            },
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
                    .clickable { showHelpDialog.value = true },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Need\nhelp?",
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
                            painter = painterResource(Res.drawable.volleyball_nav),
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
                            painter = painterResource(Res.drawable.favorite_unselected),
                            contentDescription = "Yours",
                            modifier = Modifier.size(24.dp),
                        )
                    },
                    label = { Text("Yours") },
                )
            }
        },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(Res.drawable.volleyball_background_tropical),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().alpha(0.5f),
            )
            when (currentDest.value) {
                is BottomNavDest.AllLeagues -> HomeUi(modifier = Modifier.padding(innerPadding))
                is BottomNavDest.Yours -> YoursScreen(modifier = Modifier.padding(innerPadding))
            }
        }
    }
}

@Composable
fun HomeUi(
    modifier: Modifier = Modifier,
    viewModel: LeaguesViewModel = koinViewModel(),
) {
    val selectedDay = remember { mutableStateOf(Pair("Select A Day", 0)) }
    val selectedLeague = remember { mutableStateOf("Select A League") }
    val uiState by viewModel.uiState.collectAsState()
    val navState by viewModel.navState.collectAsState()
    val leaguesState by viewModel.leaguesState.collectAsState()
    val selectedTeam by viewModel.selectedTeam.collectAsState()
    val leagueContext by viewModel.leagueContext.collectAsState()
    val tourneyUrl by viewModel.tourneyUrl.collectAsState()
    val uriHandler = LocalUriHandler.current

    tourneyUrl?.let { url ->
        uriHandler.openUri(url)
        viewModel.clearTourneyUrl()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(top = 8.dp, bottom = 24.dp)
            .padding(horizontal = 16.dp),
    ) {
        if (navState !is NavUiState.Schedule) {
            Text(
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                text = "Welcome to Setters Mate",
                textAlign = TextAlign.Center,
                fontSize = 32.sp,
            )
            Text(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                text = "Volleyball Scheduling Made easy!",
                textAlign = TextAlign.Center,
                fontSize = 20.sp,
            )
            Spacer(modifier = Modifier.height(16.dp))
            DropDownList(
                dropDownTitle = selectedDay.value.first,
                listOptions = daysMap.keys.toList(),
            ) { day ->
                viewModel.clearLeagueData()
                viewModel.clearLeagues()
                selectedLeague.value = "Select A League"
                selectedDay.value = daysMap.toList().find { it.first == day } ?: Pair("Select A Day", 0)
                if (selectedDay.value.second != 0) viewModel.fetchLeaguesByDay(selectedDay.value.second)
            }
            Spacer(modifier = Modifier.height(12.dp))
            when (val ls = leaguesState) {
                is LeaguesUiState.Loading -> Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                }
                is LeaguesUiState.Success -> {
                    AnimatedVisibility(visible = ls.leagues.isNotEmpty()) {
                        DropDownList(
                            dropDownTitle = selectedLeague.value,
                            listOptions = ls.leagues.map { it.leagueName },
                        ) { league ->
                            selectedLeague.value = league
                            val leagueId = ls.leagues.find { it.leagueName == league }?.leagueId ?: 0
                            if (league.contains("Tourney", ignoreCase = true)) {
                                viewModel.fetchTourneyScheduleUrl(selectedDay.value.second, leagueId)
                            } else {
                                viewModel.setLeagueContext(LeagueContext(
                                    dayName = selectedDay.value.first,
                                    dayId = selectedDay.value.second,
                                    leagueName = league,
                                    leagueId = leagueId,
                                ))
                                viewModel.fetchSchedule(selectedDay.value.second, leagueId)
                            }
                        }
                    }
                }
                is LeaguesUiState.Error -> ErrorView()
                else -> Unit
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        when (val state = uiState) {
            is ScheduleUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.size(60.dp))
                }
            }
            is ScheduleUiState.Success -> {
                when (navState) {
                    NavUiState.Schedule -> TeamScheduleScreen(
                        teamId = state.leagueData.schedule.find { fuzzyTeamMatch(it.teamName, selectedTeam) }?.teamId ?: 0,
                        teamName = selectedTeam,
                        leagueName = leagueContext?.leagueName ?: "",
                        dayName = leagueContext?.dayName ?: "",
                        dayId = leagueContext?.dayId ?: 0,
                        leagueId = leagueContext?.leagueId ?: 0,
                        schedules = state.leagueData.schedule,
                        teamRecord = state.leagueData.standings.find { it.name == selectedTeam }?.record.orEmpty(),
                        onViewPdf = {
                            viewModel.fetchTourneyScheduleUrl(
                                leagueContext?.dayId ?: 0,
                                leagueContext?.leagueId ?: 0,
                            )
                        },
                        onBack = {
                            viewModel.navigate(NavUiState.Standings)
                            viewModel.setSelectedTeam("")
                        },
                    )
                    NavUiState.Standings -> TeamStandingUi(state.leagueData.standings) { teamName ->
                        viewModel.setSelectedTeam(teamName)
                        viewModel.navigate(NavUiState.Schedule)
                    }
                }
            }
            is ScheduleUiState.Error -> ErrorView(
                onBack = if (navState is NavUiState.Schedule) {
                    { viewModel.navigate(NavUiState.Standings); viewModel.setSelectedTeam("") }
                } else null,
            )
            is ScheduleUiState.Idle -> Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                    textAlign = TextAlign.Center,
                    text = "Select a Day and League above \n to view League Data and Schedules.",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontStyle = FontStyle.Italic,
                    fontSize = 16.sp,
                )
                Text(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    text = "AND",
                    fontStyle = FontStyle.Italic,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 18.sp,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.favorite_selected),
                        contentDescription = "Yours",
                        modifier = Modifier.size(24.dp),
                    )
                    Text(
                        text = " a Team for quicker Schedule Viewing!",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontStyle = FontStyle.Italic,
                        fontSize = 16.sp,
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
                modifier = Modifier.fillMaxWidth().clickable { onSelectTeam(standing.name) },
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.1f))
                        .padding(vertical = 12.dp, horizontal = 4.dp),
                ) {
                    Box(modifier = Modifier.weight(3f), contentAlignment = Alignment.CenterStart) {
                        Row {
                            Text(text = standing.name, color = MaterialTheme.colorScheme.onBackground, fontSize = 18.sp)
                            Text(text = " - ", color = MaterialTheme.colorScheme.onBackground)
                            Text(text = standing.record, color = MaterialTheme.colorScheme.onBackground, fontStyle = FontStyle.Italic, fontSize = 16.sp)
                        }
                    }
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                        Icon(
                            modifier = Modifier.size(24.dp),
                            painter = painterResource(Res.drawable.chevron_right),
                            tint = MaterialTheme.colorScheme.onBackground,
                            contentDescription = null,
                        )
                    }
                }
                HorizontalDivider(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
            }
        }
    }
}
