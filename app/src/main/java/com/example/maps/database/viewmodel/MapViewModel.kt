package com.example.maps.database.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.example.maps.database.map.Map
import com.example.maps.database.map.MapRoomDatabase
import com.example.maps.database.repository.Repository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class MapViewModel(application: Application) : AndroidViewModel(application) {

    val allCords: LiveData<List<Map>>

    private val repository: Repository

    init {
        val mapDao = MapRoomDatabase.getInstance(application).mapDao()
        repository = Repository(mapDao)
        allCords = repository.mapList
    }


    fun addCord(mediaID: Long, uri: Uri, latLong: FloatArray) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.addCord(mediaID, uri.toString(), latLong)
        }
    }
}