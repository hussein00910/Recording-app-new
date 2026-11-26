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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.Executors;

public class CallRecorderService extends Service {
    private static final String TAG = "CallRecorderService";
    private static final int NOTIFICATION_ID_SERVICE = 1001;
    private static final int NOTIFICATION_ID_RECORDING = 1002;
    private static final int NOTIFICATION_ID_COMPLETE = 1003;
    private static final int NOTIFICATION_ID_ERROR = 1004;

    public static final String ACTION_STOP_RECORDING = "com.callrecorder.app.STOP_RECORDING";

    private MediaRecorder mediaRecorder;
    private boolean isRecording = false;
    private boolean inForeground = false;
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

        if (!inForeground) {
            try {
                startForeground(NOTIFICATION_ID_SERVICE, buildServiceNotification("جاهز للتسجيل"));
                inForeground = true;
            } catch (Exception e) {
                Log.e(TAG, "startForeground failed", e);
            }
        }
        if (phoneStateListener == null) setupPhoneStateListener();
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

    /** Audio sources tried in order; devices differ in which one is allowed during a call. */
    private static final int[] AUDIO_SOURCES = {
            MediaRecorder.AudioSource.VOICE_COMMUNICATION,
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            MediaRecorder.AudioSource.MIC,
    };

    private void startRecording() {
        if (isRecording) return;

        File directory = resolveStorageDirectory();
        if (directory == null) {
            reportError("تعذّر إنشاء مجلد التسجيلات");
            return;
        }
        outputFile = buildOutputFilePath(directory);

        Exception lastError = null;
        for (int source : AUDIO_SOURCES) {
            try {
                mediaRecorder = new MediaRecorder();
                mediaRecorder.setAudioSource(source);
                applyQualitySettings();
                mediaRecorder.setOutputFile(outputFile);
                mediaRecorder.prepare();
                mediaRecorder.start();

                if (source == MediaRecorder.AudioSource.MIC && settingsManager.isRecordOnSpeakerEnabled()) {
                    audioManager.setSpeakerphoneOn(true);
                }
                startTime = System.currentTimeMillis();
                isRecording = true;
                Log.d(TAG, "Recording started with audio source " + source + " -> " + outputFile);

                String contactDisplay = (phoneNumber != null) ?
                        ContactUtils.getContactName(this, phoneNumber) : null;
                if (contactDisplay == null || contactDisplay.isEmpty()) contactDisplay = phoneNumber;
                showRecordingNotification(contactDisplay != null ? contactDisplay : "مكالمة");
                return;
            } catch (Exception e) {
                // RuntimeException ("start failed") is common here, not only IOException
                Log.w(TAG, "Audio source " + source + " failed", e);
                lastError = e;
                releaseRecorder();
                new File(outputFile).delete();
            }
        }
        reportError("تعذّر بدء التسجيل: " + describe(lastError));
    }

    /** Uses the configured folder if writable, otherwise the app-specific default. */
    private File resolveStorageDirectory() {
        File dir = new File(settingsManager.getStoragePath());
        if ((dir.isDirectory() || dir.mkdirs()) && dir.canWrite()) return dir;

        Log.w(TAG, "Storage path not writable: " + dir + ", using app folder");
        File fallback = new File(settingsManager.getDefaultStoragePath());
        settingsManager.setStoragePath(fallback.getAbsolutePath());
        if ((fallback.isDirectory() || fallback.mkdirs()) && fallback.canWrite()) return fallback;
        return null;
    }

    private void applyQualitySettings() {
        int quality = settingsManager.getRecordingQuality();
        switch (quality) {
            case SettingsManager.QUALITY_HIGH:
                mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
                mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
                mediaRecorder.setAudioSamplingRate(44100);
                mediaRecorder.setAudioEncodingBitRate(128000);
                // Call audio is mono; requesting 2 channels makes start() fail on some devices
                mediaRecorder.setAudioChannels(1);
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
                mediaRecorder.setAudioEncodingBitRate(64000);
                mediaRecorder.setAudioChannels(1);
                break;
        }
    }

    private void stopRecording() {
        if (!isRecording) return;
        isRecording = false;

        long duration = System.currentTimeMillis() - startTime;
        boolean stopped = false;
        try {
            mediaRecorder.stop();
            stopped = true;
        } catch (Exception e) {
            // stop() throws if no audio data was captured
            Log.e(TAG, "Error stopping recording", e);
        }
        releaseRecorder();
        audioManager.setSpeakerphoneOn(false);
        cancelRecordingNotification();
        restoreServiceNotification();

        File f = new File(outputFile);
        if (!stopped || !f.exists() || f.length() == 0) {
            f.delete();
            reportError("فشل حفظ التسجيل: لم يُلتقط أي صوت");
            return;
        }

        long durationSeconds = duration / 1000;
        if (settingsManager.isSkipShortCallsEnabled() && durationSeconds < settingsManager.getMinCallDuration()) {
            f.delete();
            Log.d(TAG, "Call too short (" + durationSeconds + "s), discarding recording");
            return;
        }
        saveRecordingToDatabase(duration);
        showCompletedNotification(duration);
    }

    private void releaseRecorder() {
        if (mediaRecorder != null) {
            try { mediaRecorder.reset(); } catch (Exception ignored) {}
            try { mediaRecorder.release(); } catch (Exception ignored) {}
            mediaRecorder = null;
        }
    }

    private static String describe(Exception e) {
        if (e == null) return "سبب غير معروف";
        String msg = e.getMessage();
        return e.getClass().getSimpleName() + (msg != null ? " (" + msg + ")" : "");
    }

    /** Shows the failure to the user instead of only writing it to logcat. */
    private void reportError(String message) {
        Log.e(TAG, message);
        Notification n = new NotificationCompat.Builder(this, CallRecorderApp.CHANNEL_ID_COMPLETE)
                .setContentTitle("لم يتم تسجيل المكالمة")
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setSmallIcon(R.drawable.ic_notification)
                .setAutoCancel(true)
                .build();
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(NOTIFICATION_ID_ERROR, n);
    }

    private String buildOutputFilePath(File directory) {
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

        return new File(directory, name + ext).getAbsolutePath();
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
