package com.example.maps.database.repository

import android.util.Log
import androidx.lifecycle.LiveData
import com.example.maps.database.map.Map
import com.example.maps.database.map.MapDao

class Repository(private val mapDao: MapDao) {
    val mapList: LiveData<List<Map>> = mapDao.getAll()

    suspend fun addCord(mediaID: Long, uri: String, latLong: FloatArray) {
        val newCord = Map(
            mediaId = mediaID,
            uri = uri,
            coordinates = latLong.map { it.toDouble() }
        )
        mapDao.addCord(newCord)
        Log.d("MyLog", "Координата добавлена: $newCord")
    }
}