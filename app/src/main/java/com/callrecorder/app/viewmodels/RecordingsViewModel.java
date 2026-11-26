package com.callrecorder.app.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.callrecorder.app.database.AppDatabase;
import com.callrecorder.app.database.RecordingDao;
import com.callrecorder.app.models.Recording;

import java.io.File;
import java.util.List;
import java.util.concurrent.Executors;

public class RecordingsViewModel extends AndroidViewModel {
    private final RecordingDao dao;
    private final LiveData<List<Recording>> allRecordings;
    private final MutableLiveData<Integer> filterType = new MutableLiveData<>(0);
    private final MutableLiveData<String> searchQuery = new MutableLiveData<>("");

    // Statistics LiveData
    public final LiveData<Integer> totalCount;
    public final LiveData<Long> totalDuration;
    public final LiveData<Integer> incomingCount;
    public final LiveData<Integer> outgoingCount;
    public final LiveData<Integer> starredCount;
    public final LiveData<Long> totalFileSize;

    public RecordingsViewModel(@NonNull Application application) {
        super(application);
        dao = AppDatabase.getInstance(application).recordingDao();
        allRecordings = dao.getAllRecordings();

        totalCount = dao.getTotalCount();
        totalDuration = dao.getTotalDuration();
        incomingCount = dao.getIncomingCount();
        outgoingCount = dao.getOutgoingCount();
        starredCount = dao.getStarredCount();
        totalFileSize = dao.getTotalFileSize();
    }

    public LiveData<List<Recording>> getFilteredRecordings() {
        return Transformations.switchMap(filterType, type -> {
            switch (type) {
                case -1: return dao.getStarredRecordings();
                case -2: return dao.getBookmarkedRecordings();
                case 1: return dao.getRecordingsByType(1);
                case 2: return dao.getRecordingsByType(2);
                default: return allRecordings;
            }
        });
    }

    public LiveData<List<Recording>> getSearchResults() {
        return Transformations.switchMap(searchQuery, query -> {
            if (query == null || query.trim().isEmpty()) {
                return getFilteredRecordings();
            }
            return dao.searchRecordings(query.trim());
        });
    }

    public LiveData<List<Recording>> getAllRecordings() { return allRecordings; }

    public void setFilter(int type) { filterType.setValue(type); }
    public void setSearchQuery(String query) { searchQuery.setValue(query); }
    public int getCurrentFilter() { return filterType.getValue() != null ? filterType.getValue() : 0; }

    public void toggleStar(Recording recording) {
        recording.setStarred(!recording.isStarred());
        updateRecording(recording);
    }

    public void toggleBookmark(Recording recording) {
        recording.setBookmarked(!recording.isBookmarked());
        updateRecording(recording);
    }

    public void updateNotes(Recording recording, String notes) {
        recording.setNotes(notes);
        updateRecording(recording);
    }

    public void insertRecording(Recording recording) {
        Executors.newSingleThreadExecutor().execute(() -> dao.insert(recording));
    }

    public void updateRecording(Recording recording) {
        Executors.newSingleThreadExecutor().execute(() -> dao.update(recording));
    }

    public void deleteRecording(Recording recording) {
        Executors.newSingleThreadExecutor().execute(() -> {
            File file = new File(recording.getFilePath());
            if (file.exists()) file.delete();
            dao.delete(recording);
        });
    }

    public void incrementPlayCount(long recordingId) {
        Executors.newSingleThreadExecutor().execute(() -> dao.incrementPlayCount(recordingId));
    }

    public LiveData<Recording> getRecordingById(long id) {
        return dao.getRecordingByIdLive(id);
    }
}
