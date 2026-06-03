package com.corp.bookmate.settermate.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorite_teams")
data class FavoriteTeam(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val teamName: String,
    val leagueName: String,
    val dayName: String,
    val dayId: Int,
    val leagueId: Int,
)
