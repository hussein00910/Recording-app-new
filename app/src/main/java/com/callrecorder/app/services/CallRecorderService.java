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
import androidx.core.content.ContextCompat;
import android.Manifest;
import android.content.pm.PackageManager;

import com.callrecorder.app.CallRecorderApp;
import com.callrecorder.app.R;
import com.callrecorder.app.activities.MainActivity;
import com.callrecorder.app.database.AppDatabase;
import com.callrecorder.app.models.Recording;
import com.callrecorder.app.utils.ContactUtils;
import com.callrecorder.app.utils.DiagLog;
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
    private int savedCallVolume = -1;
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
        diag("Service created. Android " + Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + "), "
                + Build.MANUFACTURER + " " + Build.MODEL
                + ", RECORD_AUDIO=" + granted(Manifest.permission.RECORD_AUDIO)
                + ", READ_PHONE_STATE=" + granted(Manifest.permission.READ_PHONE_STATE)
                + ", READ_CALL_LOG=" + granted(Manifest.permission.READ_CALL_LOG)
                + ", autoRecord=" + settingsManager.isAutoRecordEnabled()
                + ", quality=" + settingsManager.getRecordingQuality()
                + ", storage=" + settingsManager.getStoragePath());
    }

    private void diag(String message) {
        DiagLog.log(this, TAG, message);
    }

    private boolean granted(String permission) {
        return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP_RECORDING.equals(intent.getAction())) {
            if (isRecording) stopRecording();
            return START_STICKY;
        }

        diag("onStartCommand intent=" + (intent == null ? "null(restart)" : intent.getAction()
                + " type=" + intent.getIntExtra("call_type", 0)));
        if (intent != null) {
            phoneNumber = intent.getStringExtra("phone_number");
            callType = intent.getIntExtra("call_type", 0);
        }

        if (!inForeground) {
            try {
                startForeground(NOTIFICATION_ID_SERVICE, buildServiceNotification("جاهز للتسجيل"));
                inForeground = true;
            } catch (Exception e) {
                diag("startForeground FAILED: " + describe(e));
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
                diag("Call state -> " + (state == TelephonyManager.CALL_STATE_IDLE ? "IDLE"
                        : state == TelephonyManager.CALL_STATE_OFFHOOK ? "OFFHOOK" : "RINGING")
                        + ", recording=" + isRecording);
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
            try {
                telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
                diag("Call state listener registered");
            } catch (Exception e) {
                diag("Call state listener FAILED: " + describe(e));
            }
        } else {
            diag("No TelephonyManager on this device");
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
        int[][] configs = qualityLadder(settingsManager.getRecordingQuality());
        for (int source : AUDIO_SOURCES) {
            for (int[] config : configs) {
                try {
                    mediaRecorder = new MediaRecorder();
                    mediaRecorder.setAudioSource(source);
                    applyQualitySettings(config);
                    mediaRecorder.setOutputFile(outputFile);
                    mediaRecorder.prepare();
                    mediaRecorder.start();

                    if (source == MediaRecorder.AudioSource.MIC && settingsManager.isRecordOnSpeakerEnabled()) {
                        enableLoudSpeaker();
                    }
                    startTime = System.currentTimeMillis();
                    isRecording = true;
                    diag("Recording STARTED: source=" + source + " rate=" + config[1]
                            + " bitrate=" + config[2] + " -> " + outputFile);

                    String contactDisplay = (phoneNumber != null) ?
                            ContactUtils.getContactName(this, phoneNumber) : null;
                    if (contactDisplay == null || contactDisplay.isEmpty()) contactDisplay = phoneNumber;
                    showRecordingNotification(contactDisplay != null ? contactDisplay : "مكالمة");
                    return;
                } catch (Exception e) {
                    // RuntimeException ("start failed") is common here, not only IOException
                    diag("Start failed: source=" + source + " rate=" + config[1] + ": " + describe(e));
                    lastError = e;
                    releaseRecorder();
                    new File(outputFile).delete();
                }
            }
        }
        reportError("تعذّر بدء التسجيل: " + describe(lastError));
    }

    /** Uses the configured folder if writable, otherwise the app-specific default. */
    private File resolveStorageDirectory() {
        File dir = new File(settingsManager.getStoragePath());
        if ((dir.isDirectory() || dir.mkdirs()) && dir.canWrite()) return dir;

        diag("Storage path not writable: " + dir + ", using app folder");
        File fallback = new File(settingsManager.getDefaultStoragePath());
        settingsManager.setStoragePath(fallback.getAbsolutePath());
        if ((fallback.isDirectory() || fallback.mkdirs()) && fallback.canWrite()) return fallback;
        return null;
    }

    private static final int CODEC_AAC = 0;
    private static final int CODEC_AMR_NB = 1;

    /**
     * Encoder settings per quality level, best first: {codec, sampleRate, bitRate}.
     * If the device rejects one, the next (more widely supported) one is tried.
     * Mono only: call audio is mono, and requesting stereo makes start() fail on some devices.
     */
    private static int[][] qualityLadder(int quality) {
        switch (quality) {
            case SettingsManager.QUALITY_HIGH:
                return new int[][]{
                        {CODEC_AAC, 48000, 192000},
                        {CODEC_AAC, 44100, 128000},
                        {CODEC_AAC, 16000, 64000},
                };
            case SettingsManager.QUALITY_LOW:
                return new int[][]{
                        {CODEC_AMR_NB, 8000, 12200},
                };
            case SettingsManager.QUALITY_MEDIUM:
            default:
                return new int[][]{
                        {CODEC_AAC, 32000, 96000},
                        {CODEC_AAC, 16000, 64000},
                };
        }
    }

    private void applyQualitySettings(int[] config) {
        if (config[0] == CODEC_AMR_NB) {
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        } else {
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            mediaRecorder.setAudioChannels(1);
        }
        mediaRecorder.setAudioSamplingRate(config[1]);
        mediaRecorder.setAudioEncodingBitRate(config[2]);
    }

    /** Speaker on with call volume at max, so the other party is captured clearly by the mic. */
    private void enableLoudSpeaker() {
        try {
            savedCallVolume = audioManager.getStreamVolume(AudioManager.STREAM_VOICE_CALL);
            audioManager.setSpeakerphoneOn(true);
            audioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL,
                    audioManager.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL), 0);
        } catch (Exception e) {
            Log.w(TAG, "Could not enable loud speaker", e);
        }
    }

    private void restoreSpeaker() {
        try {
            audioManager.setSpeakerphoneOn(false);
            if (savedCallVolume >= 0) {
                audioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL, savedCallVolume, 0);
                savedCallVolume = -1;
            }
        } catch (Exception e) {
            Log.w(TAG, "Could not restore speaker", e);
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
            diag("stop() FAILED: " + describe(e));
        }
        releaseRecorder();
        restoreSpeaker();
        cancelRecordingNotification();
        restoreServiceNotification();

        File f = new File(outputFile);
        diag("Recording stopped after " + (duration / 1000) + "s, file size=" + f.length() + " bytes");
        if (!stopped || !f.exists() || f.length() == 0) {
            f.delete();
            reportError("فشل حفظ التسجيل: لم يُلتقط أي صوت");
            return;
        }

        long durationSeconds = duration / 1000;
        if (settingsManager.isSkipShortCallsEnabled() && durationSeconds < settingsManager.getMinCallDuration()) {
            f.delete();
            diag("Call too short (" + durationSeconds + "s < " + settingsManager.getMinCallDuration()
                    + "s), recording discarded");
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
        diag("ERROR: " + message);
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

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                long id = database.recordingDao().insert(recording);
                diag("Saved to database, id=" + id);
            } catch (Exception e) {
                diag("Database insert FAILED: " + describe(e));
            }
        });
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
        diag("Service destroyed");
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
