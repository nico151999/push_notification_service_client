package de.nico.pushnotification.servicetester;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class StartNotificationServiceReceiver extends BroadcastReceiver {
    private static final String TAG = "TAG";
    public static final String START = "START";
    private static final String IP = "192.168.178.42";
    private static final int PORT = 12345;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getBooleanExtra(START, false)) {
            try {
                context.startService(
                        new Intent(context, NotificationService.class)
                                .putExtra(
                                        NotificationService.IP_EXTRA_KEY,
                                        IP
                                )
                                .putExtra(
                                        NotificationService.PORT_EXTRA_KEY,
                                        PORT
                                )
                );
            } catch (IllegalStateException e) {
                Log.e(TAG, "The app must be installed as system app to be able to start the notification service");
                e.printStackTrace();
            }
        } else {
            context.stopService(
                    new Intent(context, NotificationService.class)
            );
        }
    }
}
