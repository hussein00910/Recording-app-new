package com.callrecorder.app.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;

import java.io.File;

public class SettingsManager {
    private static final String PREFS_NAME = "call_recorder_settings";

    private static final String KEY_AUTO_RECORD = "auto_record";
    private static final String KEY_RECORDING_QUALITY = "recording_quality";
    private static final String KEY_STORAGE_PATH = "storage_path";
    private static final String KEY_PASSWORD_PROTECTION = "password_protection";
    private static final String KEY_PASSWORD = "password";
    private static final String KEY_NOTIFICATION_SOUND = "notification_sound";
    private static final String KEY_VIBRATION = "vibration";
    private static final String KEY_SHOW_NOTIFICATION = "show_notification";
    private static final String KEY_DARK_MODE = "dark_mode";
    private static final String KEY_SKIP_SHORT_CALLS = "skip_short_calls";
    private static final String KEY_MIN_CALL_DURATION = "min_call_duration";
    private static final String KEY_AUDIO_FORMAT = "audio_format";
    private static final String KEY_BIOMETRIC_LOCK = "biometric_lock";
    private static final String KEY_AUTO_DELETE_DAYS = "auto_delete_days";
    private static final String KEY_RECORD_ON_SPEAKER = "record_on_speaker";
    private static final String KEY_SHOW_WAVEFORM = "show_waveform";
    private static final String KEY_PLAYBACK_SPEED = "playback_speed";

    public static final int QUALITY_LOW = 0;
    public static final int QUALITY_MEDIUM = 1;
    public static final int QUALITY_HIGH = 2;

    public static final int FORMAT_MP4 = 0;
    public static final int FORMAT_AAC = 1;
    public static final int FORMAT_AMR = 2;
    public static final int FORMAT_WAV = 3;

    public static final int THEME_SYSTEM = 0;
    public static final int THEME_LIGHT = 1;
    public static final int THEME_DARK = 2;

    private final SharedPreferences prefs;
    private final Context appContext;

