package de.nico.pushnotification.servicetester.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.util.Log;

import de.nico.pushnotification.servicetester.NotificationService;

public class NotificationAppInstalledStatusReceiver extends BroadcastReceiver {
    private static final String TAG = NotificationAppInstalledStatusReceiver.class.getSimpleName();
    public static final String RECEIVE_NOTIFICATION_PERMISSION = "de.nico.pushnotification.servicetester.permission.RECEIVE_NOTIFICATION";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action == null) {
            Log.e(TAG, "Intent does have no action");
            return;
        }

        Uri uri = intent.getData();
        if (uri == null) {
            Log.e(TAG, "Intent does have no action");
            return;
        }

        String packageName = intent.getData().getSchemeSpecificPart();

        switch (action) {
            case Intent.ACTION_PACKAGE_ADDED:
                try {
                    onPackageAdded(packageName, context);
                } catch (PackageManager.NameNotFoundException e) {
                    Log.e(TAG, "Failed adding package", e);
                }
                break;
            case Intent.ACTION_PACKAGE_FULLY_REMOVED:
                onPackageRemoved(packageName, context);
                break;
        }
    }

    private void onPackageAdded(String pkg, Context context) throws PackageManager.NameNotFoundException {
        PackageInfo pi = context.getPackageManager().getPackageInfo(pkg, PackageManager.GET_PERMISSIONS);
        boolean hasNotificationPermission = false;
        for (String permission : pi.requestedPermissions) {
            if (permission.equals(RECEIVE_NOTIFICATION_PERMISSION)) {
                hasNotificationPermission = true;
                break;
            }
        }
        if (!hasNotificationPermission) {
            return;
        }
        Log.i(TAG, "Adding subscriber " + pkg);
        try {
            context.startService(
                    new Intent(context, NotificationService.class)
                            .putExtra(
                                    NotificationService.SUBSCRIPTION_EXTRA_KEY,
                                    pkg
                            )
            );
        } catch (IllegalStateException e) {
            AdminStateReceiver.noBackgroundAppException(context, e);
        }
    }

    private void onPackageRemoved(String pkg, Context context) {
        Log.i(TAG, pkg + " was removed. If it was registered as notification receiver, it will be unregistered.");
        try {
            context.startService(
                    new Intent(context, NotificationService.class)
                            .putExtra(
                                    NotificationService.UNSUBSCRIPTION_EXTRA_KEY,
                                    pkg
                            )
            );
        } catch (IllegalStateException e) {
            AdminStateReceiver.noBackgroundAppException(context, e);
        }
    }
}
