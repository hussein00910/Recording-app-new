package com.callrecorder.app;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;

import androidx.appcompat.app.AppCompatDelegate;

import com.callrecorder.app.database.AppDatabase;
import com.callrecorder.app.utils.SettingsManager;

public class CallRecorderApp extends Application {

    public static final String CHANNEL_ID_SERVICE = "call_recorder_service_channel";
    public static final String CHANNEL_ID_RECORDING = "call_recorder_recording_channel";
    public static final String CHANNEL_ID_COMPLETE = "call_recorder_complete_channel";

    private static AppDatabase database;
    private static SettingsManager settingsManager;

    @Override
    public void onCreate() {
        super.onCreate();
        database = AppDatabase.getInstance(this);
        settingsManager = new SettingsManager(this);
        createNotificationChannels();
        applyTheme();
    }

    private void applyTheme() {
        int mode = settingsManager.getDarkMode();
        switch (mode) {
            case SettingsManager.THEME_LIGHT:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case SettingsManager.THEME_DARK:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            default:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
        }
    }

    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = getSystemService(NotificationManager.class);

            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID_SERVICE,
                    "خدمة تسجيل المكالمات",
                    NotificationManager.IMPORTANCE_LOW);
            serviceChannel.setDescription("خدمة المراقبة في الخلفية");
            serviceChannel.setShowBadge(false);

            NotificationChannel recordingChannel = new NotificationChannel(
                    CHANNEL_ID_RECORDING,
                    "جاري التسجيل",
                    NotificationManager.IMPORTANCE_HIGH);
            recordingChannel.setDescription("إشعار عند تسجيل مكالمة نشطة");

            NotificationChannel completeChannel = new NotificationChannel(
                    CHANNEL_ID_COMPLETE,
                    "تم الحفظ",
                    NotificationManager.IMPORTANCE_DEFAULT);
            completeChannel.setDescription("إشعار عند اكتمال حفظ التسجيل");

            manager.createNotificationChannel(serviceChannel);
            manager.createNotificationChannel(recordingChannel);
            manager.createNotificationChannel(completeChannel);
        }
    }

    public static AppDatabase getDatabase() {
        return database;
    }

    public static SettingsManager getSettingsManager() {
        return settingsManager;
    }
}
