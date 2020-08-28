package de.nico.pushnotification.servicetester;

import android.app.admin.DeviceAdminReceiver;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.PowerManager;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

public class AdminStateReceiver extends DeviceAdminReceiver {
    private static final String TAG = AdminStateReceiver.class.getSimpleName();

    @Override
    public void onReceive(@NonNull Context context, @NonNull Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            startNotificationServiceBroadcast(context);
        } else {
            super.onReceive(context, intent);
        }
    }

    @Override
    public void onEnabled(@NonNull Context context, @NonNull Intent intent) {
        if (!hasServerSpecs(context, false)) {
            Toast.makeText(
                    context,
                    R.string.set_server_specs,
                    Toast.LENGTH_LONG
            ).show();
            return;
        }
        startNotificationServiceBroadcast(context);
        startNetworkStateService(context);
    }

    @Override
    public void onDisabled(@NonNull Context context, @NonNull Intent intent) {
        stopNetworkStateService(context);
        stopNotificationServiceBroadcast(context);
        hasServerSpecs(context, true);
    }

    public static boolean isAdmin(Context context) {
        DevicePolicyManager dpm =
                (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        ComponentName mAdminName = new ComponentName(context, AdminStateReceiver.class);
        return (dpm != null && dpm.isAdminActive(mAdminName));
    }

    public static boolean hasServerSpecs(Context context, boolean delete) {
        SharedPreferences preferences = context.getSharedPreferences(
                StartNotificationServiceReceiver.SERVER_PREFERENCES,
                Context.MODE_PRIVATE
        );
        if (preferences.contains(StartNotificationServiceReceiver.IP_KEY) &&
                preferences.contains(StartNotificationServiceReceiver.PORT_KEY)) {
            if (delete) {
                preferences.edit()
                        .remove(StartNotificationServiceReceiver.IP_KEY)
                        .remove(StartNotificationServiceReceiver.PORT_KEY)
                        .apply();
            }
            return true;
        } else {
            return false;
        }
    }

    public static void startNotificationServiceBroadcast(Context context) {
        Log.i(TAG, "Device admin tries to start notification service");
        context.sendBroadcast(
                new Intent(
                        context,
                        StartNotificationServiceReceiver.class
                ).putExtra(
                        StartNotificationServiceReceiver.START,
                        true
                )
        );
    }

    private void stopNotificationServiceBroadcast(Context context) {
        Log.i(TAG, "Device admin tries to stop notification service");
        context.sendBroadcast(
                new Intent(
                        context,
                        StartNotificationServiceReceiver.class
                )
        );
    }

    public static void startNetworkStateService(Context context) {
        // todo: this method has to be removed cause this service will be started by the system
        //  when we switch to DeviceAdminService
        Log.i(TAG, "Device admin tries to start network state service");
        try {
            context.startService(
                    new Intent(
                            context,
                            NetworkStateService.class
                    )
            );
        } catch (IllegalStateException e) {
            noBackgroundAppException(context, e);
        }
    }

    private void stopNetworkStateService(Context context) {
        // todo: this method has to be removed cause this service will be started by the system
        //  when we switch to DeviceAdminService
        Log.i(TAG, "Device admin tries to stop network state service");
        try {
            context.stopService(
                    new Intent(
                            context,
                            NetworkStateService.class
                    )
            );
        } catch (IllegalStateException e) {
            noBackgroundAppException(context, e);
        }
    }

    public static void noBackgroundAppException(Context context, IllegalStateException e) {
        String errorMessage = context.getString(R.string.no_background_app);
        Log.e(TAG, errorMessage, e);
        Toast.makeText(
                context,
                errorMessage,
                Toast.LENGTH_LONG
        ).show();
    }
}