    public SettingsManager(Context context) {
        appContext = context.getApplicationContext();
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public boolean isAutoRecordEnabled() {
        return prefs.getBoolean(KEY_AUTO_RECORD, true);
    }

    public void setAutoRecordEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_AUTO_RECORD, enabled).apply();
    }

    public int getRecordingQuality() {
        return prefs.getInt(KEY_RECORDING_QUALITY, QUALITY_HIGH);
    }

    public void setRecordingQuality(int quality) {
        prefs.edit().putInt(KEY_RECORDING_QUALITY, quality).apply();
    }

    public int getAudioFormat() {
        return prefs.getInt(KEY_AUDIO_FORMAT, FORMAT_MP4);
    }

    public void setAudioFormat(int format) {
        prefs.edit().putInt(KEY_AUDIO_FORMAT, format).apply();
    }

    public String getStoragePath() {
        return prefs.getString(KEY_STORAGE_PATH, getDefaultStoragePath());
    }

    public void setStoragePath(String path) {
        prefs.edit().putString(KEY_STORAGE_PATH, path).apply();
    }

    public boolean isPasswordProtectionEnabled() {
        return prefs.getBoolean(KEY_PASSWORD_PROTECTION, false);
    }

    public void setPasswordProtectionEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_PASSWORD_PROTECTION, enabled).apply();
    }

    public String getPassword() {
        return prefs.getString(KEY_PASSWORD, null);
    }

    public void setPassword(String password) {
        prefs.edit().putString(KEY_PASSWORD, password).apply();
    }

    public boolean isBiometricLockEnabled() {
        return prefs.getBoolean(KEY_BIOMETRIC_LOCK, false);
    }

    public void setBiometricLockEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_BIOMETRIC_LOCK, enabled).apply();
    }

    public boolean isNotificationSoundEnabled() {
        return prefs.getBoolean(KEY_NOTIFICATION_SOUND, true);
    }

    public void setNotificationSoundEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_NOTIFICATION_SOUND, enabled).apply();
    }

    public boolean isVibrationEnabled() {
        return prefs.getBoolean(KEY_VIBRATION, true);
    }

    public void setVibrationEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_VIBRATION, enabled).apply();
    }

    public boolean isShowNotificationEnabled() {
        return prefs.getBoolean(KEY_SHOW_NOTIFICATION, true);
    }

    public void setShowNotificationEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_SHOW_NOTIFICATION, enabled).apply();
    }

    public int getDarkMode() {
        return prefs.getInt(KEY_DARK_MODE, THEME_SYSTEM);
    }

    public void setDarkMode(int mode) {
        prefs.edit().putInt(KEY_DARK_MODE, mode).apply();
    }

    public boolean isSkipShortCallsEnabled() {
        return prefs.getBoolean(KEY_SKIP_SHORT_CALLS, true);
    }

    public void setSkipShortCallsEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_SKIP_SHORT_CALLS, enabled).apply();
    }

    public int getMinCallDuration() {
        return prefs.getInt(KEY_MIN_CALL_DURATION, 5);
    }

    public void setMinCallDuration(int seconds) {
        prefs.edit().putInt(KEY_MIN_CALL_DURATION, seconds).apply();
    }

    public int getAutoDeleteDays() {
        return prefs.getInt(KEY_AUTO_DELETE_DAYS, 0);
    }

    public void setAutoDeleteDays(int days) {
        prefs.edit().putInt(KEY_AUTO_DELETE_DAYS, days).apply();
    }

    public boolean isRecordOnSpeakerEnabled() {
        return prefs.getBoolean(KEY_RECORD_ON_SPEAKER, false);
    }

    public void setRecordOnSpeakerEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_RECORD_ON_SPEAKER, enabled).apply();
    }

    public boolean isShowWaveformEnabled() {
        return prefs.getBoolean(KEY_SHOW_WAVEFORM, true);
    }

    public void setShowWaveformEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_SHOW_WAVEFORM, enabled).apply();
    }

    public float getPlaybackSpeed() {
        return prefs.getFloat(KEY_PLAYBACK_SPEED, 1.0f);
    }

    public void setPlaybackSpeed(float speed) {
        prefs.edit().putFloat(KEY_PLAYBACK_SPEED, speed).apply();
    }

    /**
     * App-specific directory: always writable on every Android version and needs no
     * storage permission (public folders such as Music/ are blocked by scoped storage).
     */
    public String getDefaultStoragePath() {
        File base = appContext.getExternalFilesDir(Environment.DIRECTORY_MUSIC);
        if (base == null) base = new File(appContext.getFilesDir(), "Music");
        return new File(base, "CallRecordings").getAbsolutePath();
    }

    public void resetToDefaults() {
        prefs.edit()
                .putBoolean(KEY_AUTO_RECORD, true)
                .putInt(KEY_RECORDING_QUALITY, QUALITY_HIGH)
                .putInt(KEY_AUDIO_FORMAT, FORMAT_MP4)
                .putString(KEY_STORAGE_PATH, getDefaultStoragePath())
                .putBoolean(KEY_PASSWORD_PROTECTION, false)
                .remove(KEY_PASSWORD)
                .putBoolean(KEY_BIOMETRIC_LOCK, false)
                .putBoolean(KEY_NOTIFICATION_SOUND, true)
                .putBoolean(KEY_VIBRATION, true)
                .putBoolean(KEY_SHOW_NOTIFICATION, true)
                .putInt(KEY_DARK_MODE, THEME_SYSTEM)
                .putBoolean(KEY_SKIP_SHORT_CALLS, true)
                .putInt(KEY_MIN_CALL_DURATION, 5)
                .putInt(KEY_AUTO_DELETE_DAYS, 0)
                .putBoolean(KEY_RECORD_ON_SPEAKER, false)
                .putBoolean(KEY_SHOW_WAVEFORM, true)
                .putFloat(KEY_PLAYBACK_SPEED, 1.0f)
                .apply();
    }
}
