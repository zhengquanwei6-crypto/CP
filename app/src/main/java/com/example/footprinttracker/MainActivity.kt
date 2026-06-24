package com.example.footprinttracker

import android.Manifest
import android.content.pm.PackageManager
import android.location.Geocoder
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import java.text.DateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { FootprintApp() }
    }
}

data class Footprint(
    val id: Long,
    val title: String,
    val latitude: Double,
    val longitude: Double,
    val address: String,
    val photoUri: Uri?,
    val visitedAt: Long,
)

private val demoRoute = listOf(
    LatLng(37.7749, -122.4194),
    LatLng(37.7793, -122.4192),
    LatLng(37.7847, -122.4075),
    LatLng(37.7936, -122.3965),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FootprintApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val footprints = remember { mutableStateListOf<Footprint>() }
    var selected by remember { mutableStateOf<Footprint?>(null) }
    var playbackIndex by remember { mutableStateOf(0) }
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(demoRoute.first(), 12f)
    }
    val locationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    var hasLocationPermission by remember {
        mutableStateOf(context.hasLocationPermission())
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { grants ->
        hasLocationPermission = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (!hasLocationPermission) {
            scope.launch { snackbarHostState.showSnackbar("定位权限被拒绝：仍可使用模拟轨迹播放。") }
        }
    }

    LaunchedEffect(Unit) {
        if (!hasLocationPermission) {
            permissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
            )
        }
    }

    MaterialTheme {
        Scaffold(
            topBar = { TopAppBar(title = { Text("Footprint Tracker") }) },
            snackbarHost = { SnackbarHost(snackbarHostState) },
        ) { padding ->
            Column(Modifier.padding(padding).fillMaxSize()) {
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    GoogleMap(
                        modifier = Modifier.fillMaxSize(),
                        cameraPositionState = cameraPositionState,
                    ) {
                        Polyline(points = demoRoute.take(playbackIndex + 1))
                        footprints.forEach { footprint ->
                            Marker(
                                state = MarkerState(position = LatLng(footprint.latitude, footprint.longitude)),
                                title = footprint.title,
                                snippet = footprint.address,
                                onClick = {
                                    selected = footprint
                                    true
                                },
                            )
                        }
                    }
                    selected?.let { footprint ->
                        FootprintCard(
                            footprint = footprint,
                            modifier = Modifier.align(Alignment.BottomCenter).padding(12.dp),
                        )
                    }
                }
                ActionPanel(
                    hasLocationPermission = hasLocationPermission,
                    onRequestPermission = {
                        permissionLauncher.launch(
                            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                        )
                    },
                    onAddCurrentLocation = {
                        scope.launch {
                            if (!hasLocationPermission) {
                                snackbarHostState.showSnackbar("请先授予定位权限，或使用模拟轨迹 fallback。")
                                return@launch
                            }
                            val location = runCatching { locationClient.lastLocation.await() }.getOrNull()
                            if (location == null) {
                                snackbarHostState.showSnackbar("暂时无法获取当前位置，请稍后重试。")
                                return@launch
                            }
                            footprints.add(context.toFootprint(location.latitude, location.longitude))
                        }
                    },
                    onPlayDemo = {
                        scope.launch {
                            demoRoute.forEachIndexed { index, point ->
                                playbackIndex = index
                                footprints.add(context.toFootprint(point.latitude, point.longitude, "模拟足迹 ${index + 1}"))
                                cameraPositionState.position = CameraPosition.fromLatLngZoom(point, 14f)
                                delay(800)
                            }
                        }
                    },
                )
                FootprintList(footprints)
            }
        }
    }
}

@Composable
private fun ActionPanel(
    hasLocationPermission: Boolean,
    onRequestPermission: () -> Unit,
    onAddCurrentLocation: () -> Unit,
    onPlayDemo: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (!hasLocationPermission) {
            Text("定位权限未开启。你可以授权后添加当前位置，或继续使用模拟轨迹播放。")
            Button(onClick = onRequestPermission) { Text("重新请求定位权限") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onAddCurrentLocation) { Text("添加当前位置") }
            Button(onClick = onPlayDemo) { Text("模拟轨迹播放") }
        }
    }
}

@Composable
private fun FootprintList(footprints: List<Footprint>) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(footprints, key = { it.id }) { footprint -> FootprintCard(footprint = footprint) }
    }
}

@Composable
private fun FootprintCard(footprint: Footprint, modifier: Modifier = Modifier) {
    Card(modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text(footprint.title, style = MaterialTheme.typography.titleMedium)
            Text(footprint.address)
            Text("${footprint.latitude.format()}, ${footprint.longitude.format()}")
            Text("到访：${DateFormat.getDateTimeInstance().format(Date(footprint.visitedAt))}")
            footprint.photoUri?.let { Text("照片：$it") }
            Spacer(Modifier.height(2.dp))
        }
    }
}

private fun android.content.Context.hasLocationPermission(): Boolean =
    ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

private fun android.content.Context.toFootprint(latitude: Double, longitude: Double, title: String = "当前位置"): Footprint {
    val address = runCatching {
        Geocoder(this, Locale.getDefault()).getFromLocation(latitude, longitude, 1)?.firstOrNull()?.getAddressLine(0)
    }.getOrNull() ?: "未知地址"
    return Footprint(
        id = System.currentTimeMillis(),
        title = title,
        latitude = latitude,
        longitude = longitude,
        address = address,
        photoUri = null,
        visitedAt = System.currentTimeMillis(),
    )
}

private fun Double.format(): String = String.format(Locale.US, "%.5f", this)
