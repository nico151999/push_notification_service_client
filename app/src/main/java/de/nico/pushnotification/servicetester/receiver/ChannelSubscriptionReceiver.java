package de.nico.pushnotification.servicetester.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import de.nico.pushnotification.servicetester.NotificationService;

public class ChannelSubscriptionReceiver extends BroadcastReceiver {

    private static final String TAG = ChannelSubscriptionReceiver.class.getSimpleName();
    private static final String SUBSCRIPTION_PACKAGE_KEY = "SUBSCRIPTION_PACKAGE_KEY";
    private static final String SUBSCRIPTION_CHANNEL_KEY = "SUBSCRIPTION_CHANNEL_KEY";
    private static final String ACTION_SUBSCRIBE_NOTIFICATION_CHANNEL = "de.nico.pushnotification.servicetester.action.SUBSCRIBE_NOTIFICATION_CHANNEL";

    // TODO: currently an app could pretend to be an app that is already registered and subscribe to channels in its name.
    //  this needs to be fixed
    @Override
    public void onReceive(Context context, Intent intent) {
        String subscription = intent.getStringExtra(SUBSCRIPTION_CHANNEL_KEY);
        String subscriber = intent.getStringExtra(SUBSCRIPTION_PACKAGE_KEY);
        if (subscription != null && subscriber != null &&
                ACTION_SUBSCRIBE_NOTIFICATION_CHANNEL.equals(intent.getAction())) {
            Log.i(TAG, "Trying to let " + subscriber + " subscribe to " + subscription);
            try {
                context.startService(
                        new Intent(context, NotificationService.class)
                                .putExtra(
                                        NotificationService.SUBSCRIPTION_CHANNEL_EXTRA_KEY,
                                        subscription
                                ).putExtra(
                                        NotificationService.SUBSCRIPTION_EXTRA_KEY,
                                        subscriber
                                )
                );
            } catch (IllegalStateException e) {
                AdminStateReceiver.noBackgroundAppException(context, e);
            }
        }
    }
}
