package com.example.audioboog.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import androidx.annotation.Nullable;

import com.example.audioboog.R;

import java.util.concurrent.TimeUnit;

public class Utils {
    public static Bitmap convertEmbeddedPictureToBitmap(Context context, @Nullable byte[] picture) {
        if (picture != null) {
            return BitmapFactory.decodeByteArray(picture, 0, picture.length);
        } else {

            return BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_book);
        }
    }

    public static  String convertPlayingTimeToString(int milliseconds) {
        long secs = TimeUnit.SECONDS.convert(milliseconds, TimeUnit.MILLISECONDS);
        long mins = TimeUnit.MINUTES.convert(secs, TimeUnit.SECONDS);
        long hours = TimeUnit.HOURS.convert(mins, TimeUnit.MINUTES);

        secs = secs - (mins * 60);
        mins = mins - (hours * 60);

        String hoursString = ((hours < 10) ? ("0" + hours) : hours).toString();
        String minsString = ((mins < 10) ? ("0" + mins) : mins).toString();
        String secsString = ((secs < 10) ? ("0" + secs) : secs).toString();
        return hoursString + ":" + minsString + ":" + secsString;
    }
}
