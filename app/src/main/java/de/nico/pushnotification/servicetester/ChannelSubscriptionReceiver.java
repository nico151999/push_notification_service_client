package de.nico.pushnotification.servicetester;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

// todo: additionally receive apps connecting to the service
public class ChannelSubscriptionReceiver extends BroadcastReceiver {

    private static final String TAG = ChannelSubscriptionReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        String subscription = intent.getStringExtra(
                context.getString(R.string.EXTRA_SUBSCRIPTION)
        );
        if (subscription != null &&
                context.getString(R.string.ACTION_SUBSCRIBE_NOTIFICATION_CHANNEL)
                        .equals(intent.getAction())) {
            Log.i(TAG, "Trying to subscribe to " + subscription);
            try {
                context.startService(
                        new Intent(context, NotificationService.class)
                                .putExtra(
                                        NotificationService.SUBSCRIPTION_EXTRA_KEY,
                                        subscription
                                )
                );
            } catch (IllegalStateException e) {
                AdminStateReceiver.noBackgroundAppException(context, e);
            }
        }
    }
}
