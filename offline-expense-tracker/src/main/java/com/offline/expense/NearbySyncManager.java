package com.offline.expense;

import android.app.Activity;

import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.AdvertisingOptions;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.ConnectionsClient;
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo;
import com.google.android.gms.nearby.connection.DiscoveryOptions;
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;
import com.google.android.gms.nearby.connection.Strategy;

import java.nio.charset.StandardCharsets;

/**
 * Thin wrapper around Play Services Nearby Connections so one split-expense record can be sent
 * device-to-device (Bluetooth/Wi-Fi Direct, picked automatically by the API) with no server and
 * no internet — the same offline promise as the rest of the app. One side advertises (receiver,
 * waits to be found), the other discovers and connects (sender, then pushes one JSON payload).
 */
final class NearbySyncManager {
    private static final String SERVICE_ID = "com.offline.expense.SPLIT_SYNC";
    private static final Strategy STRATEGY = Strategy.P2P_POINT_TO_POINT;

    interface Listener {
        void onStatus(String message);
        void onConnected(String endpointId);
        void onPayloadReceived(byte[] bytes);
        void onError(String message);
    }

    private final ConnectionsClient connectionsClient;
    private final Listener listener;
    private String connectedEndpointId;

    NearbySyncManager(Activity activity, Listener listener) {
        this.connectionsClient = Nearby.getConnectionsClient(activity);
        this.listener = listener;
    }

    private final ConnectionLifecycleCallback connectionLifecycleCallback = new ConnectionLifecycleCallback() {
        @Override
        public void onConnectionInitiated(String endpointId, ConnectionInfo connectionInfo) {
            // Both sides already confirm what they're sending/accepting via the app's own
            // confirm dialogs before/after transfer, so auto-accept the raw connection itself.
            connectionsClient.acceptConnection(endpointId, payloadCallback);
        }

        @Override
        public void onConnectionResult(String endpointId, ConnectionResolution result) {
            if (result.getStatus().isSuccess()) {
                connectedEndpointId = endpointId;
                listener.onStatus("Connected");
                listener.onConnected(endpointId);
            } else {
                listener.onError("Connection failed");
            }
        }

        @Override
        public void onDisconnected(String endpointId) {
            if (endpointId.equals(connectedEndpointId)) connectedEndpointId = null;
            listener.onStatus("Disconnected");
        }
    };

    private final PayloadCallback payloadCallback = new PayloadCallback() {
        @Override
        public void onPayloadReceived(String endpointId, Payload payload) {
            if (payload.getType() == Payload.Type.BYTES) {
                listener.onPayloadReceived(payload.asBytes());
            }
        }

        @Override
        public void onPayloadTransferUpdate(String endpointId, PayloadTransferUpdate update) {
            if (update.getStatus() == PayloadTransferUpdate.Status.SUCCESS) {
                listener.onStatus("Sent");
            }
        }
    };

    void startAdvertising(String localName) {
        AdvertisingOptions options = new AdvertisingOptions.Builder().setStrategy(STRATEGY).build();
        connectionsClient.startAdvertising(localName, SERVICE_ID, connectionLifecycleCallback, options)
                .addOnSuccessListener(unused -> listener.onStatus("Waiting for a nearby device..."))
                .addOnFailureListener(e -> listener.onError("Couldn't start: " + e.getMessage()));
    }

    void startDiscovery(String localName) {
        DiscoveryOptions options = new DiscoveryOptions.Builder().setStrategy(STRATEGY).build();
        EndpointDiscoveryCallback discoveryCallback = new EndpointDiscoveryCallback() {
            @Override
            public void onEndpointFound(String endpointId, DiscoveredEndpointInfo info) {
                connectionsClient.requestConnection(localName, endpointId, connectionLifecycleCallback)
                        .addOnFailureListener(e -> listener.onError("Couldn't connect: " + e.getMessage()));
            }

            @Override
            public void onEndpointLost(String endpointId) {}
        };
        connectionsClient.startDiscovery(SERVICE_ID, discoveryCallback, options)
                .addOnSuccessListener(unused -> listener.onStatus("Looking for a nearby device..."))
                .addOnFailureListener(e -> listener.onError("Couldn't start: " + e.getMessage()));
    }

    void sendJson(String json) {
        if (connectedEndpointId == null) {
            listener.onError("Not connected yet");
            return;
        }
        connectionsClient.sendPayload(connectedEndpointId, Payload.fromBytes(json.getBytes(StandardCharsets.UTF_8)));
    }

    void stop() {
        connectionsClient.stopAdvertising();
        connectionsClient.stopDiscovery();
        connectionsClient.stopAllEndpoints();
    }
}
