package com.example.audioboog.database.converters;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.room.TypeConverter;

import com.example.audioboog.source.Chapter;

import java.nio.charset.Charset;
import java.util.Arrays;

public class ByteArrayConverter {
    @TypeConverter
    public byte[] storedStringToByteArray(String byteString) {
        return byteString.getBytes(Charset.defaultCharset());
    }

    @TypeConverter
    public String bytesToStoredString(byte[] bytes) {
        return new String(bytes, Charset.defaultCharset());
    }
}
