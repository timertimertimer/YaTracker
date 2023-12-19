package com.example.yatracker.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "locations",
    foreignKeys = [ForeignKey(
        entity = Session::class,
        parentColumns = arrayOf("id"),
        childColumns = arrayOf("sessionId"),
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("sessionId")]
)
data class Location(

    @PrimaryKey(autoGenerate = true)
    var id: Long = 0,

    var latitude: Double,

    var longitude: Double,

    var sessionId: Long
)