package de.nico.pushnotification.servicetester;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class ChannelSubscriptionReceiver extends BroadcastReceiver {

    private static final String TAG = ChannelSubscriptionReceiver.class.getSimpleName();
    public static final String EXTRA_SUBSCRIPTION = "EXTRA_SUBSCRIPTION";
    public static final String ACTION_SUBSCRIBE_NOTIFICATION_CHANNEL =
            "de.nico.pushnotification.servicetester.intent.action.SUBSCRIBE_NOTIFICATION_CHANNEL";

    @Override
    public void onReceive(Context context, Intent intent) {
        String subscription = intent.getStringExtra(EXTRA_SUBSCRIPTION);
        if (subscription != null && ACTION_SUBSCRIBE_NOTIFICATION_CHANNEL.equals(intent.getAction())) {
            try {
                context.startService(
                        new Intent(context, NotificationService.class)
                                .putExtra(
                                        NotificationService.SUBSCRIPTION_EXTRA_KEY,
                                        subscription
                                )
                );
            } catch (IllegalStateException e) {
                Log.e(TAG, "The app must be installed as system app to be able to start the notification service");
                e.printStackTrace();
            }
        }
    }
}
