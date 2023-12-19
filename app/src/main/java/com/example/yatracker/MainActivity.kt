package com.example.yatracker

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.ViewModelProvider
import com.example.yatracker.ui.theme.YaTrackerTheme
import com.example.yatracker.viewmodels.MainViewModel
import com.yandex.mapkit.Animation
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.geometry.PolylineBuilder
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.mapview.MapView
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

class MainActivity : ComponentActivity() {

    private lateinit var mapView: MapView


    private val mvm: MainViewModel by lazy {
        ViewModelProvider(this)[MainViewModel::class.java]
    }

    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        mvm.showRequestDialog = false
        when {
            permissions.getOrDefault(ACCESS_FINE_LOCATION, false) -> {

            }

            permissions.getOrDefault(ACCESS_COARSE_LOCATION, false) -> {
            }

            else -> {
                finish()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MapKitFactory.setApiKey(BuildConfig.MAPKIT_API_KEY)
        MapKitFactory.initialize(this)
        mapView = MapView(this)
        setContent {
            YaTrackerTheme {
                MainUI(mvm, Modifier.fillMaxSize()) {
                    mvm.showRequestDialog =
                        !mvm.isPermissionsGranted(
                            ACCESS_COARSE_LOCATION,
                            ACCESS_FINE_LOCATION, context = this
                        )
                    if (mvm.showRequestDialog) {
                        LocationRequestDialog(
                            onDeny = {
                                finish()
                            }
                        ) {
                            // Формирование запроса из системы на доступ к геолокации
                            mvm.showRequestDialog = false
                            locationPermissionRequest.launch(
                                arrayOf(
                                    ACCESS_FINE_LOCATION,
                                    ACCESS_COARSE_LOCATION
                                )
                            )
                        }
                    } else {
                        mvm.startLocationUpdates()
                        YaMap(mvm, mapView, Modifier.fillMaxSize())
                    }
                }
            }
        }

    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
        MapKitFactory.getInstance().onStart()
    }

    override fun onResume() {
        super.onResume()
        if (mvm.requestLocationUpdates) mvm.startLocationUpdates()
    }

    override fun onPause() {
        super.onPause()
        mvm.stopLocationUpdates()
    }

    override fun onStop() {
        MapKitFactory.getInstance().onStop()
        super.onStop()
        mapView.onStart()
    }
}

@Composable
fun MainUI(
    mvm: MainViewModel,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Scaffold(
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(it)
        ) {
            content()
            if (!mvm.showRequestDialog) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                ) {
                    Column {
                        Button(
                            onClick = { if (!mvm.isTracking) mvm.startSession() else mvm.stopSession() },
                            modifier = Modifier
                                .padding(6.dp)
                                .align(Alignment.End)
                        ) {
                            Text(text = if (!mvm.isTracking) "Запустить трекинг" else "Остановить трекинг")
                        }
                        if (!mvm.isTracking) {
                            TracksCards(mvm = mvm)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TracksCards(
    mvm: MainViewModel
) {
    val sessions by mvm.sessions.collectAsState(initial = listOf())
    LazyRow(modifier = Modifier.fillMaxWidth()) {
        items(sessions) {
            ElevatedCard(
                modifier = Modifier.padding(4.dp),
                onClick = {
                    mvm.updateRouteForSession(it.id)
                }
            ) {
                Column {
                    Text(
                        text = it.id.toString(),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = it.startTime.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT)),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Normal,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = it.endTime.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT)),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Normal,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun YaMap(
    mvm: MainViewModel,
    mapView: MapView,
    modifier: Modifier = Modifier
) {
    val currentLocation by mvm.location.collectAsState()
    val currentRoute by mvm.currentRoute.collectAsState()

    currentRoute?.let {
        mapView.mapWindow.map.mapObjects.clear()
        val polylineBuilder = PolylineBuilder()
        it.forEach { location ->
            polylineBuilder.append(Point(location.latitude, location.longitude))
        }
        val polyline = polylineBuilder.build()
        mapView.mapWindow.map.mapObjects.addPolyline(polyline)
    }


    if (mvm.isTracking) {

        currentLocation?.let { location ->
            mapView.apply {
                mapWindow.map.move(
                    CameraPosition(
                        Point(location.latitude, location.longitude),
                        17.0f,
                        0.0f,
                        30.0f
                    ),
                    Animation(Animation.Type.SMOOTH, 1F),
                    null
                )
            }
        }
    }

    Box(modifier = modifier) {
        AndroidView(
            factory = {
                mapView.apply {
                    mapWindow.map.move(
                        CameraPosition(
                            Point(55.738035, 49.20867),
                            17.0f,
                            0.0f,
                            30.0f
                        )
                    )
                }

            },
            modifier = Modifier.matchParentSize()
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationRequestDialog(
    modifier: Modifier = Modifier,
    onDeny: () -> Unit,
    onAllow: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { onDeny() },
    ) {
        ElevatedCard(
            modifier = modifier.shadow(3.dp, shape = RoundedCornerShape(20.dp))
        ) {
            Column(
                modifier = Modifier
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    painterResource(id = R.drawable.twotone_not_listed_location_48),
                    contentDescription = null,
                    tint = colorResource(id = R.color.purple_700)
                )
                Text(stringResource(R.string.loc_permission_request))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Spacer(modifier = Modifier.weight(1f))

                    Button(onClick = { onAllow() }) {
                        Text("Yes")
                    }
                    Button(onClick = { onDeny() }) {
                        Text("No")
                    }
                }
            }
        }
    }
}