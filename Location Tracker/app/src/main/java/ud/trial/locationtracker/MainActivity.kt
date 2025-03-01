package ud.trial.locationtracker

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {
    private lateinit var webSocketManager: WebSocketManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request location permissions
        requestLocationPermissions()

        val apiKey = "AIzaSyB0GOhcgbuW64h9V4OTf6HYFj4bbaT6aEc"
        val deviceId = "Phone1" // Unique device identifier

        // ✅ Pass a lambda function for `onMessageReceived`
        webSocketManager = WebSocketManager("ws://192.168.56.1:8080") { message ->
            println("📩 Received WebSocket message: $message")
        }

        setContent {
            MapScreen(context = this, apiKey = apiKey, webSocketManager = webSocketManager, deviceId = deviceId)
        }
    }

    private fun requestLocationPermissions() {
        val permissions = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        if (permissions.any { ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }) {
            ActivityCompat.requestPermissions(this, permissions, 1)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        webSocketManager.close()
    }
}
