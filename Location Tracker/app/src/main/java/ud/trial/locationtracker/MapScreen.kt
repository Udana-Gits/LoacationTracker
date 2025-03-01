package ud.trial.locationtracker

import android.content.Context
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

@Composable
fun MapScreen(context: Context, apiKey: String, webSocketManager: WebSocketManager, deviceId: String) {
    var routePoints by remember { mutableStateOf<List<LatLng>>(emptyList()) }
    var currentLocation by remember { mutableStateOf<LatLng?>(null) }
    var trackedPhones by remember { mutableStateOf<Map<String, LatLng>>(emptyMap()) }

    val locationTracker = remember { LocationTracker(context) }

    LaunchedEffect(Unit) {
        locationTracker.startTracking()
        locationTracker.locationFlow.collectLatest { location ->
            location?.let {
                val latLng = LatLng(it.latitude, it.longitude)
                currentLocation = latLng

                // ✅ Send properly formatted JSON message
                val locationMessage = JSONObject().apply {
                    put("type", "location")
                    put("deviceId", deviceId)
                    put("lat", it.latitude)
                    put("lng", it.longitude)
                }

                webSocketManager.sendMessage(locationMessage.toString())
            }
        }

        // ✅ Fetch the real road route from Google Maps API
        routePoints = getRouteFromGoogleMaps(apiKey)
    }

    // ✅ Send a properly formatted connection message
    LaunchedEffect(webSocketManager) {
        val connectionMessage = JSONObject().apply {
            put("type", "connection")
            put("deviceId", deviceId)
        }
        webSocketManager.sendMessage(connectionMessage.toString())

        snapshotFlow { trackedPhones }.collectLatest {
            val requestMessage = JSONObject().apply {
                put("type", "request_live_locations")
            }
            webSocketManager.sendMessage(requestMessage.toString())
        }
    }

    DisposableEffect(Unit) {
        val messageListener: (String) -> Unit = { message ->
            try {
                val json = JSONObject(message)
                if (json.getString("type") == "update") {
                    val updatedPhones = trackedPhones.toMutableMap()
                    updatedPhones[json.getString("deviceId")] = LatLng(json.getDouble("lat"), json.getDouble("lng"))
                    trackedPhones = updatedPhones
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        onDispose {
            val disconnectMessage = JSONObject().apply {
                put("type", "disconnect")
                put("deviceId", deviceId)
            }
            webSocketManager.sendMessage(disconnectMessage.toString())
        }
    }

    AndroidView(
        factory = { ctx ->
            MapView(ctx).apply {
                onCreate(null)
                getMapAsync { googleMap ->
                    googleMap.uiSettings.isZoomControlsEnabled = true
                    googleMap.uiSettings.isCompassEnabled = true

                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(7.2906, 80.6337), 10f))

                    val polylineOptions = PolylineOptions().apply {
                        addAll(routePoints)
                        color(0xFF007AFF.toInt()) // Blue road path
                        width(10f)
                    }
                    googleMap.addPolyline(polylineOptions)
                }
            }
        },
        update = { mapView ->
            currentLocation?.let { location ->
                mapView.getMapAsync { googleMap ->
                    googleMap.clear()

                    // ✅ Add markers for all tracked phones
                    trackedPhones.forEach { (id, latLng) ->
                        googleMap.addMarker(
                            MarkerOptions()
                                .position(latLng)
                                .title("Tracking: $id")
                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                        )
                    }

                    // ✅ Add marker for this viewer device
                    googleMap.addMarker(
                        MarkerOptions()
                            .position(location)
                            .title("My Location")
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
                    )

                    googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(location, 12f))

                    val polylineOptions = PolylineOptions().apply {
                        addAll(routePoints)
                        color(0xFF007AFF.toInt())
                        width(10f)
                    }
                    googleMap.addPolyline(polylineOptions)
                }
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

/**
 * ✅ Fetch the real road route from Google Directions API
 */
suspend fun getRouteFromGoogleMaps(apiKey: String): List<LatLng> {
    return withContext(Dispatchers.IO) {
        try {
            val origin = "7.2906,80.6337" // Kandy
            val destination = "6.9271,79.8612" // Colombo
            val url = "https://maps.googleapis.com/maps/api/directions/json?origin=$origin&destination=$destination&key=$apiKey"

            val response = URL(url).readText()
            val json = JSONObject(response)

            val route = json.getJSONArray("routes").getJSONObject(0)
                .getJSONArray("legs").getJSONObject(0)
                .getJSONArray("steps")

            val path = mutableListOf<LatLng>()
            for (i in 0 until route.length()) {
                val step = route.getJSONObject(i)
                val start = step.getJSONObject("start_location")
                val end = step.getJSONObject("end_location")

                path.add(LatLng(start.getDouble("lat"), start.getDouble("lng")))
                path.add(LatLng(end.getDouble("lat"), end.getDouble("lng")))
            }

            path
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}
