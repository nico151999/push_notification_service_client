package de.nico.pushnotification.servicetester;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.Process;
import android.util.Base64;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.function.Consumer;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class NotificationService extends Service {
    private static final String TAG = NotificationService.class.getSimpleName();
    public static final String SUBSCRIPTION_EXTRA_KEY = "SUBSCRIPTION_EXTRA_KEY";
    public static final String SUBSCRIPTIONS_EXTRA_KEY = "SUBSCRIPTIONS_EXTRA_KEY";
    public static final String IP_EXTRA_KEY = "IP_EXTRA_KEY";
    public static final String PORT_EXTRA_KEY = "PORT_EXTRA_KEY";
    private static final String CHANNEL_ID = "NOTIFICATION_SERVICE_CHANNEL_ID";
    private static final String TITLE_KEY = "title";
    private static final String CONTENT_KEY = "message";
    private static final String URI_KEY = "uri";
    private static final String ICON_KEY = "icon";

    private boolean mServiceRunning;
    private Looper mServiceLooper;
    private Handler mServiceHandler;
    private final NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(
            this,
            CHANNEL_ID
    ).setSmallIcon(
            R.drawable.ic_launcher_foreground
    ).setPriority(
            NotificationCompat.PRIORITY_DEFAULT
    );
    private int mId = -2147483648;
    private NotificationManagerCompat mNotificationManager;
    private PushNotificationClient mPushNotificationClient;


    private final class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) {
            super(looper);
        }

        private void delayedRetry(Message msg) {
            try {
                Thread.sleep(30000);
            } catch (InterruptedException ex) {
                Log.e(TAG, "An error occurred. The service is shutting down now...");
                stopSelf(msg.arg1);
                return;
            }
            if (isNetworkConnectionPresent()) {
                Log.i(TAG, "Trying to connect to notification server again...");
                handleMessage(msg);
            } else {
                delayedRetry(msg);
            }
        }

        private boolean isNetworkConnectionPresent() {
            ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm != null) {
                Network network = cm.getActiveNetwork();
                if (network != null) {
                    NetworkCapabilities nc = cm.getNetworkCapabilities(network);
                    if (nc != null) {
                        return new ArrayList<Integer>() {{
                            add(NetworkCapabilities.TRANSPORT_CELLULAR);
                            add(NetworkCapabilities.TRANSPORT_WIFI);
                        }}.parallelStream().anyMatch(nc::hasTransport);
                    }
                }
            }
            return false;
        }

        @Override
        public void handleMessage(Message msg) {
            Intent intent = (Intent)msg.obj;

            Log.i(TAG, "Started notification handler");
            String ip = intent.getStringExtra(IP_EXTRA_KEY);
            int port = intent.getIntExtra(PORT_EXTRA_KEY, -1);
            if (ip == null || port == -1) {
                throw new IllegalArgumentException("The service requires an IP and a port to be passed");
            }
            if (!isNetworkConnectionPresent()) {
                delayedRetry(msg);
                return;
            }
            try {
                mPushNotificationClient = new PushNotificationClient(ip, port);
            } catch (IOException e) {
                Log.e(TAG, "Could not establish connection to notification server");
                delayedRetry(msg);
                return;
            }
            try {
                mPushNotificationClient.addOnReceiveMessageListeners(new ArrayList<Consumer<String>>() {{
                    add((notification) -> {
                        if (notification == null) {
                            Log.i(TAG, "The connection to the server was closed");
                            try {
                                mPushNotificationClient.stopConnection();
                            } catch (IOException ignored) {}
                            return;
                        }
                        synchronized (mBuilder) {
                            try {
                                JSONObject messageJSON = new JSONObject(notification);
                                mBuilder
                                        .setLargeIcon(null)
                                        .setContentIntent(null)
                                        .setContentTitle(messageJSON.getString(TITLE_KEY))
                                        .setContentText(messageJSON.getString(CONTENT_KEY));
                                if (messageJSON.has(ICON_KEY)) {
                                    try {
                                        byte[] b = Base64.decode(messageJSON.getString(ICON_KEY), Base64.DEFAULT);
                                        mBuilder.setLargeIcon(
                                                BitmapFactory.decodeByteArray(b, 0, b.length)
                                        );
                                    } catch (IllegalArgumentException e) {
                                        Log.e(TAG, "The submitted base64 image has an improper format");
                                    }
                                }
                                if (messageJSON.has(URI_KEY)) {
                                    mBuilder
                                            .setAutoCancel(true)
                                            .setContentIntent(
                                                    PendingIntent.getActivity(
                                                            getApplicationContext(),
                                                            (int) System.currentTimeMillis(),
                                                            new Intent(
                                                                    Intent.ACTION_VIEW,
                                                                    Uri.parse(messageJSON.getString(URI_KEY))
                                                            ),
                                                            PendingIntent.FLAG_UPDATE_CURRENT
                                                    )
                                            );
                                }
                            } catch (JSONException e) {
                                Log.e(TAG, "Failed to parse notification specifications");
                                return;
                            }
                            mNotificationManager.notify(
                                    mId++,
                                    mBuilder.build()
                            );
                        }
                    });
                }}, true);
            } catch (IOException e) {
                Log.e(TAG, "Cannot read server input stream");
                delayedRetry(msg);
                return;
            }
            stopSelf(msg.arg1);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        String packageName = getPackageName();
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (!pm.isIgnoringBatteryOptimizations(packageName)) {
            startActivity(
                    new Intent()
                            .setAction(
                                    android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                            )
                            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            .setData(Uri.parse("package:" + packageName))
            );
        }
        getSystemService(NotificationManager.class).createNotificationChannel(
                new NotificationChannel(
                        CHANNEL_ID,
                        getString(R.string.channel_name),
                        NotificationManager.IMPORTANCE_DEFAULT
                )
        );
        mNotificationManager = NotificationManagerCompat.from(this);

        HandlerThread thread = new HandlerThread(
                "Notification Service Handler",
                Process.THREAD_PRIORITY_LOWEST
        );
        thread.start();
        mServiceLooper = thread.getLooper();
        mServiceHandler = new ServiceHandler(mServiceLooper);
    }

    @Override
    public synchronized int onStartCommand(@Nullable Intent intent, int flags, int startId) {
        if (intent != null) {
            if (mPushNotificationClient != null && intent.hasExtra(SUBSCRIPTION_EXTRA_KEY)) {
                mPushNotificationClient.sendMessage(
                        intent.getStringExtra(SUBSCRIPTIONS_EXTRA_KEY)
                );
            } else if (!mServiceRunning) {
                mServiceRunning = true;
                Message msg = mServiceHandler.obtainMessage();
                msg.arg1 = startId;
                msg.obj = intent;
                mServiceHandler.sendMessage(msg);
                return START_REDELIVER_INTENT;
            }
        }
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "Notification service destroyed");
        mServiceRunning = false;
        if (mPushNotificationClient != null) {
            try {
                mPushNotificationClient.stopConnection();
            } catch (IOException ignore) {}
        }
        mServiceLooper.quit();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
