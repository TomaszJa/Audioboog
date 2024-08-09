package com.example.audioboog.services;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.provider.MediaStore;
import android.widget.SeekBar;

import androidx.annotation.Nullable;

import com.example.audioboog.R;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MediaPlayerService extends Service implements MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener {
    private final IBinder binder = new LocalBinder();
    private static final String ACTION_PLAY = "com.example.action.PLAY";
    MediaPlayer mediaPlayer = null;
    String duration;
    ScheduledExecutorService timer;
    SharedPreferences sharedPreferences;
    Uri mediaUri;
    String filename;

    public class LocalBinder extends Binder {
        public MediaPlayerService getService() {
            // Return this instance of LocalService so clients can call public methods
            return MediaPlayerService.this;
        }
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        Uri uri = intent.getData();
        playMedia(uri);
        return START_STICKY;
    }

    public void playMedia(Uri uri) {
        if (mediaUri == null || !mediaUri.equals(uri))  {
            mediaUri = uri;
            if (mediaPlayer != null) releaseMediaPlayer();
            createMediaPlayer(mediaUri);
        }
        sharedPreferences = getSharedPreferences("sp", MODE_PRIVATE);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    /** Called when MediaPlayer is ready */
    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {
        startMediaPlayer();
    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        releaseMediaPlayer();
        stopSelf();
    }

    public void createMediaPlayer(Uri uri){
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioAttributes(
                new AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
        );
        try {
            mediaPlayer.setDataSource(getApplicationContext(), uri);
            mediaPlayer.setOnPreparedListener(this);
            mediaPlayer.setOnCompletionListener(this);
            mediaPlayer.prepare();

            filename = getNameFromUri(uri);
            int millis = mediaPlayer.getDuration();
            long total_secs = TimeUnit.SECONDS.convert(millis, TimeUnit.MILLISECONDS);
            long mins = TimeUnit.MINUTES.convert(total_secs, TimeUnit.SECONDS);
            long secs = total_secs - (mins*60);
            duration = mins + ":" + secs;
        } catch (IOException e){
        }
    }

    public String getNameFromUri(Uri uri){
        String fileName = "";
        Cursor cursor = null;
        cursor = getContentResolver().query(uri, new String[]{
                MediaStore.Images.ImageColumns.DISPLAY_NAME
        }, null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
//            fileName = cursor.getString(cursor.getColumnIndex(MediaStore.Images.ImageColumns.DISPLAY_NAME));
        }
        if (cursor != null) {
            cursor.close();
        }
        return fileName;
    }

    public void releaseMediaPlayer(){
        if (timer != null) {
            timer.shutdown();
        }
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        sharedPreferences.edit().putString("created", "false").apply();
    }

    public void playOrPause() {
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                pauseMediaPlayer();
            } else {
                startMediaPlayer();
            }
        }
    }

    private void startMediaPlayer() {
        mediaPlayer.start();
    }

    private void pauseMediaPlayer() {
        mediaPlayer.pause();
    }

    public void fastForward() {
        if (mediaPlayer != null) {
            int position = mediaPlayer.getCurrentPosition() + 15000;
            mediaPlayer.seekTo(position);
        }
    }

    public void fastRewind() {
        if (mediaPlayer != null) {
            int position = Math.max(0, mediaPlayer.getCurrentPosition() - 15000);
            mediaPlayer.seekTo(position);
        }
    }

    public void seekMediaPlayer(int position){
        if (mediaPlayer != null) {
            mediaPlayer.seekTo(position);
        }
    }

    public boolean isPlaying() {
        if (mediaPlayer != null) {
            return mediaPlayer.isPlaying();
        }
        return false;
    }

    public MediaPlayer getMediaPlayer() {
        return mediaPlayer;
    }
}
