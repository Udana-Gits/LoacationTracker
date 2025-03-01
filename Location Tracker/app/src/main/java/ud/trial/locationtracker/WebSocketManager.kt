package ud.trial.locationtracker

import kotlinx.coroutines.*
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.net.URI

class WebSocketManager(serverUrl: String, private val onMessageReceived: (String) -> Unit) {
    private var webSocketClient: WebSocketClient? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    init {
        connectWebSocket(serverUrl)
    }

    private fun connectWebSocket(serverUrl: String) {
        webSocketClient = object : WebSocketClient(URI(serverUrl)) {
            override fun onOpen(handshakedata: ServerHandshake?) {
                println("✅ WebSocket Connected to $serverUrl")
            }

            override fun onMessage(message: String?) {
                message?.let { onMessageReceived(it) }
            }

            override fun onClose(code: Int, reason: String?, remote: Boolean) {
                println("❌ WebSocket Disconnected: $reason")
            }

            override fun onError(ex: Exception?) {
                ex?.printStackTrace()
            }
        }
        webSocketClient?.connect()
    }

    // ✅ Corrected sendMessage() function
    fun sendMessage(message: String) {
        scope.launch {
            if (webSocketClient?.isOpen == true) {
                webSocketClient?.send(message)
            } else {
                println("❌ WebSocket is not connected, message not sent")
            }
        }
    }

    fun close() {
        webSocketClient?.close()
    }
}
