package com.example.audioboog.database.converters;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.room.TypeConverter;

import com.example.audioboog.source.Chapter;

import java.nio.charset.Charset;
import java.util.Arrays;

public class UriConverter {
    @TypeConverter
    public Uri storedStringToUri(String uriString) {
        return Uri.parse(uriString);
    }

    @TypeConverter
    public String uriToStoredString(Uri uri) {
        return uri.toString();
    }
}
