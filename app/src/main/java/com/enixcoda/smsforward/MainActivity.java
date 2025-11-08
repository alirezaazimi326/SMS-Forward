package com.enixcoda.smsforward;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Telephony;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 1;

    private TextView permissionsStatusView;
    private TextView defaultSmsStatusView;
    private TextView serviceStatusView;
    private Button setDefaultSmsButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d("MainActivity", "onCreate: SMS Forwarder started");

        TextView targetNumberView = findViewById(R.id.text_target_number_value);
        TextView allowedSendersView = findViewById(R.id.text_allowed_senders_value);
        permissionsStatusView = findViewById(R.id.text_permissions_value);
        defaultSmsStatusView = findViewById(R.id.text_default_sms_value);
        serviceStatusView = findViewById(R.id.text_service_value);
        setDefaultSmsButton = findViewById(R.id.button_set_default_sms);

        targetNumberView.setText(ForwardingConfig.TARGET_NUMBER);
        allowedSendersView.setText(TextUtils.join("\n", Arrays.asList(ForwardingConfig.ALLOWED_SENDERS)));

        setDefaultSmsButton.setOnClickListener(v -> openDefaultSmsSettings());

        requestRequiredPermissions();
        updatePermissionStatus();
        updateDefaultSmsStatus();
        ensureForegroundServiceState();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updatePermissionStatus();
        updateDefaultSmsStatus();
        ensureForegroundServiceState();
    }

    private void requestRequiredPermissions() {
        List<String> permissionsToRequest = new ArrayList<>();
        String[] basePermissions = new String[]{
                Manifest.permission.RECEIVE_SMS,
                Manifest.permission.SEND_SMS,
                Manifest.permission.READ_SMS
        };

        for (String permission : basePermissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission);
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS);
        }

        if (!permissionsToRequest.isEmpty()) {
            ActivityCompat.requestPermissions(this,
                    permissionsToRequest.toArray(new String[0]),
                    PERMISSION_REQUEST_CODE);
        }
    }

    private void updatePermissionStatus() {
        StringBuilder statusBuilder = new StringBuilder();
        statusBuilder.append(formatPermissionStatus(Manifest.permission.RECEIVE_SMS, R.string.permission_receive_sms));
        statusBuilder.append('\n');
        statusBuilder.append(formatPermissionStatus(Manifest.permission.SEND_SMS, R.string.permission_send_sms));
        statusBuilder.append('\n');
        statusBuilder.append(formatPermissionStatus(Manifest.permission.READ_SMS, R.string.permission_read_sms));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            statusBuilder.append('\n');
            statusBuilder.append(formatPermissionStatus(Manifest.permission.POST_NOTIFICATIONS, R.string.permission_post_notifications));
        }

        permissionsStatusView.setText(statusBuilder.toString());
    }

    private String formatPermissionStatus(String permission, @StringRes int labelRes) {
        boolean granted = ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED;
        return getString(R.string.permission_status_line,
                getString(labelRes),
                granted ? getString(R.string.status_granted) : getString(R.string.status_missing));
    }

    private void updateDefaultSmsStatus() {
        String defaultSmsPackage = Telephony.Sms.getDefaultSmsPackage(this);
        boolean isDefault = getPackageName().equals(defaultSmsPackage);

        defaultSmsStatusView.setText(isDefault
                ? getString(R.string.status_default_sms_true)
                : getString(R.string.status_default_sms_false));
        setDefaultSmsButton.setEnabled(!isDefault);
    }

    private void ensureForegroundServiceState() {
        if (areCorePermissionsGranted()) {
            Intent serviceIntent = new Intent(this, SMSForwardForegroundService.class);
            ContextCompat.startForegroundService(this, serviceIntent);
            serviceStatusView.setText(getString(R.string.status_service_running));
        } else {
            serviceStatusView.setText(getString(R.string.status_service_missing_permissions));
        }
    }

    private boolean areCorePermissionsGranted() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED;
    }

    private void openDefaultSmsSettings() {
        Intent intent = new Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT);
        intent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, getPackageName());
        startActivity(intent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            updatePermissionStatus();
            ensureForegroundServiceState();
        }
    }
}
