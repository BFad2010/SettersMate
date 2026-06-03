package com.corp.bookmate.settermate.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [FavoriteTeam::class], version = 1, exportSchema = false)
abstract class SetterMateDatabase : RoomDatabase() {
    abstract fun favoriteTeamDao(): FavoriteTeamDao
}
