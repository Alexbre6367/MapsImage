package com.example.maps.database.converters

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromList(array: List<Double>): String {
        return array.joinToString(",")
    }

    @TypeConverter
    fun toList(data: String): List<Double> {
        return if (data.isEmpty()) emptyList() else data.split(",").map  { it.toDouble() }
    }
}