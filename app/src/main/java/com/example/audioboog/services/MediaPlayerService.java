package com.example.audioboog.services;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.provider.OpenableColumns;

import androidx.annotation.Nullable;

import com.example.audioboog.source.Audiobook;
import com.example.audioboog.source.Chapter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.ScheduledExecutorService;

public class MediaPlayerService extends Service implements MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener {
    private final IBinder binder = new LocalBinder();
    MediaPlayer mediaPlayer = null;
    ScheduledExecutorService timer;
    CountDownTimer timeout;
    long remainingTimeout;
    SharedPreferences sharedPreferences;
    Uri mediaUri;
    String filename;

    Audiobook audiobook;
    DatabaseService databaseService;
    boolean databaseServiceBound;

    public class LocalBinder extends Binder {
        public MediaPlayerService getService() {
            // Return this instance of LocalService so clients can call public methods
            return MediaPlayerService.this;
        }
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        sharedPreferences = getSharedPreferences("sp", MODE_PRIVATE);
        bindDatabaseService();
        Bundle bundle = intent.getExtras();

        if (bundle != null) {
            audiobook = bundle.getParcelable("audiobook", Audiobook.class);
        }
        if (audiobook != null) {
            Uri uri = audiobook.getCurrentChapter().getPath();
            playMedia(uri);
        }
        return START_STICKY;
    }

    private void bindDatabaseService() {
        Intent intent = new Intent(getApplicationContext(), DatabaseService.class);
        startService(intent);
        bindService(intent, databaseConnection, Context.BIND_AUTO_CREATE);
    }

    public void playMedia(Uri uri) {
        if (mediaUri == null || !mediaUri.equals(uri)) {
            mediaUri = uri;
            if (mediaPlayer != null) releaseMediaPlayer();
            createMediaPlayer(mediaUri);
        }
    }

    public void playMedia(Audiobook audiobook) {
        if (Objects.equals(audiobook.getUid(), this.audiobook.getUid())) return;
        if (timeout != null) timeout.cancel();
        this.audiobook = audiobook;
        Uri uri = audiobook.getCurrentChapter().getPath();
        playMedia(uri);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return true;
    }

    /**
     * Called when MediaPlayer is ready
     */
    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {
        startMediaPlayer();
    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        if (playNextChapter()) return;
        releaseMediaPlayer();
        if (timeout != null) timeout.cancel();
        stopSelf();
    }

    @Override
    public void onDestroy() {
        onCompletion(mediaPlayer);
        unbindDatabaseService();
        super.onDestroy();
    }

    public void createMediaPlayer(Uri uri) {
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

            filename = getNameFromUri();
        } catch (IOException e) {
        }
    }

    private String getNameFromUri() {
        String file = "";
        if (mediaUri == null) return file;
        Cursor cursor =
                getContentResolver().query(mediaUri, null, null, null, null);
        if (cursor != null) {
            int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
            cursor.moveToFirst();
            file = cursor.getString(nameIndex);
            cursor.close();
        } else {
            String[] splittedUri = mediaUri.toString().split("/");
            file = splittedUri[splittedUri.length - 1];
        }
        file = file.replace(".mp3", "").replace(".wav", "");
        return file;
    }

    public void releaseMediaPlayer() {
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
            int position = mediaPlayer.getCurrentPosition() + 10000;
            mediaPlayer.seekTo(position);
        }
    }

    public void fastRewind() {
        if (mediaPlayer != null) {
            int position = Math.max(0, mediaPlayer.getCurrentPosition() - 10000);
            mediaPlayer.seekTo(position);
        }
    }

    public boolean playNextChapter() {
        if (audiobook != null && mediaPlayer != null) {
            Chapter chapter = audiobook.getNextChapter();
            if (playNewChapter(chapter)) {
                audiobook.setNextChapterAsCurrent();
                return true;
            }
        }
        return false;
    }

    public boolean playPreviousChapter() {
        if (audiobook != null && mediaPlayer != null) {
            Chapter chapter = audiobook.getPreviousChapter();
            if (playNewChapter(chapter)) {
                audiobook.setPreviousChapterAsCurrent();
                return true;
            }
        }
        return false;
    }

    private boolean playNewChapter(Chapter chapter) {
        if (chapter != null) {
            playMedia(chapter.getPath());
            return true;
        }
        return false;
    }

    public void seekMediaPlayer(int position) {
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

    public int getDuration() {
        if (mediaPlayer != null) {
            return mediaPlayer.getDuration();
        }
        return 0;
    }

    public int getCurrentPosition() {
        if (mediaPlayer != null) {
            return mediaPlayer.getCurrentPosition();
        } else {
            return 0;
        }
    }

    public String getFilename() {
        if (filename == null || filename.isEmpty()) {
            filename = getNameFromUri();
        }
        return filename;
    }

    public Chapter getCurrentChapter() {
        if (audiobook != null){
            return audiobook.getCurrentChapter();
        }
        return null;
    }

    public int getPercentage() {
        if (mediaPlayer != null) {
            return Math.round((float) (getCurrentPosition() * 100) / getDuration());
        } else {
            return 0;
        }
    }

    public void setPlaybackSpeed(float speed) {
        if (mediaPlayer != null) {
            mediaPlayer.setPlaybackParams(mediaPlayer.getPlaybackParams().setSpeed(speed));
            mediaPlayer.getPlaybackParams().getSpeed();
        }
    }

    public float getPlaybackSpeed() {
        if (mediaPlayer != null) {
            return mediaPlayer.getPlaybackParams().getSpeed();
        } else {
            return 1.0f;
        }
    }

    public void setTimeout(int minutes) {
        timeout = new CountDownTimer(minutes * 60000L, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                remainingTimeout = millisUntilFinished;
            }

            @Override
            public void onFinish() {
                if (mediaPlayer != null) {
                    if (mediaPlayer.isPlaying()) {
                        onCompletion(mediaPlayer);
                    }
                }
            }
        };
        timeout.start();
    }

    public long getRemainingTimeout() {
        return remainingTimeout;
    }

    public boolean timeoutSet() {
        return timeout != null;
    }

    public byte[] getCover() {
        if (audiobook != null) {
            return audiobook.getEmbeddedPicture();
        }
        return null;
    }

    private final ServiceConnection databaseConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            DatabaseService.LocalBinder binder = (DatabaseService.LocalBinder) service;
            databaseService = binder.getService();
            if (databaseService != null) {
                databaseServiceBound = true;
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            databaseServiceBound = false;
        }
    };

    private void unbindDatabaseService() {
        if (databaseService != null && databaseServiceBound) {
            unbindService(databaseConnection);
        }
    }
}
