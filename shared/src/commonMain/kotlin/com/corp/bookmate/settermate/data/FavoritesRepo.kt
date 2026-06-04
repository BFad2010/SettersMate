package com.corp.bookmate.settermate.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class FavoritesRepo(private val database: FavoritesDatabase) {

    fun getAllFavorites(): Flow<List<FavoriteTeam>> =
        database.favoriteTeamQueries.getAllFavorites()
            .asFlow()
            .mapToList(Dispatchers.Default)

    suspend fun toggleFavorite(
        teamName: String,
        leagueName: String,
        dayName: String,
        dayId: Int,
        leagueId: Int,
    ) = withContext(Dispatchers.Default) {
        val existing = database.favoriteTeamQueries
            .getFavorite(teamName, leagueId.toLong())
            .executeAsOneOrNull()
        if (existing != null) {
            database.favoriteTeamQueries.deleteFavorite(teamName, leagueId.toLong())
        } else {
            database.favoriteTeamQueries.insertFavorite(
                teamName = teamName,
                leagueName = leagueName,
                dayName = dayName,
                dayId = dayId.toLong(),
                leagueId = leagueId.toLong(),
            )
        }
    }
}
