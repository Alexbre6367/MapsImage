package com.example.maps.database.map

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface MapDao {
    @Query("SELECT * FROM map")
    fun getAll(): LiveData<List<Map>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addCord(map: Map)
}
