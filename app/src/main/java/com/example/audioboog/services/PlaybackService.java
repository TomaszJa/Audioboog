package com.example.audioboog.services;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.IBinder;

import androidx.annotation.NonNull;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.session.MediaSession;
import androidx.media3.session.MediaSessionService;

import com.example.audioboog.source.Audiobook;
import com.example.audioboog.source.Chapter;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class PlaybackService extends MediaSessionService {
    private MediaSession mediaSession = null;

    ScheduledExecutorService databaseUpdater;
    Audiobook audiobook;
    DatabaseService databaseService;
    boolean databaseServiceBound;

    @Override
    public void onCreate() {
        super.onCreate();
        bindDatabaseService();
        androidx.media3.common.AudioAttributes playbackAttributes = new androidx.media3.common.AudioAttributes.Builder().
                setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .build();
        ExoPlayer exoPlayer = new ExoPlayer.Builder(this)
                .setAudioAttributes(playbackAttributes, true)
                .build();
        mediaSession = new MediaSession.Builder(this, exoPlayer).setCallback(new MediaSession.Callback() {
            @NonNull
            @Override
            public ListenableFuture<List<MediaItem>> onAddMediaItems(@NonNull MediaSession mediaSession, @NonNull MediaSession.ControllerInfo controller, @NonNull List<MediaItem> mediaItems) {
                if (!mediaItems.isEmpty()) {
                    Bundle extras = mediaItems.get(0).mediaMetadata.extras;
                    if (extras != null) {
                        String audiobookuid = extras.getString("audiobook-uid");
                        if (databaseServiceBound) {
                            audiobook = databaseService.getAudiobook(audiobookuid);
                        }
                    }
                }
                return MediaSession.Callback.super.onAddMediaItems(mediaSession, controller, mediaItems);
            }
        }).build();
    }

    @Override
    public MediaSession onGetSession(@NonNull MediaSession.ControllerInfo controllerInfo) {
        // This example always accepts the connection request
        return mediaSession;
    }

    @Override
    public void onDestroy() {
        unbindDatabaseService();
        mediaSession.getPlayer().release();
        mediaSession.release();
        mediaSession = null;
        super.onDestroy();
    }

    private void bindDatabaseService() {
        Intent intent = new Intent(getApplicationContext(), DatabaseService.class);
        startService(intent);
        bindService(intent, databaseConnection, Context.BIND_AUTO_CREATE);
    }

    private final ServiceConnection databaseConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            DatabaseService.LocalBinder binder = (DatabaseService.LocalBinder) service;
            databaseService = binder.getService();
            if (databaseService != null) {
                databaseServiceBound = true;
                updateAudiobookInDatabase();
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
    private void updateAudiobookInDatabase() {
        databaseUpdater = Executors.newScheduledThreadPool(1);
        databaseUpdater.scheduleWithFixedDelay(() -> {
            if (databaseServiceBound && mediaSession.getPlayer().isPlaying() && audiobook != null) {
//                int currentPosition = getCurrentPosition();
//                audiobook.setCurrentPosition(currentPosition);
                audiobook.getCurrentChapter().setCurrentPosition(mediaSession.getPlayer().getCurrentPosition());
                databaseService.updateAudiobookInDatabase(audiobook);
            }
        }, 1, 1, TimeUnit.SECONDS);
    }

    private void stopUpdatingAudiobook() {
        if (databaseUpdater != null) {
            databaseUpdater.shutdown();
            try {
                if (!databaseUpdater.isShutdown()) {
                    boolean shutdown = true;
                    while (shutdown) shutdown = !databaseUpdater.awaitTermination(1, TimeUnit.SECONDS);
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void playAudiobook(Audiobook audiobook) {
        Player player = mediaSession.getPlayer();
        for (Chapter chapter: audiobook.getChapters()) {
            Uri uri = chapter.getPath();
            MediaItem item = MediaItem.fromUri(uri);
            player.addMediaItem(item);
        }
        player.prepare();
        player.play();
    }
}
