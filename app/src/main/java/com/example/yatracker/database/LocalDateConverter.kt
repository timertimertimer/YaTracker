package com.example.yatracker.database

import androidx.room.TypeConverter
import java.time.LocalDateTime
import java.time.ZoneOffset

class LocalDateTimeConverter {
    @TypeConverter
    fun localDateTimeToEpochDay(dateTime: LocalDateTime?): Long? =
        dateTime?.toEpochSecond(ZoneOffset.UTC)

    @TypeConverter
    fun epochSecondToLocalDateTime(value: Long?): LocalDateTime? = value?.let{
        LocalDateTime.ofEpochSecond(it, 0, ZoneOffset.UTC)
    }
}
