package com.callrecorder.app.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.callrecorder.app.R;
import com.callrecorder.app.activities.PlaybackActivity;
import com.callrecorder.app.adapters.RecordingsAdapter;
import com.callrecorder.app.models.Recording;
import com.callrecorder.app.viewmodels.RecordingsViewModel;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import java.util.concurrent.TimeUnit;

public class RecordingsFragment extends Fragment implements RecordingsAdapter.RecordingItemListener {

    private RecordingsViewModel viewModel;
    private RecordingsAdapter adapter;
    private RecyclerView recyclerView;
    private View emptyView;
    private EditText searchEditText;
    private ChipGroup filterChipGroup;

    // Stats views
    private TextView statsTotal;
    private TextView statsDuration;
    private TextView statsIncoming;
    private TextView statsOutgoing;
    private View statsCard;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        return inflater.inflate(R.layout.fragment_recordings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(RecordingsViewModel.class);

        recyclerView = view.findViewById(R.id.recycler_view_recordings);
        emptyView = view.findViewById(R.id.empty_view);
        searchEditText = view.findViewById(R.id.edit_text_search);
        filterChipGroup = view.findViewById(R.id.chip_group_filter);
        statsCard = view.findViewById(R.id.stats_card);
        statsTotal = view.findViewById(R.id.text_stats_total);
        statsDuration = view.findViewById(R.id.text_stats_duration);
        statsIncoming = view.findViewById(R.id.text_stats_incoming);
        statsOutgoing = view.findViewById(R.id.text_stats_outgoing);

        setupRecyclerView();
        setupSearch();
        setupFilterChips();
        observeRecordings();
        observeStatistics();
    }

    private void setupRecyclerView() {
        adapter = new RecordingsAdapter(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);
        recyclerView.setHasFixedSize(true);

        // Swipe-to-delete
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            @Override
            public boolean onMove(@NonNull RecyclerView rv,
                                  @NonNull RecyclerView.ViewHolder vh,
                                  @NonNull RecyclerView.ViewHolder target) { return false; }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int pos = viewHolder.getAdapterPosition();
                Recording r = adapter.getCurrentList().get(pos);
                viewModel.deleteRecording(r);
                Snackbar.make(recyclerView, "تم حذف التسجيل", Snackbar.LENGTH_LONG)
                        .setAction("تراجع", v -> viewModel.insertRecording(r))
                        .show();
            }
        }).attachToRecyclerView(recyclerView);
    }

    private void setupSearch() {
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                viewModel.setSearchQuery(s.toString());
            }
        });
    }

    private void setupFilterChips() {
        filterChipGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chip_all) viewModel.setFilter(0);
            else if (checkedId == R.id.chip_starred) viewModel.setFilter(-1);
            else if (checkedId == R.id.chip_incoming) viewModel.setFilter(1);
            else if (checkedId == R.id.chip_outgoing) viewModel.setFilter(2);
            else if (checkedId == R.id.chip_bookmarked) viewModel.setFilter(-2);
        });
    }

    private void observeRecordings() {
        viewModel.getSearchResults().observe(getViewLifecycleOwner(), recordings -> {
            adapter.submitList(recordings);
            boolean empty = recordings == null || recordings.isEmpty();
            emptyView.setVisibility(empty ? View.VISIBLE : View.GONE);
            recyclerView.setVisibility(empty ? View.GONE : View.VISIBLE);
        });
    }

    private void observeStatistics() {
        viewModel.totalCount.observe(getViewLifecycleOwner(), count -> {
            if (count != null) statsTotal.setText(String.valueOf(count));
        });
        viewModel.totalDuration.observe(getViewLifecycleOwner(), duration -> {
            if (duration != null) {
                long hours = TimeUnit.MILLISECONDS.toHours(duration);
                long mins = TimeUnit.MILLISECONDS.toMinutes(duration) % 60;
                statsDuration.setText(hours > 0 ? hours + "س " + mins + "د" : mins + " دقيقة");
            }
        });
        viewModel.incomingCount.observe(getViewLifecycleOwner(), c -> {
            if (c != null) statsIncoming.setText(String.valueOf(c));
        });
        viewModel.outgoingCount.observe(getViewLifecycleOwner(), c -> {
            if (c != null) statsOutgoing.setText(String.valueOf(c));
        });
    }

    @Override
    public void onItemClick(Recording recording) {
        viewModel.incrementPlayCount(recording.getId());
        Intent intent = new Intent(requireContext(), PlaybackActivity.class);
        intent.putExtra("recording_id", recording.getId());
        startActivity(intent);
    }

    @Override
    public void onStarClick(Recording recording) {
        viewModel.toggleStar(recording);
    }

    @Override
    public void onDeleteClick(Recording recording) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("حذف التسجيل")
                .setMessage("هل أنت متأكد من حذف تسجيل " + recording.getDisplayName() + "؟")
                .setPositiveButton("حذف", (d, w) -> viewModel.deleteRecording(recording))
                .setNegativeButton("إلغاء", null)
                .show();
    }

    @Override
    public void onShareClick(Recording recording) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("audio/*");
        shareIntent.putExtra(Intent.EXTRA_STREAM,
                androidx.core.content.FileProvider.getUriForFile(
                        requireContext(),
                        requireContext().getPackageName() + ".provider",
                        new java.io.File(recording.getFilePath())));
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(shareIntent, "مشاركة التسجيل"));
    }

    @Override
    public void onBookmarkClick(Recording recording) {
        viewModel.toggleBookmark(recording);
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.menu_recordings, menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_sort) {
            showSortDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showSortDialog() {
        String[] options = {"الأحدث أولاً", "الأقدم أولاً", "الأطول مدةً", "المفضلة أولاً"};
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("ترتيب التسجيلات")
                .setItems(options, (d, w) -> {
                    // sorting handled via DAO queries in a future enhancement
                })
                .show();
    }
}
