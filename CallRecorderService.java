package com.callrecorder.app.services;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.callrecorder.app.CallRecorderApp;
import com.callrecorder.app.R;
import com.callrecorder.app.activities.MainActivity;
import com.callrecorder.app.database.AppDatabase;
import com.callrecorder.app.models.Recording;
import com.callrecorder.app.utils.ContactUtils;
import com.callrecorder.app.utils.SettingsManager;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.Executors;

public class CallRecorderService extends Service {
    private static final String TAG = "CallRecorderService";
    private static final int NOTIFICATION_ID_SERVICE = 1001;
    private static final int NOTIFICATION_ID_RECORDING = 1002;
    private static final int NOTIFICATION_ID_COMPLETE = 1003;

    public static final String ACTION_STOP_RECORDING = "com.callrecorder.app.STOP_RECORDING";

    private MediaRecorder mediaRecorder;
    private boolean isRecording = false;
    private String outputFile;
    private long startTime;
    private String phoneNumber;
    private int callType;
    private PhoneStateListener phoneStateListener;
    private TelephonyManager telephonyManager;
    private SettingsManager settingsManager;
    private AppDatabase database;
    private PowerManager.WakeLock wakeLock;
    private AudioManager audioManager;

    @Override
    public void onCreate() {
        super.onCreate();
        settingsManager = CallRecorderApp.getSettingsManager();
        database = CallRecorderApp.getDatabase();
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        acquireWakeLock();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP_RECORDING.equals(intent.getAction())) {
            if (isRecording) stopRecording();
            return START_STICKY;
        }

        if (intent != null) {
            phoneNumber = intent.getStringExtra("phone_number");
            callType = intent.getIntExtra("call_type", 0);
        }

