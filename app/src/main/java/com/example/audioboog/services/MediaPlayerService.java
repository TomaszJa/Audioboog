package com.example.audioboog.services;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Binder;
import android.os.CountDownTimer;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.media3.common.MediaItem;
import androidx.media3.session.MediaController;
import androidx.media3.session.SessionToken;
import com.example.audioboog.source.Audiobook;
import com.example.audioboog.source.Chapter;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;

import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MediaPlayerService extends Service {
    private final IBinder binder = new LocalBinder();
    ScheduledExecutorService timer;
    ScheduledExecutorService databaseUpdater;
    float playbackSpeed;
    CountDownTimer timeout;
    long remainingTimeout;

    Audiobook audiobook;
    DatabaseService databaseService;
    boolean databaseServiceBound;
    ActionPlaying actionPlaying;

    MediaController mediaController;

    public class LocalBinder extends Binder {
        public MediaPlayerService getService() {
            // Return this instance of LocalService so clients can call public methods
            return MediaPlayerService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        bindDatabaseService();
        createMediaSession();
        playbackSpeed = 1.0f;
    }

    private void createMediaSession() {
        SessionToken sessionToken =
                new SessionToken(this, new ComponentName(this, PlaybackService.class));
        ListenableFuture<MediaController> controllerFuture =
                new MediaController.Builder(this, sessionToken).buildAsync();
        controllerFuture.addListener(() -> {
            onMediaControllerPrepared(controllerFuture);
        }, MoreExecutors.directExecutor());
    }

    private void onMediaControllerPrepared(ListenableFuture<MediaController> controllerFuture) {
        // Call controllerFuture.get() to retrieve the MediaController.
        // MediaController implements the Player interface, so it can be
        // attached to the PlayerView UI component.
        try {
            mediaController = controllerFuture.get();
            if (audiobook != null) {
                putAudiobookInPlayer();
                playAudiobook();
            }
        } catch (ExecutionException | InterruptedException ignored) {}

    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    public void setCallback(ActionPlaying actionPlaying) {
        this.actionPlaying = actionPlaying;
    }

    private void bindDatabaseService() {
        Intent intent = new Intent(getApplicationContext(), DatabaseService.class);
        startService(intent);
        bindService(intent, databaseConnection, Context.BIND_AUTO_CREATE);
    }

    public void playMedia(Audiobook audiobook) {
        if (this.audiobook == null || !Objects.equals(audiobook.getUid(), this.audiobook.getUid())) {
            this.audiobook = audiobook;
            if (mediaController != null) {
                mediaController.clearMediaItems();
                putAudiobookInPlayer();
                playAudiobook();
            }
        }
    }

    private void putAudiobookInPlayer() {
        for (Chapter chapter: audiobook.getChapters()) {
            Uri uri = chapter.getPath();
            MediaItem item = MediaItem.fromUri(uri);
            mediaController.addMediaItem(item);
        }
    }

    private void playAudiobook() {
        Chapter currentChapter = audiobook.getCurrentChapter();
        mediaController.seekTo(currentChapter.getChapterNumber()-1, currentChapter.getCurrentPosition());
        mediaController.setPlaybackSpeed(playbackSpeed);
        mediaController.prepare();
        mediaController.play();
        updateAudiobookInDatabase();
    }

    public void playSelectedChapter(String chapterUid) {
        if (this.audiobook == null) return;
        audiobook.setChapterByUid(chapterUid);
        playAudiobook();
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

    @Override
    public void onDestroy() {
        releaseMediaPlayer();
        unbindDatabaseService();
        stopSelf();
        super.onDestroy();
    }

    public Audiobook getCurrentAudiobook() {
        return audiobook;
    }

    public void releaseMediaPlayer() {
        stopUpdatingAudiobook();
        cancelTimeout();
        if (timer != null) timer.shutdown();
        cancelTimeout();
        audiobook = null;
    }

    public void cancelTimeout() {
        if (timeout != null) {
            timeout.cancel();
            timeout = null;
        }
    }

    public void playOrPause() {
        if (mediaController != null) {
            if (mediaController.isPlaying()) {
                pauseMediaPlayer();
            } else {
                startMediaPlayer();
            }
        }
    }

    public void startMediaPlayer() {
        mediaController.play();
    }

    public void pauseMediaPlayer() {
        mediaController.pause();
        stopSelf();
    }

    public void fastForward() {
        if (mediaController != null) {
            mediaController.seekForward();
//            int new_position = mediaPlayer.getCurrentPosition() + 10000;
//            if (new_position > mediaPlayer.getDuration()) {
//                new_position = getCurrentPosition() + 10000;
//                seekMediaPlayer(new_position);
//            } else {
//                mediaPlayer.seekTo(new_position);
//            }
        }
    }

    public void fastRewind() {
        if (mediaController != null) {
            mediaController.seekBack();
//            int new_position = mediaPlayer.getCurrentPosition() - 10000;
//            if (new_position < 0) {
//                new_position = Math.max(0, getCurrentPosition() - 10000);
//                seekMediaPlayer(new_position);
//            } else {
//                mediaPlayer.seekTo(new_position);
//            }
        }
    }

    public boolean playNextChapter() {
        if (audiobook != null && mediaController != null) {
            Chapter nextChapter = audiobook.getNextChapter();
            if (nextChapter != null && mediaController.hasNextMediaItem()) {
                mediaController.seekToNextMediaItem();
                nextChapter.setCurrentPosition(0);
                audiobook.setNextChapterAsCurrent();
                return true;
            }
        }
        return false;
    }

    public boolean playPreviousChapter() {
        if (audiobook != null && mediaController != null) {
            Chapter chapter = audiobook.getPreviousChapter();
            if (chapter != null && mediaController.hasPreviousMediaItem()) {
                mediaController.seekToPreviousMediaItem();
                chapter.setCurrentPosition(0);
                audiobook.setPreviousChapterAsCurrent();
                return true;
            }
        }
        return false;
    }

    public void seekMediaPlayer(int position) {
        if (mediaController != null) {
            if (position > audiobook.getTotalDuration()) position = (int)audiobook.getTotalDuration();
            if (position <= 0) position = 0;
            if (getCurrentChapter().positionOutOfChapterBounds(position)) {
                audiobook.setChapterByPosition(position);
                audiobook.getCurrentChapter().setCurrentPosition(position - audiobook.getCurrentChapter().getChapterStart());
                playAudiobook();
                return;
            }
            mediaController.seekTo(position - (int)audiobook.getCurrentChapter().getChapterStart());
        }
    }

    public boolean isPlaying() {
        if (mediaController != null) {
            return mediaController.isPlaying();
        }
        return false;
    }

    public int getDuration() {
        if (mediaController != null) {
            return (int)audiobook.getTotalDuration();
        }
        return 0;
    }

    public int getCurrentPosition() {
        if (mediaController != null) {
            long chapterStart = getCurrentChapter().getChapterStart();
            long currentPosition = mediaController.getCurrentPosition();
            return (int)(chapterStart + currentPosition);
        } else {
            return 0;
        }
    }

    public Chapter getCurrentChapter() {
        if (audiobook != null){
            return audiobook.getCurrentChapter();
        }
        return null;
    }

    public void setPlaybackSpeed(float speed) {
        if (mediaController != null) {
            mediaController.setPlaybackSpeed(speed);
            playbackSpeed = speed;
        }
    }

    public float getPlaybackSpeed() {
        if (mediaController != null) {
            return mediaController.getPlaybackParameters().speed;
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
                if (mediaController != null) {
                    if (mediaController.isPlaying()) {
                        mediaController.pause();
                    }
                }
            }
        };
        timeout.start();
    }

    public int getTimeToTheEndOfChapter() {
        if (mediaController != null) {
            return (int)(mediaController.getDuration() - mediaController.getCurrentPosition());
        }
        return 0;
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
    private void updateAudiobookInDatabase() {
        databaseUpdater = Executors.newScheduledThreadPool(1);
        databaseUpdater.scheduleWithFixedDelay(() -> {
            if (databaseServiceBound && isPlaying() && audiobook != null) {
                int currentPosition = getCurrentPosition();
                audiobook.setCurrentPosition(currentPosition);
                audiobook.getCurrentChapter().setCurrentPosition(mediaController.getCurrentPosition());
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
}
