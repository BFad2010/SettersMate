package com.corp.bookmate.settermate.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.corp.bookmate.settermate.helpers.BackHandlerWrapper
import com.corp.bookmate.settermate.helpers.CalendarEvent
import com.corp.bookmate.settermate.helpers.extractOpponent
import com.corp.bookmate.settermate.helpers.fuzzyTeamMatch
import com.corp.bookmate.settermate.helpers.rememberCalendarLauncher
import com.corp.bookmate.settermate.service.LeagueSchedule
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel
import settermate.shared.generated.resources.Res
import settermate.shared.generated.resources.back_arrow
import settermate.shared.generated.resources.calendar_icon
import settermate.shared.generated.resources.favorite_selected
import settermate.shared.generated.resources.favorite_unselected

@Composable
fun TeamScheduleScreen(
    teamName: String,
    leagueName: String,
    dayName: String,
    dayId: Int,
    leagueId: Int,
    schedules: List<LeagueSchedule>,
    teamRecord: String,
    modifier: Modifier = Modifier,
    yoursViewModel: YoursViewModel = koinViewModel(),
    onBack: () -> Unit,
) {
    val favorites by yoursViewModel.favorites.collectAsState()
    val isFavorite = favorites.any { it.leagueId == leagueId.toLong() && it.teamName == teamName }
    val teamSchedule = schedules.firstOrNull { fuzzyTeamMatch(it.teamName, teamName) }
    val uriHandler = LocalUriHandler.current

    val calendarEvents = teamSchedule?.weeks?.flatMap { week ->
        week.versus
            .filter { it.team1Id == teamSchedule.teamId || it.team2Id == teamSchedule.teamId }
            .map { playTime ->
                val opponent = extractOpponent(playTime.versusText, teamName)
                CalendarEvent(
                    title = "$dayName $leagueName - Vs. $opponent",
                    description = "Week ${week.weekNumber}",
                    dateString = week.date,
                    timeString = playTime.time,
                )
            }
    } ?: emptyList()

    var calendarMessage by remember { mutableStateOf("") }
    var showCalendarMessage by remember { mutableStateOf(false) }
    LaunchedEffect(showCalendarMessage) {
        if (showCalendarMessage) {
            delay(3000)
            showCalendarMessage = false
        }
    }
    val launchCalendar = rememberCalendarLauncher(calendarEvents) { count ->
        calendarMessage = "$count game${if (count == 1) "" else "s"} added to your calendar"
        showCalendarMessage = true
    }

    BackHandlerWrapper(onBack = onBack)

    if (teamSchedule == null) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    Icon(
                        modifier = Modifier.size(24.dp).clickable { onBack() },
                        painter = painterResource(Res.drawable.back_arrow),
                        tint = MaterialTheme.colorScheme.onBackground,
                        contentDescription = null,
                    )
                }
                Text(
                    modifier = Modifier.weight(3f),
                    text = "$teamName Schedule",
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
            Text(
                text = "No schedule found...",
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(16.dp),
            )
        }
        return
    }

    Box(modifier = modifier.fillMaxSize()) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Header lives outside the scroll container — cards can never scroll over it
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = 12.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(10.dp))
                    .clickable { onBack() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    modifier = Modifier.size(20.dp),
                    painter = painterResource(Res.drawable.back_arrow),
                    tint = MaterialTheme.colorScheme.onBackground,
                    contentDescription = "Back",
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = "$teamName Schedule",
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onBackground,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = teamRecord,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(10.dp))
                    .clickable {
                        yoursViewModel.toggleFavorite(
                            teamName = teamName,
                            leagueName = leagueName,
                            dayName = dayName,
                            dayId = dayId,
                            leagueId = leagueId,
                        )
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    modifier = Modifier.size(20.dp),
                    painter = if (isFavorite) painterResource(Res.drawable.favorite_selected)
                    else painterResource(Res.drawable.favorite_unselected),
                    tint = if (isFavorite) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onBackground,
                    contentDescription = if (isFavorite) "Remove from Yours" else "Add to Yours",
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier.clickable { launchCalendar() },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    modifier = Modifier.size(28.dp),
                    painter = painterResource(Res.drawable.calendar_icon),
                    tint = MaterialTheme.colorScheme.primary,
                    contentDescription = "Add to Calendar",
                )
                Text(
                    modifier = Modifier.padding(start = 8.dp),
                    text = "Add to Calendar",
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Text(
                modifier = Modifier.clickable {
                    teamSchedule.pdfUrl.takeIf { it.isNotEmpty() }?.let { uriHandler.openUri(it) }
                },
                text = buildAnnotatedString {
                    withStyle(
                        SpanStyle(
                            textDecoration = TextDecoration.Underline,
                            fontStyle = FontStyle.Italic,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                    ) { append("View as PDF") }
                },
            )
        }

        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(teamSchedule.weeks) { week ->
                val teamGames =
                    week.versus.filter { it.team1Id == teamSchedule.teamId || it.team2Id == teamSchedule.teamId }
                if (teamGames.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(
                                alpha = 0.7f
                            )
                        ),
                        elevation = CardDefaults.cardElevation(4.dp),
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Week ${week.weekNumber}  •  ${week.date}",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            teamGames.forEach { playTime ->
                                val opponent = extractOpponent(playTime.versusText, teamName)
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
                        text = "No Game Data",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }
            }
        }
    } // end Column

    AnimatedVisibility(
        visible = showCalendarMessage,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp),
    ) {
        Box(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .clip(RoundedCornerShape(50.dp))
                .background(MaterialTheme.colorScheme.primary)
                .padding(horizontal = 20.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = calendarMessage,
                color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }

    } // end Box
}
