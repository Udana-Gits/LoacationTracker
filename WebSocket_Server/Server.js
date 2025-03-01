const WebSocket = require('ws');

const wss = new WebSocket.Server({ port: 8080 });

let connectedClients = new Map();

wss.on('connection', ws => {
    console.log("✅ New client connected");

    ws.on('message', message => {
        console.log(`📩 Received: ${message}`);

        try {
            let data = JSON.parse(message);

            if (data.type === "connection") {
                console.log(`✅ Device Connected: ${data.deviceId}`);
            } else if (data.type === "request_live_locations") {
                console.log("📡 Received live location request");
            } else if (data.type === "location") {
                connectedClients.set(data.deviceId, { lat: data.lat, lng: data.lng });

                // ✅ Broadcast updated location to all viewers
                wss.clients.forEach(client => {
                    if (client.readyState === WebSocket.OPEN) {
                        client.send(JSON.stringify({
                            type: "update",
                            deviceId: data.deviceId,
                            lat: data.lat,
                            lng: data.lng
                        }));
                    }
                });
            } else if (data.type === "disconnect") {
                console.log(`❌ Device Disconnected: ${data.deviceId}`);
                connectedClients.delete(data.deviceId);
            }
        } catch (error) {
            console.error("❌ Error processing message:", error);
            console.error("🔴 Received raw message:", message); // Debug log
        }
    });

    ws.on('close', () => {
        console.log("❌ Client disconnected");
    });
});

console.log("🚀 WebSocket Server started on port 8080");
