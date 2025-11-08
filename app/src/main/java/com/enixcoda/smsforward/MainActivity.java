package com.enixcoda.smsforward;

import android.Manifest;
import android.app.ActivityManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Telephony;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_PERMISSIONS_CODE = 1001;

    private TextView targetNumberView;
    private TextView allowedSendersView;
    private TextView permissionsStatusView;
    private TextView defaultSmsStatusView;
    private TextView serviceStatusView;
    private Button requestPermissionsButton;
    private Button setDefaultSmsButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d("MainActivity", "onCreate: SMS Forwarder started");

        bindViews();
        populateStaticInfo();

        requestRequiredPermissions(false);
        ensureForegroundServiceState();
        checkDefaultSmsApp();
        updateStatusViews();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateStatusViews();
        ensureForegroundServiceState();
    }

    private void bindViews() {
        targetNumberView = findViewById(R.id.text_target_number);
        allowedSendersView = findViewById(R.id.text_allowed_senders);
        permissionsStatusView = findViewById(R.id.text_permissions_status);
        defaultSmsStatusView = findViewById(R.id.text_default_sms_status);
        serviceStatusView = findViewById(R.id.text_service_status);
        requestPermissionsButton = findViewById(R.id.button_request_permissions);
        setDefaultSmsButton = findViewById(R.id.button_set_default_sms);

        requestPermissionsButton.setOnClickListener(v -> requestRequiredPermissions(true));
        setDefaultSmsButton.setOnClickListener(v -> promptSetDefaultSmsApp());
    }

    private void populateStaticInfo() {
        targetNumberView.setText(getString(R.string.status_target_number, ForwardingConfig.getTargetNumber()));
        allowedSendersView.setText(getString(
                R.string.status_allowed_senders,
                TextUtils.join(", ", ForwardingConfig.getDisplayAllowedSenders())
        ));
    }

    private void updateStatusViews() {
        updatePermissionStatus();
        updateDefaultSmsStatus();
        updateServiceStatus();
    }

    private void requestRequiredPermissions(boolean forceRequest) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return;
        }

        List<String> missingPermissions = new ArrayList<>();
        for (String permission : getRuntimePermissions()) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                missingPermissions.add(permission);
            }
        }

        if (!missingPermissions.isEmpty() && (forceRequest || Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)) {
            ActivityCompat.requestPermissions(this, missingPermissions.toArray(new String[0]), REQUEST_PERMISSIONS_CODE);
        }
    }

    private String[] getRuntimePermissions() {
        List<String> permissions = new ArrayList<>();
        permissions.add(Manifest.permission.RECEIVE_SMS);
        permissions.add(Manifest.permission.SEND_SMS);
        permissions.add(Manifest.permission.READ_SMS);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS);
        }
        return permissions.toArray(new String[0]);
    }

    private void ensureForegroundServiceState() {
        Intent serviceIntent = new Intent(this, SMSForwardForegroundService.class);
        ContextCompat.startForegroundService(this, serviceIntent);
    }

    private void updatePermissionStatus() {
        StringBuilder builder = new StringBuilder();
        for (String permission : getRuntimePermissions()) {
            boolean granted = ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED;
            builder.append(getFriendlyPermissionName(permission))
                    .append(": ")
                    .append(granted ? getString(R.string.status_granted) : getString(R.string.status_denied))
                    .append('\n');
        }
        permissionsStatusView.setText(builder.toString().trim());
    }

    private void updateDefaultSmsStatus() {
        String defaultPackage = Telephony.Sms.getDefaultSmsPackage(this);
        boolean isDefault = getPackageName().equals(defaultPackage);
        defaultSmsStatusView.setText(getString(
                R.string.status_default_sms_app,
                isDefault ? getString(R.string.status_enabled) : getString(R.string.status_disabled)
        ));
        setDefaultSmsButton.setVisibility(isDefault ? View.GONE : View.VISIBLE);
    }

    private void updateServiceStatus() {
        boolean running = isServiceRunning();
        serviceStatusView.setText(getString(
                R.string.status_service_running,
                running ? getString(R.string.status_running) : getString(R.string.status_not_running)
        ));
    }

    private boolean isServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        if (manager == null) {
            return false;
        }

        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (SMSForwardForegroundService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    private void checkDefaultSmsApp() {
        if (!getPackageName().equals(Telephony.Sms.getDefaultSmsPackage(this))) {
            promptSetDefaultSmsApp();
        }
    }

    private void promptSetDefaultSmsApp() {
        Intent intent = new Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT);
        intent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, getPackageName());
        startActivity(intent);
    }

    private String getFriendlyPermissionName(String permission) {
        if (Manifest.permission.RECEIVE_SMS.equals(permission)) {
            return getString(R.string.permission_receive_sms);
        }
        if (Manifest.permission.SEND_SMS.equals(permission)) {
            return getString(R.string.permission_send_sms);
        }
        if (Manifest.permission.READ_SMS.equals(permission)) {
            return getString(R.string.permission_read_sms);
        }
        if (Manifest.permission.POST_NOTIFICATIONS.equals(permission)) {
            return getString(R.string.permission_post_notifications);
        }
        return permission;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSIONS_CODE) {
            updatePermissionStatus();
            ensureForegroundServiceState();
        }
    }
}
