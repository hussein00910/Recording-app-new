package com.callrecorder.app.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.callrecorder.app.models.Recording;

@Database(entities = {Recording.class}, version = 2, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    private static final String DATABASE_NAME = "call_recorder_database";
    private static volatile AppDatabase INSTANCE;

    public abstract RecordingDao recordingDao();

    static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE recordings ADD COLUMN fileSize INTEGER NOT NULL DEFAULT 0");
            database.execSQL("ALTER TABLE recordings ADD COLUMN playCount INTEGER NOT NULL DEFAULT 0");
            database.execSQL("ALTER TABLE recordings ADD COLUMN isBookmarked INTEGER NOT NULL DEFAULT 0");
            database.execSQL("ALTER TABLE recordings ADD COLUMN tags TEXT");
        }
    };

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            DATABASE_NAME)
                            .addMigrations(MIGRATION_1_2)
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
