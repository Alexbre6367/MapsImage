@file:Suppress("DEPRECATION")

package com.example.maps.screen

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.imageLoader
import coil.request.ImageRequest
import coil.transform.RoundedCornersTransformation
import com.example.maps.database.viewmodel.MapViewModel
import com.example.maps.metadata.getImagesWithLocation
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.layers.ObjectEvent
import com.yandex.mapkit.location.LocationManagerUtils
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.map.MapObjectCollection
import com.yandex.mapkit.mapview.MapView
import com.yandex.mapkit.user_location.UserLocationObjectListener
import com.yandex.mapkit.user_location.UserLocationView
import com.yandex.runtime.image.ImageProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.Q)
@Composable
fun MapScreen(
    viewModel: MapViewModel= viewModel(),
) {
    val context = LocalContext.current
    val imageCoordinates by viewModel.allCords.observeAsState(initial = emptyList())

    val mapView = remember { MapView(context) }
    val mapObject = remember { mapView.map.mapObjects }

    val locationPermission = Manifest.permission.ACCESS_FINE_LOCATION //создаём разрешения
    val readMediaPermission = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
    val mediaLocationPermission = Manifest.permission.ACCESS_MEDIA_LOCATION

    //состояния для обработки разрешений
    var locationGranted by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, locationPermission) == PackageManager.PERMISSION_GRANTED) }
    var readMediaGranted by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, readMediaPermission) == PackageManager.PERMISSION_GRANTED) }
    var mediaLocationGranted by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, mediaLocationPermission) == PackageManager.PERMISSION_GRANTED) }

    //два лаунчера для обработки разрешений в цепочке
    val mediaPermissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { permissionsMap -> //извлекаем результат из разрешений и обновляем состояния в переменных выше
            locationGranted = permissionsMap[locationPermission] ?: locationGranted
            readMediaGranted = permissionsMap[readMediaPermission] ?: readMediaGranted
            mediaLocationGranted = permissionsMap[mediaLocationPermission] ?: mediaLocationGranted
        }
    )

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            locationGranted = isGranted
            if (isGranted) {
                val mediaPermissionsToRequest = mutableListOf<String>()
                if (!readMediaGranted) mediaPermissionsToRequest.add(readMediaPermission)
                if (!mediaLocationGranted) mediaPermissionsToRequest.add(mediaLocationPermission)

                if (mediaPermissionsToRequest.isNotEmpty()) {
                    mediaPermissionsLauncher.launch(mediaPermissionsToRequest.toTypedArray())
                }
            }
        }
    )

    LaunchedEffect(Unit) {
        if (!locationGranted) {
            locationPermissionLauncher.launch(locationPermission)
        } else {
            val mediaPermissionsToRequest = mutableListOf<String>()
            if (!readMediaGranted) mediaPermissionsToRequest.add(readMediaPermission)
            if (!mediaLocationGranted) mediaPermissionsToRequest.add(mediaLocationPermission)

            if (mediaPermissionsToRequest.isNotEmpty()) {
                mediaPermissionsLauncher.launch(mediaPermissionsToRequest.toTypedArray())
            }
        }
    }


    Box(modifier = Modifier.fillMaxSize()) {
        YandexMapScreen(
            mapView = mapView,
            mapObject = mapObject,
            imageCoordinates = imageCoordinates,
            locationPermissionGranted = locationGranted
        )


        AddPhoto(
            viewModel = viewModel,
            existingCoordinates = imageCoordinates,
            mediaPermissionGranted = readMediaGranted && mediaLocationGranted,
            onClick = {
                val position = mapView.map.cameraPosition
                mapView.map.move(
                    CameraPosition(
                        position.target, position.zoom, 0.0f, 0.0f
                    )
                )
            }
        )
    }
}

@Composable
fun YandexMapScreen(
    mapView: MapView,
    mapObject: MapObjectCollection,
    imageCoordinates: List<com.example.maps.database.map.Map>,
    locationPermissionGranted: Boolean
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    val mapKit = MapKitFactory.getInstance()
    val userLocation = remember(mapKit) {
        mapKit.createUserLocationLayer(mapView.mapWindow)
    }

    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> {
                    mapKit.onStart()
                    mapView.onStart()
                }

                Lifecycle.Event.ON_STOP -> {
                    mapKit.onStop()
                    mapView.onStop()
                } else -> {  }
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
        }
    }


    LaunchedEffect(imageCoordinates) {
        mapObject.clear()
        imageCoordinates.forEach { mapItem ->
            if(mapItem.uri.isNotBlank()) {
                val placemark = mapObject.addPlacemark().apply {
                    geometry = Point(mapItem.coordinates[0], mapItem.coordinates[1])
                    isVisible = true
                }

                val request = ImageRequest.Builder(context)
                    .data(mapItem.uri)
                    .size(130)
                    .transformations(RoundedCornersTransformation(25f))
                    .target { result ->
                        val bitmap = (result as BitmapDrawable).bitmap
                        placemark.setIcon(ImageProvider.fromBitmap(bitmap))
                    }
                    .build()
                context.imageLoader.enqueue(request)
            }
        }
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = {
            val objectListener = object : UserLocationObjectListener {
                override fun onObjectAdded(userLOcationView: UserLocationView) {
                    val last = LocationManagerUtils.getLastKnownLocation()
                    last?.position?.let { p ->
                        mapView.map.move(CameraPosition(Point(p.latitude, p.longitude), 16.0f, 0.0f, 0.0f))
                    }
                }
                override fun onObjectRemoved(p0: UserLocationView) {  }
                override fun onObjectUpdated(p0: UserLocationView, p1: ObjectEvent) {  } //для обновления, сделать позже
            }
            userLocation.setObjectListener(objectListener)
            mapView
        }
    ) { view ->
        if (locationPermissionGranted) {
            userLocation.isVisible = true //делает видимым на карте
            userLocation.isHeadingEnabled = true //направление пользователя
        } else {
            userLocation.isVisible = false
            view.map.move(
                CameraPosition(Point(55.751244, 37.618423), 14.0f, 0.0f, 0.0f)
            )
        }
    }
}


@RequiresApi(Build.VERSION_CODES.Q)
@Composable
fun AddPhoto(
    viewModel: MapViewModel,
    existingCoordinates: List<com.example.maps.database.map.Map>,
    onClick: () -> Unit,
    mediaPermissionGranted: Boolean
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val scan: suspend () -> Unit = {
        val existingUri = existingCoordinates.map { it.mediaId }.toSet()
        val result: Map<Long, Pair <Uri, FloatArray>> = getImagesWithLocation(context)
        result.forEach { (mediaId, pair) ->
            val uri = pair.first
            val latLongArray = pair.second
            if(mediaId !in existingUri) {
                viewModel.addCord(mediaId, uri, latLongArray)
            }
        }
    }

    LaunchedEffect(mediaPermissionGranted) {
        if(mediaPermissionGranted) {
            coroutineScope.launch(Dispatchers.IO) {
                scan()
            }
        } else {
            Log.d("MyLog", "Нет разрешений на доступ к галерее")
        }
    }


    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Button (
            onClick = onClick,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .size(55.dp),
            shape = RoundedCornerShape(16.dp),
            contentPadding = PaddingValues(0.dp)
        ) {
            Icon(
                Icons.Default.Refresh,
                contentDescription = "Выровнить",
                tint = Color.Black,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

