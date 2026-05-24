package com.callrecorder.app.utils;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;

public class ContactUtils {

    public static String getContactName(Context context, String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isEmpty()) return null;
        try {
            Uri uri = Uri.withAppendedPath(
                    ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                    Uri.encode(phoneNumber));
            Cursor cursor = context.getContentResolver().query(
                    uri,
                    new String[]{ContactsContract.PhoneLookup.DISPLAY_NAME},
                    null, null, null);
            if (cursor != null) {
                try {
                    if (cursor.moveToFirst()) return cursor.getString(0);
                } finally {
                    cursor.close();
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    public static String formatPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isEmpty()) return "";
        String digits = phoneNumber.replaceAll("[^0-9+]", "");
        if (digits.startsWith("+")) return digits;
        if (digits.length() == 10) {
            return digits.substring(0, 3) + "-" + digits.substring(3, 6) + "-" + digits.substring(6);
        }
        if (digits.length() == 11 && digits.startsWith("0")) {
            return digits.substring(0, 4) + "-" + digits.substring(4, 7) + "-" + digits.substring(7);
        }
        return phoneNumber;
    }

    public static String getContactPhotoUri(Context context, String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isEmpty()) return null;
        try {
            Uri uri = Uri.withAppendedPath(
                    ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                    Uri.encode(phoneNumber));
            Cursor cursor = context.getContentResolver().query(
                    uri,
                    new String[]{ContactsContract.PhoneLookup.PHOTO_URI},
                    null, null, null);
            if (cursor != null) {
                try {
                    if (cursor.moveToFirst()) return cursor.getString(0);
                } finally {
                    cursor.close();
                }
            }
        } catch (Exception ignored) {}
        return null;
    }
}
