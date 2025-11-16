package com.example.maps.metadata

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.exifinterface.media.ExifInterface

fun hasLocation(context: Context, uri: Uri): FloatArray? {
    return try {
        val targetUri = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.setRequireOriginal(uri)
        } else uri

        context.contentResolver.openInputStream(targetUri)?.use { stream ->
            val exif = ExifInterface(stream)
            val latLong = FloatArray(2)
            if(exif.getLatLong(latLong) && latLong[0] != 0f || latLong[1] != 0f) {
                Log.d("MyLog", "Широта: ${latLong[0]}, Долгота: ${latLong[1]}")
                return@use latLong
            } else {
                return@use null
            }
        }
    } catch (e: Exception){
        Log.e("MyLog", "Нет разрешения ACCESS_MEDIA_LOCATION", e)
        null
    }
}

fun getImagesWithLocation(context: Context): Map<Long, Pair<Uri, FloatArray>> {
    val imagesWithCords = mutableMapOf<Long, Pair<Uri, FloatArray>>()
    val projection = arrayOf(MediaStore.Images.Media._ID)
    val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI

    context.contentResolver.query(
        collection,
        projection,
        null,
        null,
        null
    )?.use { cursor ->
        val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
        while(cursor.moveToNext()) {
            val id = cursor.getLong(idColumn)
            val uri = ContentUris.withAppendedId(collection, id)

            hasLocation(context, uri)?.let { latLong ->
                imagesWithCords[id] = Pair(uri, latLong)
            }
        }
    }

    return imagesWithCords
}