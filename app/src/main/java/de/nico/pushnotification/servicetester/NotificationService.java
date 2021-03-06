package de.nico.pushnotification.servicetester;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.util.Log;

import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.couchbase.lite.Array;
import com.couchbase.lite.CouchbaseLite;
import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.DataSource;
import com.couchbase.lite.Database;
import com.couchbase.lite.DatabaseConfiguration;
import com.couchbase.lite.Dictionary;
import com.couchbase.lite.Expression;
import com.couchbase.lite.Meta;
import com.couchbase.lite.MutableArray;
import com.couchbase.lite.MutableDocument;
import com.couchbase.lite.Query;
import com.couchbase.lite.QueryBuilder;
import com.couchbase.lite.Result;
import com.couchbase.lite.ResultSet;
import com.couchbase.lite.SelectResult;

import de.nico.pushnotification.servicetester.message.ApplicationSubscriptionMessage;
import de.nico.pushnotification.servicetester.message.ApplicationUnsubscriptionMessage;
import de.nico.pushnotification.servicetester.message.ChannelSubscriptionMessage;
import de.nico.pushnotification.servicetester.message.ClientsMessage;
import de.nico.pushnotification.servicetester.message.PushNotificationMessage;

import static de.nico.pushnotification.servicetester.receiver.NotificationAppInstalledStatusReceiver.RECEIVE_NOTIFICATION_PERMISSION;

public class NotificationService extends Service {
    private static final String TAG = NotificationService.class.getSimpleName();
    public static final String SUBSCRIPTION_EXTRA_KEY = "SUBSCRIPTION_EXTRA_KEY";
    public static final String UNSUBSCRIPTION_EXTRA_KEY = "UNSUBSCRIPTION_EXTRA_KEY";
    public static final String SUBSCRIPTION_CHANNEL_EXTRA_KEY = "SUBSCRIPTION_CHANNEL_EXTRA_KEY";
    public static final String IP_EXTRA_KEY = "IP_EXTRA_KEY";
    public static final String PORT_EXTRA_KEY = "PORT_EXTRA_KEY";
    private static final String DB_PACKAGE_KEY = "package";
    private static final String DB_SUBSCRIPTIONS_KEY = "subscriptions";
    private static final String DB_NAME = "subscribers";
    private static final String NOTIFICATION_LIBRARY_RECEIVER = "de.nico.pushnotification.library.NotificationReceiver";
    private static final String ACTION_SHOW_NOTIFICATION = "de.nico.pushnotification.library.action.SHOW_NOTIFICATION";
    private static final String PERMISSION_RECEIVE_NOTIFICATION = "de.nico.pushnotification.servicetester.permission.RECEIVE_NOTIFICATION";

    private boolean mServiceRunning;
    private Handler mServiceHandler;
    private PushNotificationClient mPushNotificationClient;


