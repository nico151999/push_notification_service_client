package de.nico.pushnotification.servicetester;

import android.app.admin.DeviceAdminService;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;

import androidx.annotation.NonNull;

public class NetworkStateService extends DeviceAdminService {

    private ConnectivityManager mConnectivityManager;
    private NetworkStateCallback mNetworkStateCallback;

    private final class NetworkStateCallback extends ConnectivityManager.NetworkCallback {
        @Override
        public void onAvailable(@NonNull Network network) {
            super.onAvailable(network);
            sendBroadcast(
                    new Intent(
                            NetworkStateService.this,
                            StartNotificationServiceReceiver.class
                    ).putExtra(
                            StartNotificationServiceReceiver.START,
                            true
                    )
            );
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mConnectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        mNetworkStateCallback = new NetworkStateCallback();
        mConnectivityManager.registerNetworkCallback(
                new NetworkRequest.Builder()
                        .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
                        .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                        .build(),
                mNetworkStateCallback
        );
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mConnectivityManager.unregisterNetworkCallback(mNetworkStateCallback);
        mNetworkStateCallback = null;
        mConnectivityManager = null;
    }
}
