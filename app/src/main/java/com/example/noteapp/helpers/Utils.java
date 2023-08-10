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
}
