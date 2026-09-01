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
            .getFavorite(teamName, leagueName, dayId.toLong())
            .executeAsOneOrNull()
        if (existing != null) {
            database.favoriteTeamQueries.deleteFavorite(teamName, leagueName, dayId.toLong())
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

    // The site re-assigns leagueId each season, so a saved favorite's leagueId can go
    // stale. When we re-resolve the current id for a favorite (by leagueName/dayId),
    // persist the correction here instead of leaving the stale id in place.
    suspend fun updateLeagueId(id: Long, leagueId: Int) = withContext(Dispatchers.Default) {
        database.favoriteTeamQueries.updateLeagueId(leagueId.toLong(), id)
    }
}
