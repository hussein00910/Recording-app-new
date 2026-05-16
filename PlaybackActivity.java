package com.callrecorder.app.activities;

import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;

import com.callrecorder.app.R;
import com.callrecorder.app.models.Recording;
import com.callrecorder.app.viewmodels.RecordingsViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.chip.Chip;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class PlaybackActivity extends AppCompatActivity {

    private RecordingsViewModel viewModel;
    private Recording currentRecording;
    private MediaPlayer mediaPlayer;
    private Handler handler = new Handler(Looper.getMainLooper());

    private FloatingActionButton fabPlayPause;
    private ImageButton btnRewind, btnForward, btnStop;
    private SeekBar seekBar;
    private TextView tvCurrentTime, tvTotalTime;
    private TextView tvName, tvPhone, tvDate, tvDuration, tvFileSize, tvCallType;
    private MaterialButtonToggleGroup speedToggle;
    private TextInputEditText notesInput;
    private MaterialButton btnSaveNotes;
    private Chip chipStar, chipBookmark;
    private View loadingView;

    private float currentSpeed = 1.0f;
    private boolean isPlaying = false;
    private boolean isPrepared = false;

    private final Runnable updateSeekBar = new Runnable() {
        @Override
        public void run() {
            if (mediaPlayer != null && isPlaying) {
                seekBar.setProgress(mediaPlayer.getCurrentPosition());
                tvCurrentTime.setText(formatTime(mediaPlayer.getCurrentPosition()));
                handler.postDelayed(this, 500);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playback);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("تشغيل التسجيل");
        }

        viewModel = new ViewModelProvider(this).get(RecordingsViewModel.class);

        initViews();
        setupListeners();

        long recordingId = getIntent().getLongExtra("recording_id", -1);
        if (recordingId != -1) {
            viewModel.getRecordingById(recordingId).observe(this, recording -> {
                if (recording != null) {
                    currentRecording = recording;
                    populateDetails();
                    preparePlayer();
                }
            });
        } else {
            Toast.makeText(this, "خطأ: لم يتم العثور على التسجيل", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void initViews() {
        fabPlayPause = findViewById(R.id.fab_play_pause);
        btnRewind = findViewById(R.id.btn_rewind);
        btnForward = findViewById(R.id.btn_forward);
        btnStop = findViewById(R.id.btn_stop);
        seekBar = findViewById(R.id.seek_bar);
        tvCurrentTime = findViewById(R.id.tv_current_time);
        tvTotalTime = findViewById(R.id.tv_total_time);
        tvName = findViewById(R.id.tv_recording_name);
        tvPhone = findViewById(R.id.tv_phone_number);
        tvDate = findViewById(R.id.tv_date);
        tvDuration = findViewById(R.id.tv_duration);
        tvFileSize = findViewById(R.id.tv_file_size);
        tvCallType = findViewById(R.id.tv_call_type);
        speedToggle = findViewById(R.id.speed_toggle);
        notesInput = findViewById(R.id.input_notes);
        btnSaveNotes = findViewById(R.id.btn_save_notes);
        chipStar = findViewById(R.id.chip_star);
        chipBookmark = findViewById(R.id.chip_bookmark);
        loadingView = findViewById(R.id.loading_view);
    }

    private void setupListeners() {
        fabPlayPause.setOnClickListener(v -> togglePlayPause());

        btnRewind.setOnClickListener(v -> {
            if (mediaPlayer != null && isPrepared) {
                int pos = Math.max(0, mediaPlayer.getCurrentPosition() - 10000);
                mediaPlayer.seekTo(pos);
                seekBar.setProgress(pos);
                tvCurrentTime.setText(formatTime(pos));
            }
        });

        btnForward.setOnClickListener(v -> {
            if (mediaPlayer != null && isPrepared) {
                int pos = Math.min(mediaPlayer.getDuration(),
                        mediaPlayer.getCurrentPosition() + 10000);
                mediaPlayer.seekTo(pos);
                seekBar.setProgress(pos);
                tvCurrentTime.setText(formatTime(pos));
            }
        });

        btnStop.setOnClickListener(v -> stopPlayback());

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {
                if (fromUser && mediaPlayer != null && isPrepared) {
                    mediaPlayer.seekTo(progress);
                    tvCurrentTime.setText(formatTime(progress));
                }
            }
            @Override public void onStartTrackingTouch(SeekBar sb) {}
            @Override public void onStopTrackingTouch(SeekBar sb) {}
        });

        speedToggle.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            if (checkedId == R.id.btn_speed_05) setPlaybackSpeed(0.5f);
            else if (checkedId == R.id.btn_speed_1) setPlaybackSpeed(1.0f);
            else if (checkedId == R.id.btn_speed_15) setPlaybackSpeed(1.5f);
            else if (checkedId == R.id.btn_speed_2) setPlaybackSpeed(2.0f);
        });

        btnSaveNotes.setOnClickListener(v -> {
            if (currentRecording != null && notesInput.getText() != null) {
                viewModel.updateNotes(currentRecording, notesInput.getText().toString());
                Toast.makeText(this, "تم حفظ الملاحظات", Toast.LENGTH_SHORT).show();
            }
        });

        chipStar.setOnClickListener(v -> {
            if (currentRecording != null) {
                viewModel.toggleStar(currentRecording);
                updateChipStates();
            }
        });

        chipBookmark.setOnClickListener(v -> {
            if (currentRecording != null) {
                viewModel.toggleBookmark(currentRecording);
                updateChipStates();
            }
        });
    }

    private void populateDetails() {
        tvName.setText(currentRecording.getDisplayName());
        tvPhone.setText(currentRecording.getPhoneNumber() != null ?
                currentRecording.getPhoneNumber() : "غير معروف");

        SimpleDateFormat fmt = new SimpleDateFormat("EEEE، dd MMMM yyyy\nhh:mm a", new Locale("ar"));
        tvDate.setText(fmt.format(new Date(currentRecording.getDate())));

        tvDuration.setText(currentRecording.getFormattedDuration());
        tvFileSize.setText(currentRecording.getFormattedFileSize());

        String callTypeStr = currentRecording.getCallType() == 1 ? "مكالمة واردة" : "مكالمة صادرة";
        tvCallType.setText(callTypeStr);

        if (currentRecording.getNotes() != null) {
            notesInput.setText(currentRecording.getNotes());
        }

        updateChipStates();
    }

    private void updateChipStates() {
        chipStar.setChecked(currentRecording.isStarred());
        chipStar.setText(currentRecording.isStarred() ? "مفضّل ★" : "إضافة للمفضلة");
        chipBookmark.setChecked(currentRecording.isBookmarked());
        chipBookmark.setText(currentRecording.isBookmarked() ? "محفوظ 🔖" : "حفظ للمرجع");
    }

    private void preparePlayer() {
        if (currentRecording == null) return;

        File file = new File(currentRecording.getFilePath());
        if (!file.exists()) {
            Toast.makeText(this, "ملف الصوت غير موجود", Toast.LENGTH_LONG).show();
            return;
        }

        loadingView.setVisibility(View.VISIBLE);
        fabPlayPause.setEnabled(false);

        mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(file.getAbsolutePath());
            mediaPlayer.prepareAsync();

            mediaPlayer.setOnPreparedListener(mp -> {
                isPrepared = true;
                loadingView.setVisibility(View.GONE);
                fabPlayPause.setEnabled(true);

                int duration = mp.getDuration();
                seekBar.setMax(duration);
                tvTotalTime.setText(formatTime(duration));
                tvCurrentTime.setText(formatTime(0));

                applySpeed(currentSpeed);
            });

            mediaPlayer.setOnCompletionListener(mp -> {
                isPlaying = false;
                fabPlayPause.setImageResource(R.drawable.ic_play);
                handler.removeCallbacks(updateSeekBar);
                seekBar.setProgress(0);
                tvCurrentTime.setText(formatTime(0));
                mediaPlayer.seekTo(0);
            });

            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                Toast.makeText(this, "خطأ في تشغيل الملف", Toast.LENGTH_SHORT).show();
                loadingView.setVisibility(View.GONE);
                return true;
            });

        } catch (IOException e) {
            Toast.makeText(this, "لا يمكن تشغيل هذا الملف", Toast.LENGTH_SHORT).show();
            loadingView.setVisibility(View.GONE);
        }
    }

    private void togglePlayPause() {
        if (!isPrepared || mediaPlayer == null) return;

        if (isPlaying) {
            mediaPlayer.pause();
            isPlaying = false;
            fabPlayPause.setImageResource(R.drawable.ic_play);
            handler.removeCallbacks(updateSeekBar);
        } else {
            mediaPlayer.start();
            isPlaying = true;
            fabPlayPause.setImageResource(R.drawable.ic_pause);
            handler.post(updateSeekBar);
        }
    }

    private void stopPlayback() {
        if (mediaPlayer != null && isPrepared) {
            mediaPlayer.stop();
            isPlaying = false;
            isPrepared = false;
            fabPlayPause.setImageResource(R.drawable.ic_play);
            handler.removeCallbacks(updateSeekBar);
            seekBar.setProgress(0);
            tvCurrentTime.setText(formatTime(0));
            preparePlayer();
        }
    }

    private void setPlaybackSpeed(float speed) {
        currentSpeed = speed;
        if (mediaPlayer != null && isPrepared) {
            applySpeed(speed);
        }
    }

    private void applySpeed(float speed) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            android.media.PlaybackParams params = new android.media.PlaybackParams();
            params.setSpeed(speed);
            try {
                mediaPlayer.setPlaybackParams(params);
            } catch (Exception ignored) {}
        }
    }

    private String formatTime(long ms) {
        long secs = ms / 1000;
        return String.format(Locale.getDefault(), "%d:%02d", secs / 60, secs % 60);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_playback, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        } else if (id == R.id.action_share) {
            shareRecording();
            return true;
        } else if (id == R.id.action_delete) {
            confirmDelete();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void shareRecording() {
        if (currentRecording == null) return;
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("audio/*");
        Uri uri = androidx.core.content.FileProvider.getUriForFile(
                this, getPackageName() + ".provider",
                new File(currentRecording.getFilePath()));
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(intent, "مشاركة التسجيل"));
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setTitle("حذف التسجيل")
                .setMessage("هل تريد حذف هذا التسجيل نهائياً؟")
                .setPositiveButton("حذف", (d, w) -> {
                    viewModel.deleteRecording(currentRecording);
                    finish();
                })
                .setNegativeButton("إلغاء", null)
                .show();
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(updateSeekBar);
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
        super.onDestroy();
    }
}
