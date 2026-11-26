package com.callrecorder.app.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.callrecorder.app.CallRecorderApp;
import com.callrecorder.app.services.CallRecorderService;
import com.callrecorder.app.utils.SettingsManager;

public class CallReceiver extends BroadcastReceiver {
    private static final String TAG = "CallReceiver";
    private static String lastNumber;

    @Override
    public void onReceive(Context context, Intent intent) {
        SettingsManager settings = CallRecorderApp.getSettingsManager();
        if (!settings.isAutoRecordEnabled()) return;

        String action = intent.getAction();
        if (action == null) return;

        if (Intent.ACTION_NEW_OUTGOING_CALL.equals(action)) {
            lastNumber = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER);
            Log.d(TAG, "Outgoing call to: " + lastNumber);
            startService(context, lastNumber, 2);

        } else if (TelephonyManager.ACTION_PHONE_STATE_CHANGED.equals(action)) {
            String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
            String incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);

            if (TelephonyManager.EXTRA_STATE_RINGING.equals(state)) {
                lastNumber = incomingNumber;
            } else if (TelephonyManager.EXTRA_STATE_OFFHOOK.equals(state)) {
                String number = lastNumber != null ? lastNumber : incomingNumber;
                startService(context, number, lastNumber != null ? 1 : 2);
            } else if (TelephonyManager.EXTRA_STATE_IDLE.equals(state)) {
                lastNumber = null;
            }
        }
    }

    private void startService(Context context, String phoneNumber, int callType) {
        Intent serviceIntent = new Intent(context, CallRecorderService.class);
        if (phoneNumber != null) serviceIntent.putExtra("phone_number", phoneNumber);
        serviceIntent.putExtra("call_type", callType);

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent);
            } else {
                context.startService(serviceIntent);
            }
        } catch (Exception e) {
            // Android 12+ may refuse to start a foreground service from the background.
            // The service started from MainActivity keeps listening for call state anyway.
            Log.w(TAG, "Could not (re)start service from receiver", e);
        }
    }
}
