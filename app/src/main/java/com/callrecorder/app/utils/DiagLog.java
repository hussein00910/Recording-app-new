package com.callrecorder.app.utils;

import android.content.Context;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Small on-device diagnostic log (app-private file) so the user can see and share
 * what happened during a call without needing adb/logcat.
 */
public final class DiagLog {
    private static final String TAG = "DiagLog";
    private static final String FILE_NAME = "diagnostics.log";
    private static final long MAX_BYTES = 64 * 1024;

    private DiagLog() {}

    public static synchronized void log(Context context, String tag, String message) {
        Log.d(tag, message);
        if (context == null) return;
        try {
            File f = file(context);
            if (f.length() > MAX_BYTES) {
                // keep the newer half
                String all = readFile(f);
                try (OutputStream out = new FileOutputStream(f, false)) {
                    out.write(all.substring(all.length() / 2).getBytes("UTF-8"));
                }
            }
            String time = new SimpleDateFormat("MM-dd HH:mm:ss", Locale.US).format(new Date());
            try (FileWriter w = new FileWriter(f, true)) {
                w.write(time + " [" + tag + "] " + message + "\n");
            }
        } catch (Exception e) {
            Log.w(TAG, "Could not write diagnostics", e);
        }
    }

    public static synchronized String read(Context context) {
        try {
            File f = file(context);
            if (!f.exists()) return "";
            return readFile(f);
        } catch (Exception e) {
            return "تعذّرت قراءة السجل: " + e;
        }
    }

    public static synchronized void clear(Context context) {
        //noinspection ResultOfMethodCallIgnored
        file(context).delete();
    }

    private static String readFile(File f) throws IOException {
        try (InputStream in = new FileInputStream(f)) {
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            byte[] chunk = new byte[8192];
            int n;
            while ((n = in.read(chunk)) > 0) buf.write(chunk, 0, n);
            return buf.toString("UTF-8");
        }
    }

    private static File file(Context context) {
        return new File(context.getApplicationContext().getFilesDir(), FILE_NAME);
    }
}
