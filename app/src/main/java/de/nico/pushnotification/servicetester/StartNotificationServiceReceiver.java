package de.nico.pushnotification.servicetester;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

public class StartNotificationServiceReceiver extends BroadcastReceiver {
    private static final String TAG = "TAG";
    public static final String START = "START";
    public static final String SERVER_PREFERENCES = "server_config";
    public static final String IP_KEY = "ip";
    public static final String PORT_KEY = "port";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getBooleanExtra(START, false)) {
            SharedPreferences preferences =
                    context.getSharedPreferences(SERVER_PREFERENCES, Context.MODE_PRIVATE);
            String ip = preferences.getString(IP_KEY, null);
            int port = preferences.getInt(PORT_KEY, -1);
            if (ip != null && port != -1) {
                try {
                    context.startService(
                            new Intent(context, NotificationService.class)
                                    .putExtra(
                                            NotificationService.IP_EXTRA_KEY,
                                            ip
                                    )
                                    .putExtra(
                                            NotificationService.PORT_EXTRA_KEY,
                                            port
                                    )
                    );
                } catch (IllegalStateException e) {
                    AdminStateReceiver.noBackgroundAppException(context, e);
                }
            }
        } else {
            context.stopService(
                    new Intent(context, NotificationService.class)
            );
        }
    }
}
