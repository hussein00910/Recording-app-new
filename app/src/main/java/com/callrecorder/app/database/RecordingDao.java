package com.callrecorder.app.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.callrecorder.app.models.Recording;

import java.util.List;

@Dao
public interface RecordingDao {

    @Query("SELECT * FROM recordings ORDER BY date DESC")
    LiveData<List<Recording>> getAllRecordings();

    @Query("SELECT * FROM recordings WHERE isStarred = 1 ORDER BY date DESC")
    LiveData<List<Recording>> getStarredRecordings();

    @Query("SELECT * FROM recordings WHERE isBookmarked = 1 ORDER BY date DESC")
    LiveData<List<Recording>> getBookmarkedRecordings();

    @Query("SELECT * FROM recordings WHERE callType = :callType ORDER BY date DESC")
    LiveData<List<Recording>> getRecordingsByType(int callType);

    @Query("SELECT * FROM recordings WHERE contactName LIKE '%' || :query || '%' OR phoneNumber LIKE '%' || :query || '%' ORDER BY date DESC")
    LiveData<List<Recording>> searchRecordings(String query);

    @Query("SELECT * FROM recordings WHERE date >= :fromDate AND date <= :toDate ORDER BY date DESC")
    LiveData<List<Recording>> getRecordingsByDateRange(long fromDate, long toDate);

    @Query("SELECT * FROM recordings WHERE phoneNumber = :phoneNumber ORDER BY date DESC")
    LiveData<List<Recording>> getRecordingsByPhone(String phoneNumber);

    @Insert
    long insert(Recording recording);

    @Update
    void update(Recording recording);

    @Delete
    void delete(Recording recording);

    @Query("DELETE FROM recordings WHERE id = :id")
    void deleteById(long id);

    @Query("SELECT * FROM recordings WHERE id = :id")
    Recording getRecordingById(long id);

    @Query("SELECT * FROM recordings WHERE id = :id")
    LiveData<Recording> getRecordingByIdLive(long id);

    @Query("SELECT COUNT(*) FROM recordings")
    LiveData<Integer> getTotalCount();

    @Query("SELECT SUM(duration) FROM recordings")
    LiveData<Long> getTotalDuration();

    @Query("SELECT COUNT(*) FROM recordings WHERE callType = 1")
    LiveData<Integer> getIncomingCount();

    @Query("SELECT COUNT(*) FROM recordings WHERE callType = 2")
    LiveData<Integer> getOutgoingCount();

    @Query("SELECT COUNT(*) FROM recordings WHERE isStarred = 1")
    LiveData<Integer> getStarredCount();

    @Query("SELECT SUM(fileSize) FROM recordings")
    LiveData<Long> getTotalFileSize();

    @Query("UPDATE recordings SET playCount = playCount + 1 WHERE id = :id")
    void incrementPlayCount(long id);

    @Query("SELECT * FROM recordings ORDER BY playCount DESC LIMIT :limit")
    LiveData<List<Recording>> getMostPlayed(int limit);

    @Query("DELETE FROM recordings")
    void deleteAll();
}
