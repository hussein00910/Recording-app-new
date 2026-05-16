package com.callrecorder.app.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.callrecorder.app.R;
import com.callrecorder.app.fragments.ContactsFragment;
import com.callrecorder.app.fragments.RecordingsFragment;
import com.callrecorder.app.fragments.SettingsFragment;
import com.callrecorder.app.services.CallRecorderService;
import com.callrecorder.app.viewmodels.RecordingsViewModel;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final int PERMISSIONS_REQUEST_CODE = 100;

    private static final List<String> REQUIRED_PERMISSIONS = new ArrayList<>();

    static {
        REQUIRED_PERMISSIONS.add(Manifest.permission.RECORD_AUDIO);
        REQUIRED_PERMISSIONS.add(Manifest.permission.READ_PHONE_STATE);
        REQUIRED_PERMISSIONS.add(Manifest.permission.READ_CALL_LOG);
        REQUIRED_PERMISSIONS.add(Manifest.permission.READ_CONTACTS);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            REQUIRED_PERMISSIONS.add(Manifest.permission.READ_MEDIA_AUDIO);
            REQUIRED_PERMISSIONS.add(Manifest.permission.POST_NOTIFICATIONS);
        } else {
            REQUIRED_PERMISSIONS.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            REQUIRED_PERMISSIONS.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        }
    }

    private BottomNavigationView bottomNavigationView;
    private Fragment currentFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        new ViewModelProvider(this).get(RecordingsViewModel.class);

        bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setOnNavigationItemSelectedListener(navListener);

        if (!hasRequiredPermissions()) {
            requestMissingPermissions();
        } else {
            startCallRecorderService();
        }

        if (savedInstanceState == null) {
            bottomNavigationView.setSelectedItemId(R.id.nav_recordings);
        }
    }

    private final BottomNavigationView.OnNavigationItemSelectedListener navListener =
            item -> {
                Fragment selected = null;
                int id = item.getItemId();
                if (id == R.id.nav_recordings) selected = new RecordingsFragment();
                else if (id == R.id.nav_contacts) selected = new ContactsFragment();
                else if (id == R.id.nav_settings) selected = new SettingsFragment();

                if (selected != null) {
                    currentFragment = selected;
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.fragment_container, selected)
                            .commit();
                    return true;
                }
                return false;
            };

    private boolean hasRequiredPermissions() {
        for (String p : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED)
                return false;
        }
        return true;
    }

    private void requestMissingPermissions() {
        List<String> missing = new ArrayList<>();
        for (String p : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED)
                missing.add(p);
        }
        if (!missing.isEmpty()) {
            ActivityCompat.requestPermissions(this,
                    missing.toArray(new String[0]),
                    PERMISSIONS_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            boolean coreGranted = true;
            for (int i = 0; i < permissions.length; i++) {
                if ((permissions[i].equals(Manifest.permission.RECORD_AUDIO)
                        || permissions[i].equals(Manifest.permission.READ_PHONE_STATE))
                        && grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    coreGranted = false;
                }
            }
            if (coreGranted) {
                startCallRecorderService();
            } else {
                showPermissionsDialog();
            }
        }
    }

    private void showPermissionsDialog() {
        new AlertDialog.Builder(this)
                .setTitle("الأذونات مطلوبة")
                .setMessage("يحتاج التطبيق إلى إذن تسجيل الصوت وحالة الهاتف للعمل بشكل صحيح.")
                .setPositiveButton("منح الأذونات", (d, w) -> requestMissingPermissions())
                .setNegativeButton("إلغاء", (d, w) ->
                        Toast.makeText(this, "لن يعمل التطبيق بدون الأذونات المطلوبة", Toast.LENGTH_LONG).show())
                .setCancelable(false)
                .show();
    }

    private void startCallRecorderService() {
        Intent intent = new Intent(this, CallRecorderService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
    }
}
