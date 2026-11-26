package com.callrecorder.app.fragments;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.callrecorder.app.BuildConfig;
import com.callrecorder.app.CallRecorderApp;
import com.callrecorder.app.R;
import com.callrecorder.app.utils.DiagLog;
import com.callrecorder.app.utils.SettingsManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import java.io.File;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.concurrent.Executor;

public class SettingsFragment extends Fragment {

    private SettingsManager settings;

    // Switches
    private SwitchMaterial switchAutoRecord;
    private SwitchMaterial switchSkipShortCalls;
    private SwitchMaterial switchNotification;
    private SwitchMaterial switchVibration;
    private SwitchMaterial switchBiometric;
    private SwitchMaterial switchSpeaker;
    private SwitchMaterial switchWaveform;

    // Value displays
    private TextView tvQualityValue;
    private TextView tvFormatValue;
    private TextView tvMinDurationValue;
    private TextView tvThemeValue;
    private TextView tvStoragePath;
    private TextView tvAutoDeleteValue;
    private TextView tvAppVersion;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        settings = CallRecorderApp.getSettingsManager();

        initViews(view);
        loadSettings();
        setupListeners();
    }

    private void initViews(View view) {
        switchAutoRecord = view.findViewById(R.id.switch_auto_record);
        switchSkipShortCalls = view.findViewById(R.id.switch_skip_short);
        switchNotification = view.findViewById(R.id.switch_notification);
        switchVibration = view.findViewById(R.id.switch_vibration);
        switchBiometric = view.findViewById(R.id.switch_biometric);
        switchSpeaker = view.findViewById(R.id.switch_speaker);
        switchWaveform = view.findViewById(R.id.switch_waveform);

        tvQualityValue = view.findViewById(R.id.tv_quality_value);
        tvFormatValue = view.findViewById(R.id.tv_format_value);
        tvMinDurationValue = view.findViewById(R.id.tv_min_duration_value);
        tvThemeValue = view.findViewById(R.id.tv_theme_value);
        tvStoragePath = view.findViewById(R.id.tv_storage_path);
        tvAutoDeleteValue = view.findViewById(R.id.tv_auto_delete_value);
        tvAppVersion = view.findViewById(R.id.tv_app_version);
    }

    private void loadSettings() {
        switchAutoRecord.setChecked(settings.isAutoRecordEnabled());
        switchSkipShortCalls.setChecked(settings.isSkipShortCallsEnabled());
        switchNotification.setChecked(settings.isShowNotificationEnabled());
        switchVibration.setChecked(settings.isVibrationEnabled());
        switchBiometric.setChecked(settings.isBiometricLockEnabled());
        switchSpeaker.setChecked(settings.isRecordOnSpeakerEnabled());
        switchWaveform.setChecked(settings.isShowWaveformEnabled());

        updateQualityLabel();
        updateFormatLabel();
        updateThemeLabel();
        updateMinDurationLabel();
        updateAutoDeleteLabel();

        tvStoragePath.setText(settings.getStoragePath());

        try {
            String version = requireContext().getPackageManager()
                    .getPackageInfo(requireContext().getPackageName(), 0).versionName;
            tvAppVersion.setText("الإصدار " + version);
        } catch (Exception ignored) {}
    }

    private void setupListeners() {
        switchAutoRecord.setOnCheckedChangeListener((v, checked) ->
                settings.setAutoRecordEnabled(checked));

        switchSkipShortCalls.setOnCheckedChangeListener((v, checked) ->
                settings.setSkipShortCallsEnabled(checked));

        switchNotification.setOnCheckedChangeListener((v, checked) ->
                settings.setShowNotificationEnabled(checked));

        switchVibration.setOnCheckedChangeListener((v, checked) ->
                settings.setVibrationEnabled(checked));

        switchSpeaker.setOnCheckedChangeListener((v, checked) ->
                settings.setRecordOnSpeakerEnabled(checked));

        switchWaveform.setOnCheckedChangeListener((v, checked) ->
                settings.setShowWaveformEnabled(checked));

        switchBiometric.setOnCheckedChangeListener((v, checked) -> {
            if (checked) setupBiometric();
            else {
                settings.setBiometricLockEnabled(false);
            }
        });

        requireView().findViewById(R.id.row_quality).setOnClickListener(v -> showQualityDialog());
        requireView().findViewById(R.id.row_format).setOnClickListener(v -> showFormatDialog());
        requireView().findViewById(R.id.row_theme).setOnClickListener(v -> showThemeDialog());
        requireView().findViewById(R.id.row_min_duration).setOnClickListener(v -> showMinDurationDialog());
        requireView().findViewById(R.id.row_auto_delete).setOnClickListener(v -> showAutoDeleteDialog());
        requireView().findViewById(R.id.row_reset).setOnClickListener(v -> showResetDialog());
        requireView().findViewById(R.id.row_rate).setOnClickListener(v -> openPlayStore());
        requireView().findViewById(R.id.row_share_app).setOnClickListener(v -> shareApp());
        requireView().findViewById(R.id.row_diagnostics).setOnClickListener(v -> showDiagnostics());
        requireView().findViewById(R.id.row_test_recording).setOnClickListener(v -> runTestRecording());
    }

    // ── Diagnostics ────────────────────────────────────────────────────────────

    private void showDiagnostics() {
        Context ctx = requireContext();
        String log = DiagLog.read(ctx);
        String header = "Android " + Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + "), "
                + Build.MANUFACTURER + " " + Build.MODEL + ", app " + BuildConfig.VERSION_NAME + "\n\n";
        String text = header + (log.isEmpty() ? "(السجل فارغ: لم تُسجَّل أي أحداث بعد)" : log);
        new MaterialAlertDialogBuilder(ctx)
                .setTitle("سجل التشخيص")
                .setMessage(text)
                .setPositiveButton("نسخ", (d, w) -> {
                    ClipboardManager cm = (ClipboardManager) ctx.getSystemService(Context.CLIPBOARD_SERVICE);
                    cm.setPrimaryClip(ClipData.newPlainText("diagnostics", text));
                    Toast.makeText(ctx, "تم النسخ، الصقه في المحادثة", Toast.LENGTH_SHORT).show();
                })
                .setNeutralButton("مسح", (d, w) -> {
                    DiagLog.clear(ctx);
                    Toast.makeText(ctx, "تم مسح السجل", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("إغلاق", null)
                .show();
    }

    /** Records 3 s from the mic outside a call to check storage + microphone on their own. */
    private void runTestRecording() {
        Context ctx = requireContext().getApplicationContext();
        Toast.makeText(ctx, "جاري اختبار التسجيل لمدة 3 ثوانٍ… تكلّم الآن", Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            String result;
            File dir = new File(settings.getDefaultStoragePath());
            File file = new File(dir, "test_recording.m4a");
            MediaRecorder r = null;
            try {
                if (!dir.isDirectory() && !dir.mkdirs()) throw new IllegalStateException("mkdirs failed: " + dir);
                r = new MediaRecorder();
                r.setAudioSource(MediaRecorder.AudioSource.MIC);
                r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
                r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
                r.setAudioChannels(1);
                r.setAudioSamplingRate(44100);
                r.setAudioEncodingBitRate(128000);
                r.setOutputFile(file.getAbsolutePath());
                r.prepare();
                r.start();
                Thread.sleep(3000);
                r.stop();
                result = "نجح الاختبار ✅ حجم الملف " + file.length() + " بايت";
            } catch (Exception e) {
                result = "فشل الاختبار ❌ " + e.getClass().getSimpleName()
                        + (e.getMessage() != null ? ": " + e.getMessage() : "");
            } finally {
                if (r != null) try { r.release(); } catch (Exception ignored) {}
                //noinspection ResultOfMethodCallIgnored
                file.delete();
            }
            DiagLog.log(ctx, "TestRecording", result);
            final String msg = result;
            new Handler(Looper.getMainLooper()).post(() ->
                    Toast.makeText(ctx, msg, Toast.LENGTH_LONG).show());
        }).start();
    }

    private void showQualityDialog() {
        String[] options = {"منخفضة (AMR - 12 kbps، أصغر حجم)", "متوسطة (AAC - 96 kbps)", "عالية (AAC - 192 kbps، أفضل جودة)"};
        int current = settings.getRecordingQuality();
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("جودة التسجيل")
                .setSingleChoiceItems(options, current, (d, which) -> {
                    settings.setRecordingQuality(which);
                    updateQualityLabel();
                    d.dismiss();
                })
                .show();
    }

    private void showFormatDialog() {
        String[] options = {"MP4 (موصى به)", "AAC", "AMR"};
        int current = settings.getAudioFormat();
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("صيغة الملف")
                .setSingleChoiceItems(options, current, (d, which) -> {
                    settings.setAudioFormat(which);
                    updateFormatLabel();
                    d.dismiss();
                })
                .show();
    }

    private void showThemeDialog() {
        String[] options = {"تلقائي (حسب الجهاز)", "فاتح", "داكن"};
        int current = settings.getDarkMode();
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("مظهر التطبيق")
                .setSingleChoiceItems(options, current, (d, which) -> {
                    settings.setDarkMode(which);
                    updateThemeLabel();
                    applyTheme(which);
                    d.dismiss();
                })
                .show();
    }

    private void showMinDurationDialog() {
        String[] options = {"3 ثوانٍ", "5 ثوانٍ", "10 ثوانٍ", "15 ثواني", "30 ثانية"};
        int[] values = {3, 5, 10, 15, 30};
        int currentVal = settings.getMinCallDuration();
        int currentIdx = 1;
        for (int i = 0; i < values.length; i++) {
            if (values[i] == currentVal) { currentIdx = i; break; }
        }
        final int finalCurrentIdx = currentIdx;
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("الحد الأدنى لمدة المكالمة")
                .setSingleChoiceItems(options, finalCurrentIdx, (d, which) -> {
                    settings.setMinCallDuration(values[which]);
                    updateMinDurationLabel();
                    d.dismiss();
                })
                .show();
    }

    private void showAutoDeleteDialog() {
        String[] options = {"لا تحذف تلقائياً", "بعد 7 أيام", "بعد 30 يوماً", "بعد 90 يوماً"};
        int[] values = {0, 7, 30, 90};
        int currentVal = settings.getAutoDeleteDays();
        int currentIdx = 0;
        for (int i = 0; i < values.length; i++) {
            if (values[i] == currentVal) { currentIdx = i; break; }
        }
        final int finalIdx = currentIdx;
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("الحذف التلقائي")
                .setSingleChoiceItems(options, finalIdx, (d, which) -> {
                    settings.setAutoDeleteDays(values[which]);
                    updateAutoDeleteLabel();
                    d.dismiss();
                })
                .show();
    }

    private void showResetDialog() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("إعادة الضبط")
                .setMessage("هل تريد إعادة جميع الإعدادات إلى الافتراضي؟")
                .setPositiveButton("إعادة الضبط", (d, w) -> {
                    settings.resetToDefaults();
                    loadSettings();
                    Toast.makeText(requireContext(), "تمت إعادة الإعدادات", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("إلغاء", null)
                .show();
    }

    private void setupBiometric() {
        BiometricManager bm = BiometricManager.from(requireContext());
        if (bm.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                != BiometricManager.BIOMETRIC_SUCCESS) {
            switchBiometric.setChecked(false);
            Toast.makeText(requireContext(), "بصمة الإصبع غير متوفرة", Toast.LENGTH_SHORT).show();
            return;
        }

        Executor executor = ContextCompat.getMainExecutor(requireContext());
        BiometricPrompt prompt = new BiometricPrompt(requireActivity(), executor,
                new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                        settings.setBiometricLockEnabled(true);
                        Toast.makeText(requireContext(), "تم تفعيل قفل البصمة", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onAuthenticationFailed() {
                        switchBiometric.setChecked(false);
                    }

                    @Override
                    public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                        switchBiometric.setChecked(false);
                    }
                });

        BiometricPrompt.PromptInfo info = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("تأكيد البصمة")
                .setSubtitle("قم بالمصادقة لتفعيل القفل")
                .setNegativeButtonText("إلغاء")
                .build();
        prompt.authenticate(info);
    }

    private void applyTheme(int mode) {
        switch (mode) {
            case SettingsManager.THEME_LIGHT:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO); break;
            case SettingsManager.THEME_DARK:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES); break;
            default:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM); break;
        }
    }

    private void openPlayStore() {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("market://details?id=" + requireContext().getPackageName())));
        } catch (Exception e) {
            startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id="
                            + requireContext().getPackageName())));
        }
    }

    private void shareApp() {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT,
                "جرّب تطبيق تسجيل المكالمات!\nhttps://play.google.com/store/apps/details?id="
                        + requireContext().getPackageName());
        startActivity(Intent.createChooser(intent, "مشاركة التطبيق"));
    }

    private void updateQualityLabel() {
        String[] labels = {"منخفضة", "متوسطة", "عالية"};
        int q = settings.getRecordingQuality();
        tvQualityValue.setText(labels[Math.min(q, labels.length - 1)]);
    }

    private void updateFormatLabel() {
        String[] labels = {"MP4", "AAC", "AMR"};
        int f = settings.getAudioFormat();
        tvFormatValue.setText(labels[Math.min(f, labels.length - 1)]);
    }

    private void updateThemeLabel() {
        String[] labels = {"تلقائي", "فاتح", "داكن"};
        int t = settings.getDarkMode();
        tvThemeValue.setText(labels[Math.min(t, labels.length - 1)]);
    }

    private void updateMinDurationLabel() {
        tvMinDurationValue.setText(settings.getMinCallDuration() + " ثانية");
    }

    private void updateAutoDeleteLabel() {
        int days = settings.getAutoDeleteDays();
        tvAutoDeleteValue.setText(days == 0 ? "معطّل" : "بعد " + days + " يوماً");
    }
}
