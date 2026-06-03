package com.corp.bookmate.settermate.data

import kotlinx.coroutines.flow.Flow

class FavoritesRepo(private val dao: FavoriteTeamDao) {

    fun getAllFavorites(): Flow<List<FavoriteTeam>> = dao.getAllFavorites()

    suspend fun toggleFavorite(
        teamName: String,
        leagueName: String,
        dayName: String,
        dayId: Int,
        leagueId: Int,
    ) {
        val existing = dao.getFavorite(teamName, leagueId)
        if (existing != null) {
            dao.delete(teamName, leagueId)
        } else {
            dao.insert(
                FavoriteTeam(
                    teamName = teamName,
                    leagueName = leagueName,
                    dayName = dayName,
                    dayId = dayId,
                    leagueId = leagueId,
                )
            )
        }
    }
}
