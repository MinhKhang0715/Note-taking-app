package com.example.noteapp.helpers;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;

public class Utils {
    public static String getPathFromUri(Uri imgUri, ContentResolver contentResolver) {
        String path;
        Cursor cursor = contentResolver.query(imgUri, null, null, null, null);
        if (cursor == null)
            return imgUri.getPath();
        else {
            cursor.moveToFirst();
            int index = cursor.getColumnIndex("_data");
            path = cursor.getString(index);
            cursor.close();
            return path;
        }
    }

    /**
     * The class uses the debounce technique to limit the creation of Toast objects when the user
     * repeatedly presses a button, typically in cases where important information like a note's title
     * is missing. This ensures that only a single Toast is shown, even with frequent button presses.
     */
    public static class Debounce {
        private final long debounceTimeMillis;
        private long lastClickTime;

        public Debounce(long debounceTimeMillis) {
            this.debounceTimeMillis = debounceTimeMillis;
        }

        public boolean debounce() {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastClickTime > debounceTimeMillis) {
                lastClickTime = currentTime;
                return false;
            }
            return true;
        }
    }
}
