package com.yy200.revprint;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.FontsContract;
import android.provider.OpenableColumns;
import android.provider.SyncStateContract;
import android.util.Log;
import android.widget.Toast;

import java.net.URISyntaxException;

import static android.content.ContentValues.TAG;

/*
 * Created by Yassir on 3/29/2018.
 */


public class FileUtils {
    public static String getPath(Activity context, Uri uri) throws URISyntaxException {
        if ("content".equalsIgnoreCase(uri.getScheme())) {
            Cursor cursor = null;
            try{
                cursor = context.getContentResolver().query(uri,null, null, null, null, null);
                int column_index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (cursor.moveToFirst()) {
                    Log.i("ABCD", "Name: "+ cursor.getString(column_index));
                    return cursor.getString(column_index);
                }
            } catch (Exception e) {
                // Eat it
            }
        }
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }
}
