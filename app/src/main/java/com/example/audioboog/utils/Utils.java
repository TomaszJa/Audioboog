package com.example.audioboog.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import androidx.annotation.Nullable;

import com.example.audioboog.R;

public class Utils {
    public static Bitmap convertEmbeddedPictureToBitmap(Context context, @Nullable byte[] picture) {
        if (picture != null) {
            return BitmapFactory.decodeByteArray(picture, 0, picture.length);
        } else {

            return BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_book);
        }
    }
}