        startForeground(NOTIFICATION_ID_SERVICE, buildServiceNotification("تطبيق تسجيل المكالمات يعمل"));
        setupPhoneStateListener();
        return START_STICKY;
    }

    @SuppressLint("MissingPermission")
    private void setupPhoneStateListener() {
        telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        phoneStateListener = new PhoneStateListener() {
            @Override
            public void onCallStateChanged(int state, String incomingNumber) {
                switch (state) {
                    case TelephonyManager.CALL_STATE_IDLE:
                        if (isRecording) stopRecording();
                        phoneNumber = null;
                        callType = 0;
                        break;

                    case TelephonyManager.CALL_STATE_OFFHOOK:
                        if (!isRecording && settingsManager.isAutoRecordEnabled()) {
                            if (phoneNumber == null) phoneNumber = incomingNumber;
                            if (callType == 0) {
                                callType = (incomingNumber != null && !incomingNumber.isEmpty()) ? 1 : 2;
                            }
                            startRecording();
                        }
                        break;

                    case TelephonyManager.CALL_STATE_RINGING:
                        if (phoneNumber == null) phoneNumber = incomingNumber;
                        callType = 1;
                        break;
                }
            }
        };

        if (telephonyManager != null) {
            telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
        }
    }

    private void startRecording() {
        if (isRecording) return;

        try {
            File directory = new File(settingsManager.getStoragePath());
            if (!directory.exists()) directory.mkdirs();

            outputFile = buildOutputFilePath();
            mediaRecorder = new MediaRecorder();

            boolean usedVoiceComm = false;
            try {
                mediaRecorder.setAudioSource(MediaRecorder.AudioSource.VOICE_COMMUNICATION);
                usedVoiceComm = true;
            } catch (Exception e) {
                mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                if (settingsManager.isRecordOnSpeakerEnabled()) {
                    audioManager.setSpeakerphoneOn(true);
                }
            }

            applyQualitySettings();
            mediaRecorder.setOutputFile(outputFile);
            mediaRecorder.prepare();
            mediaRecorder.start();

            startTime = System.currentTimeMillis();
            isRecording = true;

            String contactDisplay = (phoneNumber != null) ?
                    ContactUtils.getContactName(this, phoneNumber) : null;
            if (contactDisplay == null || contactDisplay.isEmpty()) contactDisplay = phoneNumber;

            showRecordingNotification(contactDisplay != null ? contactDisplay : "مكالمة");

        } catch (IOException e) {
            Log.e(TAG, "Primary recording failed, trying fallback", e);
            startFallbackRecording();
        } catch (Exception e) {
            Log.e(TAG, "Error starting recording", e);
        }
    }

    private void startFallbackRecording() {
        try {
            if (outputFile == null) outputFile = buildOutputFilePath();

            if (mediaRecorder != null) {
                try { mediaRecorder.release(); } catch (Exception ignored) {}
            }

            mediaRecorder = new MediaRecorder();
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            audioManager.setSpeakerphoneOn(true);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            mediaRecorder.setAudioSamplingRate(22050);
            mediaRecorder.setAudioEncodingBitRate(96000);
            mediaRecorder.setOutputFile(outputFile);
            mediaRecorder.prepare();
            mediaRecorder.start();

            startTime = System.currentTimeMillis();
            isRecording = true;

            showRecordingNotification("مكالمة");
            Log.d(TAG, "Fallback recording started");

        } catch (IOException e) {
            Log.e(TAG, "Fallback recording also failed", e);
            isRecording = false;
        }
    }

    private void applyQualitySettings() {
        int quality = settingsManager.getRecordingQuality();
        switch (quality) {
            case SettingsManager.QUALITY_HIGH:
                mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
                mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
                mediaRecorder.setAudioSamplingRate(44100);
                mediaRecorder.setAudioEncodingBitRate(256000);
                mediaRecorder.setAudioChannels(2);
                break;
            case SettingsManager.QUALITY_LOW:
                mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
                mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
                mediaRecorder.setAudioSamplingRate(8000);
                mediaRecorder.setAudioEncodingBitRate(12200);
                break;
            case SettingsManager.QUALITY_MEDIUM:
            default:
                mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
                mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
                mediaRecorder.setAudioSamplingRate(22050);
                mediaRecorder.setAudioEncodingBitRate(128000);
                break;
        }
    }

    private void stopRecording() {
        if (!isRecording) return;

        try {
            mediaRecorder.stop();
            mediaRecorder.reset();
            mediaRecorder.release();
            mediaRecorder = null;

            long duration = System.currentTimeMillis() - startTime;
            long durationSeconds = duration / 1000;

            audioManager.setSpeakerphoneOn(false);

            int minDuration = settingsManager.getMinCallDuration();
            if (settingsManager.isSkipShortCallsEnabled() && durationSeconds < minDuration) {
                File f = new File(outputFile);
                if (f.exists()) f.delete();
                Log.d(TAG, "Call too short (" + durationSeconds + "s), discarding recording");
            } else {
                saveRecordingToDatabase(duration);
                showCompletedNotification(duration);
            }

            isRecording = false;
            cancelRecordingNotification();
            restoreServiceNotification();

        } catch (Exception e) {
            Log.e(TAG, "Error stopping recording", e);
            isRecording = false;
        }
    }

    private String buildOutputFilePath() {
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String name;

        if (phoneNumber != null && !phoneNumber.isEmpty()) {
            String contact = ContactUtils.getContactName(this, phoneNumber);
            name = (contact != null && !contact.isEmpty()) ? contact : phoneNumber;
            name = name.replaceAll("[^a-zA-Z0-9؀-ۿ_\\-]", "_");
        } else {
            name = "Unknown";
        }

        name += "_" + timestamp;
        name += (callType == 1) ? "_incoming" : "_outgoing";

        String ext;
        switch (settingsManager.getRecordingQuality()) {
            case SettingsManager.QUALITY_LOW: ext = ".3gp"; break;
            default: ext = ".mp4"; break;
        }

        return settingsManager.getStoragePath() + File.separator + name + ext;
    }

    private void saveRecordingToDatabase(final long duration) {
        String contactName = null;
        if (phoneNumber != null && !phoneNumber.isEmpty()) {
            contactName = ContactUtils.getContactName(this, phoneNumber);
        }

        File file = new File(outputFile);
        long fileSize = file.exists() ? file.length() : 0;

        final Recording recording = new Recording(
                0, phoneNumber, contactName, callType,
                outputFile, duration, System.currentTimeMillis(), false, null);
        recording.setFileSize(fileSize);

        Executors.newSingleThreadExecutor().execute(() ->
                database.recordingDao().insert(recording));
    }

    // ── Notifications ──────────────────────────────────────────────────────────

    private Notification buildServiceNotification(String text) {
        PendingIntent pi = PendingIntent.getActivity(this, 0,
                new Intent(this, MainActivity.class),
                PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, CallRecorderApp.CHANNEL_ID_SERVICE)
                .setContentTitle("تسجيل المكالمات")
                .setContentText(text)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentIntent(pi)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    private void showRecordingNotification(String callerName) {
        Intent stopIntent = new Intent(this, CallRecorderService.class);
        stopIntent.setAction(ACTION_STOP_RECORDING);
        PendingIntent stopPi = PendingIntent.getService(this, 0, stopIntent,
                PendingIntent.FLAG_IMMUTABLE);

        PendingIntent openPi = PendingIntent.getActivity(this, 0,
                new Intent(this, MainActivity.class),
                PendingIntent.FLAG_IMMUTABLE);

        Notification n = new NotificationCompat.Builder(this, CallRecorderApp.CHANNEL_ID_RECORDING)
                .setContentTitle("جاري تسجيل المكالمة")
                .setContentText(callerName)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentIntent(openPi)
                .addAction(R.drawable.ic_stop, "إيقاف التسجيل", stopPi)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build();

        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(NOTIFICATION_ID_RECORDING, n);
    }

    private void cancelRecordingNotification() {
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        nm.cancel(NOTIFICATION_ID_RECORDING);
    }

    private void restoreServiceNotification() {
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(NOTIFICATION_ID_SERVICE, buildServiceNotification("جاهز للتسجيل"));
    }

    private void showCompletedNotification(long duration) {
        long secs = duration / 1000;
        String durationStr = String.format(Locale.getDefault(), "%d:%02d", secs / 60, secs % 60);

        PendingIntent openPi = PendingIntent.getActivity(this, 0,
                new Intent(this, MainActivity.class),
                PendingIntent.FLAG_IMMUTABLE);

        Notification n = new NotificationCompat.Builder(this, CallRecorderApp.CHANNEL_ID_COMPLETE)
                .setContentTitle("تم حفظ التسجيل")
                .setContentText("مدة المكالمة: " + durationStr)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentIntent(openPi)
                .setAutoCancel(true)
                .build();

        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(NOTIFICATION_ID_COMPLETE, n);
    }

    // ── WakeLock ───────────────────────────────────────────────────────────────

    @SuppressLint("WakelockTimeout")
    private void acquireWakeLock() {
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "CallRecorder:WakeLock");
        wakeLock.acquire();
    }

    private void releaseWakeLock() {
        if (wakeLock != null && wakeLock.isHeld()) wakeLock.release();
    }

    // ── Lifecycle ──────────────────────────────────────────────────────────────

    @Override
    public void onDestroy() {
        if (isRecording) stopRecording();
        if (telephonyManager != null && phoneStateListener != null) {
            telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
        }
        releaseWakeLock();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
