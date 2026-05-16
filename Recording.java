package com.callrecorder.app.models;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "recordings")
public class Recording {
    @PrimaryKey(autoGenerate = true)
    private long id;
    private String phoneNumber;
    private String contactName;
    private int callType; // 1=incoming, 2=outgoing, 3=missed
    private String filePath;
    private long duration;
    private long date;
    private boolean isStarred;
    private String notes;
    private long fileSize;
    private int playCount;
    private boolean isBookmarked;
    private String tags;

    public Recording(long id, String phoneNumber, String contactName, int callType,
                    String filePath, long duration, long date, boolean isStarred, String notes) {
        this.id = id;
        this.phoneNumber = phoneNumber;
        this.contactName = contactName;
        this.callType = callType;
        this.filePath = filePath;
        this.duration = duration;
        this.date = date;
        this.isStarred = isStarred;
        this.notes = notes;
        this.fileSize = 0;
        this.playCount = 0;
        this.isBookmarked = false;
        this.tags = null;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getContactName() { return contactName; }
    public void setContactName(String contactName) { this.contactName = contactName; }

    public int getCallType() { return callType; }
    public void setCallType(int callType) { this.callType = callType; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public long getDuration() { return duration; }
    public void setDuration(long duration) { this.duration = duration; }

    public long getDate() { return date; }
    public void setDate(long date) { this.date = date; }

    public boolean isStarred() { return isStarred; }
    public void setStarred(boolean starred) { isStarred = starred; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public long getFileSize() { return fileSize; }
    public void setFileSize(long fileSize) { this.fileSize = fileSize; }

    public int getPlayCount() { return playCount; }
    public void setPlayCount(int playCount) { this.playCount = playCount; }

    public boolean isBookmarked() { return isBookmarked; }
    public void setBookmarked(boolean bookmarked) { isBookmarked = bookmarked; }

    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }

    public String getDisplayName() {
        if (contactName != null && !contactName.isEmpty()) return contactName;
        if (phoneNumber != null && !phoneNumber.isEmpty()) return phoneNumber;
        return "رقم غير معروف";
    }

    public String getFormattedDuration() {
        long totalSeconds = duration / 1000;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        if (hours > 0) {
            return String.format("%d:%02d:%02d", hours, minutes, seconds);
        }
        return String.format("%d:%02d", minutes, seconds);
    }

    public String getFormattedFileSize() {
        if (fileSize <= 0) return "";
        if (fileSize < 1024) return fileSize + " B";
        if (fileSize < 1024 * 1024) return String.format("%.1f KB", fileSize / 1024.0);
        return String.format("%.1f MB", fileSize / (1024.0 * 1024));
    }
}
