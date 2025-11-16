package com.example.maps.database.map

import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Keep
@Entity(
    tableName = "map",
    indices = [Index(value = ["mediaId"], unique = true)]
)
data class Map (
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val mediaId: Long,
    val uri: String,
    val coordinates: List<Double>,
)