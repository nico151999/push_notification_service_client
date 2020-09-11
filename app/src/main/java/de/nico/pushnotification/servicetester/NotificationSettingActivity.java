package de.nico.pushnotification.servicetester;

import android.annotation.SuppressLint;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import de.nico.pushnotification.servicetester.receiver.AdminStateReceiver;
import de.nico.pushnotification.servicetester.receiver.StartNotificationServiceReceiver;

import static android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS;

// Android apps are in a state that does not receive ON_BOOT_COMPLETE right after installation.
// Launching an activity once, enabling them as device admin or installing them as system app allows
// them to receive this intent. If background optimization is disabled, they can start
// services in the background as well. This activity exists to allow normal users to easily receive
// the ON_BOOT_COMPLETE intent. In addition it allows to configure the notification server specs.
// If you want to ship this app with your ROM, I'd encourage you to remove this activity and
// set IP and PORT as static values.
public class NotificationSettingActivity extends AppCompatActivity {

    private static final int ENABLE_ADMIN_REQUEST = 425;
    private LinearLayout mRootLayout;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.notification_setting);
        mRootLayout = findViewById(R.id.root);
    }

    @SuppressLint("BatteryLife")
    @Override
    protected void onResume() {
        super.onResume();
        if (isBatteryOptimizationEnabled()) {
            startActivity(
                    new Intent()
                            .setAction(
                                    ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                            )
                            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            .setData(Uri.parse("package:" + getPackageName()))
            );
        }
        if (AdminStateReceiver.hasServerSpecs(this)) {
            showInfoWidgets();
        } else {
            showEditWidgets();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mRootLayout.removeAllViews();
    }

    private boolean isBatteryOptimizationEnabled() {
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        return !pm.isIgnoringBatteryOptimizations(getPackageName());
    }

    private void showInfoWidgets() {
        final SharedPreferences preferences = getSharedPreferences(
                StartNotificationServiceReceiver.SERVER_PREFERENCES,
                Context.MODE_PRIVATE
        );
        TextView ipView = new TextView(this);
        ipView.setText(
                getString(
                        R.string.ip,
                        preferences.getString(
                                StartNotificationServiceReceiver.IP_KEY,
                                "-"
                        )
                )
        );
        TextView portView = new TextView(this);
        portView.setText(
                getString(
                        R.string.port,
                        preferences.getInt(
                                StartNotificationServiceReceiver.PORT_KEY,
                                -1
                        )
                )
        );
        mRootLayout.addView(ipView);
        mRootLayout.addView(portView);
        if (!AdminStateReceiver.isAdmin(this)) {
            TextView infoView = new TextView(this);
            infoView.setText(getString(R.string.admin_not_enabled));
            Button openSettingsView = new Button(this);
            openSettingsView.setText(R.string.open_admin_settings);
            openSettingsView.setOnClickListener((view) -> openAdminRequest());
            mRootLayout.addView(infoView);
            mRootLayout.addView(openSettingsView);
        } else {
            AdminStateReceiver.startNotificationServiceBroadcast(this);
            AdminStateReceiver.startNetworkStateService(this);
        }
    }

    @SuppressLint("ApplySharedPref")
    private void showEditWidgets() {
        final SharedPreferences preferences = getSharedPreferences(
                StartNotificationServiceReceiver.SERVER_PREFERENCES,
                Context.MODE_PRIVATE
        );
        final EditText ipView = new EditText(this);
        ipView.setHint(R.string.ip_hint);
        final EditText portView = new EditText(this);
        portView.setHint(R.string.port_hint);
        Button saveSpecView = new Button(this);
        saveSpecView.setText(R.string.save_spec);
        saveSpecView.setOnClickListener((view) -> {
            String ip = ipView.getText().toString();
            int port;
            try {
                port = Integer.parseInt(portView.getText().toString());
            } catch (NumberFormatException e) {
                Toast.makeText(
                        this,
                        R.string.port_not_numeric,
                        Toast.LENGTH_LONG
                ).show();
                return;
            }
            if (!isIpOrHostname(ip) || port < 0) {
                Toast.makeText(
                        this,
                        R.string.improper_ip_or_port,
                        Toast.LENGTH_LONG
                ).show();
                return;
            }
            preferences.edit().putString(
                    StartNotificationServiceReceiver.IP_KEY,
                    ip
            ).putInt(
                    StartNotificationServiceReceiver.PORT_KEY,
                    port
            ).commit();
            mRootLayout.removeAllViews();
            openAdminRequest();
        });
        mRootLayout.addView(ipView);
        mRootLayout.addView(portView);
        mRootLayout.addView(saveSpecView);
    }

    private boolean isIpOrHostname(String ip) {
        return ip.matches("^((([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\\.){3}" +
                        "([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5]))|((([a-zA-Z0-9]|" +
                        "[a-zA-Z0-9][a-zA-Z0-9\\-]*[a-zA-Z0-9])\\.)*([A-Za-z0-9]|" +
                        "[A-Za-z0-9][A-Za-z0-9\\-]*[A-Za-z0-9]))$");
    }

    private void openAdminRequest() {
        Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
        intent.putExtra(
                DevicePolicyManager.EXTRA_DEVICE_ADMIN,
                new ComponentName(
                        NotificationSettingActivity.this,
                        AdminStateReceiver.class
                )
        );
        intent.putExtra(
                DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                getString(R.string.admin_explanation)
        );
        startActivityForResult(intent, ENABLE_ADMIN_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ENABLE_ADMIN_REQUEST) {
            if (resultCode != -1) {
                Toast.makeText(this, R.string.admin_not_enabled, Toast.LENGTH_LONG).show();
            }
        }
    }
}
