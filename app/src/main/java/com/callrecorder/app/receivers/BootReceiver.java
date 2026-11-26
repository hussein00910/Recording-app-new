package com.callrecorder.app.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.callrecorder.app.CallRecorderApp;
import com.callrecorder.app.services.CallRecorderService;
import com.callrecorder.app.utils.SettingsManager;

public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (Intent.ACTION_BOOT_COMPLETED.equals(action)
                || "android.intent.action.QUICKBOOT_POWERON".equals(action)) {

            SettingsManager settings = CallRecorderApp.getSettingsManager();
            if (settings.isAutoRecordEnabled()) {
                Log.d(TAG, "Device booted, starting CallRecorderService");
                Intent serviceIntent = new Intent(context, CallRecorderService.class);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent);
                } else {
                    context.startService(serviceIntent);
                }
            }
        }
    }
}
