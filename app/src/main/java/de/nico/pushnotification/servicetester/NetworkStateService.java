package de.nico.pushnotification.servicetester;

import android.app.Service;
//import android.app.admin.DeviceAdminService;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class NetworkStateService extends /*DeviceAdmin*/Service {

    private static final String TAG = NetworkStateService.class.getSimpleName();
    private ConnectivityManager mConnectivityManager;
    private NetworkStateCallback mNetworkStateCallback;

    private final class NetworkStateCallback extends ConnectivityManager.NetworkCallback {
        @Override
        public void onAvailable(@NonNull Network network) {
            super.onAvailable(network);
            Log.i(TAG, "Network connection was established");
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
        Log.i(TAG, "Starting the network state service");
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
        Log.i(TAG, "Starting the network state service");
        mConnectivityManager.unregisterNetworkCallback(mNetworkStateCallback);
        mNetworkStateCallback = null;
        mConnectivityManager = null;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        // todo: remove this method when we switch to a working DeviceAdminService
        return null;
    }
}
