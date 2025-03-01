package ud.trial.locationtracker

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import androidx.compose.runtime.*
import com.google.android.gms.location.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LocationTracker(context: Context) {
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    private val _locationFlow = MutableStateFlow<Location?>(null)
    val locationFlow = _locationFlow.asStateFlow()

    @SuppressLint("MissingPermission") // Ensure permissions are granted
    fun startTracking() {
        val locationRequest = LocationRequest.Builder(5000)
            .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
            .build()

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    _locationFlow.value = result.lastLocation
                }
            },
            null
        )
    }
}
