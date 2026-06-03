package com.corp.bookmate.settermate.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteTeamDao {

    @Query("SELECT * FROM favorite_teams ORDER BY dayName, leagueName, teamName")
    fun getAllFavorites(): Flow<List<FavoriteTeam>>

    @Query("SELECT * FROM favorite_teams WHERE teamName = :teamName AND leagueId = :leagueId LIMIT 1")
    suspend fun getFavorite(teamName: String, leagueId: Int): FavoriteTeam?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(favorite: FavoriteTeam)

    @Query("DELETE FROM favorite_teams WHERE teamName = :teamName AND leagueId = :leagueId")
    suspend fun delete(teamName: String, leagueId: Int)
}
