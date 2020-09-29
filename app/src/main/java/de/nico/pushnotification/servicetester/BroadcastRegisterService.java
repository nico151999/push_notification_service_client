package de.nico.pushnotification.servicetester;

import android.app.Service;
//import android.app.admin.DeviceAdminService;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import de.nico.pushnotification.servicetester.receiver.NotificationAppInstalledStatusReceiver;
import de.nico.pushnotification.servicetester.receiver.StartNotificationServiceReceiver;

public class BroadcastRegisterService extends /*DeviceAdmin*/Service {

    private static final String TAG = BroadcastRegisterService.class.getSimpleName();
    private ConnectivityManager mConnectivityManager;
    private NetworkStateCallback mNetworkStateCallback;
    private NotificationAppInstalledStatusReceiver mNotificationAppInstalledStatusReceiver;

    private final class NetworkStateCallback extends ConnectivityManager.NetworkCallback {
        @Override
        public void onAvailable(@NonNull Network network) {
            super.onAvailable(network);
            Log.i(TAG, "Network connection was established");
            sendBroadcast(
                    new Intent(
                            BroadcastRegisterService.this,
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
        Log.i(TAG, "Starting the broadcast register receiver service");
        registerNetworkCallback();
        registerApplicationInstallationStateReceiver();
    }

    private void registerNetworkCallback() {
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

    private void registerApplicationInstallationStateReceiver() {
        mNotificationAppInstalledStatusReceiver = new NotificationAppInstalledStatusReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_PACKAGE_ADDED);
        intentFilter.addDataScheme("package");
        intentFilter.setPriority(999);
        registerReceiver(mNotificationAppInstalledStatusReceiver, intentFilter);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "Stopping the broadcast register receiver service");
        unregisterNetworkCallback();
        unregisterApplicationInstallationStateReceiver();
    }

    private void unregisterNetworkCallback() {
        mConnectivityManager.unregisterNetworkCallback(mNetworkStateCallback);
        mNetworkStateCallback = null;
        mConnectivityManager = null;
    }

    private void unregisterApplicationInstallationStateReceiver() {
        unregisterReceiver(mNotificationAppInstalledStatusReceiver);
        mNotificationAppInstalledStatusReceiver = null;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        // todo: remove this method when we switch to a working DeviceAdminService
        return null;
    }
}
