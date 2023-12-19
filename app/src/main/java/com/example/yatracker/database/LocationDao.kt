package com.example.yatracker.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LocationDao {
    @Insert
    suspend fun insertLocation(location: Location): Long

    @Query("SELECT * FROM locations WHERE sessionId = :sessionId")
    fun getLocationsForSession(sessionId: Long): Flow<List<Location>>

}