    private final class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) {
            super(looper);
        }

        private void delayedRetry(Message msg) {
            if (mPushNotificationClient != null) {
                try {
                    mPushNotificationClient.stopConnection();
                } catch (IOException ignore) {}
            }
            if (mServiceRunning) {
                try {
                    Thread.sleep(30000);
                    if (isNetworkConnectionPresent() && mServiceRunning) {
                        Log.i(TAG, "Trying to connect to notification server again...");
                        handleMessage(msg);
                        return;
                    }
                } catch (InterruptedException ex) {
                    Log.e(TAG, "An error occurred. The service is shutting down now...");
                }
            }
            stopSelf(msg.arg1);
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
        public void handleMessage(final Message msg) {
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

            Database database;
            try {
                database = initSubscriberDatabase();
            } catch (CouchbaseLiteException e) {
                Log.e(TAG, "Failed to create database object", e);
                stopSelf(msg.arg1);
                return;
            }
            Query query = QueryBuilder.select(SelectResult.all())
                    .from(DataSource.database(database));
            ResultSet result;
            try {
                result = query.execute();
            } catch (CouchbaseLiteException e) {
                Log.e(TAG, "Failed to query database", e);
                stopSelf(msg.arg1);
                return;
            }
            ClientsMessage message = new ClientsMessage();
            List<ApplicationInfo> packages = getPackageManager().getInstalledApplications(PackageManager.GET_META_DATA);
            apploop: for (ApplicationInfo applicationInfo : packages) {
                PackageInfo pi;
                String packageName = applicationInfo.packageName;
                try {
                    pi = getPackageManager().getPackageInfo(packageName, PackageManager.GET_PERMISSIONS);
                } catch (PackageManager.NameNotFoundException e) {
                    continue;
                }
                String[] permissions = pi.requestedPermissions;
                if (permissions != null) {
                    for (String permission : permissions) {
                        if (permission.equals(RECEIVE_NOTIFICATION_PERMISSION)) {
                            // todo: query database with where statement instead and see if we get one or no result
                            for (Result res : result) {
                                Dictionary dic = res.getDictionary(DB_NAME);
                                String pkg = dic.getString(DB_PACKAGE_KEY);
                                if (packageName.equals(pkg)) {
                                    Array channels = dic.getArray(DB_SUBSCRIPTIONS_KEY);
                                    message.put(
                                            pkg,
                                            (List<String>) (List<?>) (channels == null ? new ArrayList<>() : channels.toList())
                                    );
                                    continue apploop;
                                }
                            }
                            MutableDocument mutableDoc = new MutableDocument()
                                    .setString(DB_PACKAGE_KEY, packageName)
                                    .setArray(DB_SUBSCRIPTIONS_KEY, new MutableArray());
                            try {
                                database.save(mutableDoc);
                            } catch (CouchbaseLiteException e) {
                                Log.e(TAG, "Failed adding package to registered packages", e);
                                break;
                            }
                            message.put(
                                    packageName,
                                    new ArrayList<>()
                            );
                            break;
                        }
                    }
                }
            }
            // TODO: see TODO @handleSubscriptionIntent method
            mPushNotificationClient.sendMessage(message);
            try {
                mPushNotificationClient.addOnReceiveMessageListeners(new ArrayList<Consumer<String>>() {
                    private static final String TITLE_KEY = "title";
                    private static final String CONTENT_KEY = "content";
                    private static final String ICON_KEY = "icon";
                    private static final String URI_KEY = "uri";

                    {
                    add((notification) -> {
                        if (notification == null) {
                            Log.i(TAG, "The connection to the server was closed");
                            delayedRetry(msg);
                            return;
                        }
                        try {
                            PushNotificationMessage message = PushNotificationMessage.parse(notification);
                            Intent intent = new Intent()
                                    .putExtra(TITLE_KEY, message.getTitle())
                                    .putExtra(CONTENT_KEY, message.getContent());
                            byte[] b = message.getIcon();
                            if (b != null) {
                                intent.putExtra(ICON_KEY, b);
                            }
                            String uri = message.getUri();
                            if (uri != null) {
                                intent.putExtra(URI_KEY, uri);
                            }
                            sendBroadcast(
                                    intent
                                            .setAction(ACTION_SHOW_NOTIFICATION)
                                            .setComponent(new ComponentName(
                                                    message.getReceiverPackage(),
                                                    NOTIFICATION_LIBRARY_RECEIVER
                                            )),
                                    PERMISSION_RECEIVE_NOTIFICATION
                            );
                        } catch (JSONException e) {
                            Log.e(TAG, "Failed to parse notification specifications", e);
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
        HandlerThread thread = new HandlerThread(
                "Notification Service Handler",
                Process.THREAD_PRIORITY_LOWEST
        );
        thread.start();
        mServiceHandler = new ServiceHandler(thread.getLooper());
    }

    @Override
    public synchronized int onStartCommand(@Nullable Intent intent, int flags, int startId) {
        if (intent != null) {
            if (mPushNotificationClient != null) {
                if (intent.hasExtra(SUBSCRIPTION_EXTRA_KEY)) {
                    new Thread() {
                        @Override
                        public void run() {
                            handleSubscriptionIntent(intent);
                        }
                    }.start();
                } else if (intent.hasExtra(UNSUBSCRIPTION_EXTRA_KEY)) {
                    new Thread() {
                        @Override
                        public void run() {
                            handleUnsubscriptionIntent(intent);
                        }
                    }.start();
                }
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

    // TODO: consider not saving registered locations locally but in a database maintained by the server
    //  this requires a unique identifier the device sends every time it connects to the server
    private synchronized void handleSubscriptionIntent(@NonNull Intent intent) {
        Database database;
        try {
            database = initSubscriberDatabase();
        } catch (CouchbaseLiteException e) {
            Log.e(TAG, "Failed to create database object", e);
            return;
        }
        String subscriber = intent.getStringExtra(SUBSCRIPTION_EXTRA_KEY);
        Query query = QueryBuilder.select(SelectResult.expression(Meta.id))
                .from(DataSource.database(database))
                .where(Expression.property(DB_PACKAGE_KEY).equalTo(Expression.string(subscriber)));
        ResultSet result;
        try {
            result = query.execute();
        } catch (CouchbaseLiteException e) {
            Log.e(TAG, "Cannot query database", e);
            return;
        }
        Result res = result.next();
        if (intent.hasExtra(SUBSCRIPTION_CHANNEL_EXTRA_KEY)) {
            if (res == null) {
                Log.e(TAG, "A package must be registered before it can subscribe to channels");
                return;
            }
            String subscription = intent.getStringExtra(SUBSCRIPTION_CHANNEL_EXTRA_KEY);
            if (subscription.length() == 0) {
                Log.e(TAG, "The channel that is to be subscribed to must not be empty");
                return;
            }
            MutableDocument document = database.getDocument(res.getString("id")).toMutable();
            MutableArray subscriptions = document.getArray(DB_SUBSCRIPTIONS_KEY);
            if (!subscriptions.toList().contains(subscription)) {
                subscriptions.addString(subscription);
                try {
                    database.save(document);
                } catch (CouchbaseLiteException e) {
                    Log.e(TAG, "Could not add subscription to database", e);
                    return;
                }
                mPushNotificationClient.sendMessage(new ChannelSubscriptionMessage(subscriber, subscription));
            }
        } else {
            if (res != null) {
                Log.i(TAG, "Application " + subscriber + " is already registered");
                return;
            }
            MutableDocument mutableDoc = new MutableDocument()
                    .setString(DB_PACKAGE_KEY, subscriber)
                    .setArray(DB_SUBSCRIPTIONS_KEY, new MutableArray());
            try {
                database.save(mutableDoc);
            } catch (CouchbaseLiteException e) {
                Log.e(TAG, "Failed adding package to registered packages", e);
            }
            mPushNotificationClient.sendMessage(new ApplicationSubscriptionMessage(subscriber));
        }
    }

    // todo: also handle unsubscription of channels and notify the server about channel unsubscriptions
    private synchronized void handleUnsubscriptionIntent(@NonNull Intent intent) {
        Database database;
        try {
            database = initSubscriberDatabase();
        } catch (CouchbaseLiteException e) {
            Log.e(TAG, "Failed to create database object", e);
            return;
        }
        String unsubscriber = intent.getStringExtra(UNSUBSCRIPTION_EXTRA_KEY);
        Query query = QueryBuilder.select(SelectResult.expression(Meta.id))
                .from(DataSource.database(database))
                .where(Expression.property(DB_PACKAGE_KEY).equalTo(Expression.string(unsubscriber)));
        ResultSet result;
        try {
            result = query.execute();
        } catch (CouchbaseLiteException e) {
            Log.e(TAG, "Failed to query unsubscriber's id", e);
            return;
        }
        try {
            for (Result res : result) {
                database.delete(database.getDocument(res.getString("id")));
            }
        } catch (CouchbaseLiteException e) {
            Log.e(TAG, "Failed to delete unsubscriber from subscribers", e);
        }
        mPushNotificationClient.sendMessage(new ApplicationUnsubscriptionMessage(unsubscriber));
    }

    private Database initSubscriberDatabase() throws CouchbaseLiteException {
        CouchbaseLite.init(getApplicationContext());
        DatabaseConfiguration config = new DatabaseConfiguration();
        config.setDirectory(getFilesDir().getPath());
        return new Database(DB_NAME, config);
    }

    @Override
    public void onDestroy() {
        mServiceRunning = false;
        if (mPushNotificationClient != null) {
            try {
                mPushNotificationClient.stopConnection();
            } catch (IOException ignore) {}
        }
        mServiceHandler.getLooper().quit();
        Log.i(TAG, "Notification service destroyed");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}