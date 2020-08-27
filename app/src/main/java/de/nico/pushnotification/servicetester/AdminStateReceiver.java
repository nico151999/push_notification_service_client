package de.nico.pushnotification.servicetester;

import android.app.admin.DeviceAdminReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

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
        startNotificationServiceBroadcast(context);
    }

    @Override
    public void onDisabled(@NonNull Context context, @NonNull Intent intent) {
        stopNotificationServiceBroadcast(context);
    }

    private void startNotificationServiceBroadcast(Context context) {
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
}