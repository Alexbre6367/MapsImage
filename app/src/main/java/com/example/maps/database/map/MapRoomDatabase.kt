package com.example.maps.database.map

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.maps.database.converters.Converters

@Database(entities = [Map::class], version = 4)
@TypeConverters(Converters::class)
abstract class MapRoomDatabase : RoomDatabase() {
    abstract fun mapDao(): MapDao

    companion object {
        @Volatile
        private var INSTANCE: MapRoomDatabase? = null

        fun getInstance(context: Context): MapRoomDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    MapRoomDatabase::class.java,
                    "map_database"
                ).fallbackToDestructiveMigration().build().also { INSTANCE = it }
            }
        }
    }
}
