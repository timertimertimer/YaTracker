package com.example.yatracker.viewmodels

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.yatracker.database.Session
import com.example.yatracker.database.YaTrackerDatabase
import com.example.yatracker.locating.Locator
import com.example.yatracker.locating.Locator.locationCallback
import com.example.yatracker.locating.Locator.locationRequest
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import com.example.yatracker.database.Location as MyLocation

class MainViewModel(app: Application) : AndroidViewModel(app) {
    var showRequestDialog: Boolean by mutableStateOf(true)
    private var updJob: Job? = null

    private val fusedLocationClient = LocationServices
        .getFusedLocationProviderClient(app.applicationContext)

    var requestLocationUpdates by mutableStateOf(true)

    private val _location: MutableStateFlow<Location?> = MutableStateFlow(null)
    val location: StateFlow<Location?> = _location


    private val db = YaTrackerDatabase.getDao(
        getApplication<Application>().applicationContext
    )

    private val sessionDao = db.sessionDao()
    private val locationDao = db.locationDao()
    val sessions: Flow<List<Session>> get() = sessionDao.getAllSessions()
    private var currentSessionId: Long? = null

    var isTracking: Boolean by mutableStateOf(false)

    private val _currentRoute = MutableStateFlow<List<MyLocation>?>(null)
    val currentRoute: StateFlow<List<MyLocation>?> = _currentRoute

    @SuppressLint("MissingPermission")
    fun startLocationUpdates() {
        if (isPermissionsGranted(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                context = getApplication<Application>().applicationContext
            )
        ) {
            fusedLocationClient.lastLocation.addOnCompleteListener {
                viewModelScope.launch {
                    _location.emit(it.result)
                    fusedLocationClient.requestLocationUpdates(
                        locationRequest,
                        locationCallback,
                        Looper.getMainLooper()
                    )
                }
            }

            viewModelScope.launch {
                updJob = launch {
                    Locator.location.collect { location ->
                        _location.emit(location)
                        if (isTracking) {
                            location?.let {
                                saveLocationToTrack(
                                    MyLocation(
                                        sessionId = currentSessionId!!,
                                        latitude = it.latitude,
                                        longitude = it.longitude,
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    fun stopLocationUpdates() {
        updJob?.cancel()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    fun startSession() {
        viewModelScope.launch {
            val sessionId = sessionDao.insertSession(Session())
            currentSessionId = sessionId
            isTracking = true
        }
    }

    fun stopSession() {
        viewModelScope.launch {
            currentSessionId?.let { sessionId ->
                val session = sessionDao.getSessionById(sessionId)
                session.endTime = LocalDateTime.now()
                sessionDao.updateSession(session)
            }
        }
        isTracking = false
    }

    fun isPermissionsGranted(vararg permissions: String, context: Context) =
        permissions.fold(true) { acc, perm ->
            acc && context.checkSelfPermission(perm) == PackageManager.PERMISSION_GRANTED
        }

    private fun saveLocationToTrack(location: MyLocation) {
        viewModelScope.launch {
            locationDao.insertLocation(location)
        }
    }

    fun updateRouteForSession(sessionId: Long) {
        viewModelScope.launch {
            val locations = locationDao.getLocationsForSession(sessionId).first()
            _currentRoute.value = locations
        }
    }
}