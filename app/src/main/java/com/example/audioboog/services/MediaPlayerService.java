package com.example.audioboog.services;

import static com.example.audioboog.services.ApplicationClass.ACTION_FORWARD;
import static com.example.audioboog.services.ApplicationClass.ACTION_PLAY;
import static com.example.audioboog.services.ApplicationClass.ACTION_REVERT;
import static com.example.audioboog.services.ApplicationClass.CHANNEL_ID_2;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.support.v4.media.session.MediaSessionCompat;
import android.view.KeyEvent;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.media.session.MediaButtonReceiver;

import com.example.audioboog.PlayerActivity;
import com.example.audioboog.R;
import com.example.audioboog.source.Audiobook;
import com.example.audioboog.source.Chapter;
import com.example.audioboog.utils.Utils;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MediaPlayerService extends Service implements MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener {
    private final IBinder binder = new LocalBinder();
    MediaPlayer mediaPlayer = null;
    ScheduledExecutorService timer;
    ScheduledExecutorService databaseUpdater;
    float playbackSpeed;
    CountDownTimer timeout;
    long remainingTimeout;
    Uri mediaUri;

    Audiobook audiobook;
    DatabaseService databaseService;
    boolean databaseServiceBound;
    ActionPlaying actionPlaying;

    MediaSessionCompat mediaSession;
    AudioManager audioManager;
    AudioAttributes playbackAttributes;
    AudioManager.OnAudioFocusChangeListener audioFocusChangeListener = focusChange -> {
        if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
            if (isPlaying()) startMediaPlayer();
        } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
            if (isPlaying()) pauseMediaPlayer();
        } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
            if (isPlaying()) {
                pauseMediaPlayer();
            }
        }
    };
    int audioFocusRequest;

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
        mediaSession = new MediaSessionCompat(this, "MediaPlayerService");
        mediaSession.setActive(true);
        mediaSession.setCallback(new MediaSessionCompat.Callback() {
            @Override
            public boolean onMediaButtonEvent(Intent mediaButtonEvent) {
                KeyEvent keyEvent = MediaButtonReceiver.handleIntent(mediaSession, mediaButtonEvent);
                switch (keyEvent.getKeyCode()) {
                    case KeyEvent.KEYCODE_MEDIA_PAUSE:
                        pauseMediaPlayer();
                        break;
                    case KeyEvent.KEYCODE_MEDIA_PLAY:
                        startMediaPlayer();
                        break;
                }
                return super.onMediaButtonEvent(mediaButtonEvent);
            }
        });
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent.getStringExtra("myActionName");
        if (action != null) {
            switch (action) {
                case ACTION_FORWARD:
                    actionPlaying.forwardClicked();
                    break;
                case ACTION_REVERT:
                    actionPlaying.rewindClicked();
                    break;
                case ACTION_PLAY:
                    actionPlaying.playClicked();
                    if (isPlaying()) {
                        showNotification(R.drawable.ic_pause);
                    } else {
                        showNotification(R.drawable.ic_play);
                    }
                    break;
            }
        }
        return START_STICKY;
    }

    public void showNotification(int playPauseBtn) {
        Intent intent = new Intent(this, PlayerActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        Intent rewindIntent = new Intent(this, NotificationReceiver.class).setAction(ACTION_REVERT);
        PendingIntent rewindPendingIntent = PendingIntent.getBroadcast(this, 0, rewindIntent, PendingIntent.FLAG_IMMUTABLE);

        Intent playIntent = new Intent(this, NotificationReceiver.class).setAction(ACTION_PLAY);
        PendingIntent playPendingIntent = PendingIntent.getBroadcast(this, 0, playIntent, PendingIntent.FLAG_IMMUTABLE);

        Intent forwardIntent = new Intent(this, NotificationReceiver.class).setAction(ACTION_FORWARD);
        PendingIntent forwardPendingIntent = PendingIntent.getBroadcast(this, 0, forwardIntent, PendingIntent.FLAG_IMMUTABLE);

        Bitmap image = Utils.convertEmbeddedPictureToBitmap(this, audiobook.getEmbeddedPicture());

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID_2)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setLargeIcon(image)
                .setContentTitle(audiobook.getName())
                .setSilent(true)
                .setAllowSystemGeneratedContextualActions(false)
                .setContentText(audiobook.getCurrentChapter().getName())
                .addAction(R.drawable.ic_replay_10, "fast_rewind", rewindPendingIntent)
                .addAction(playPauseBtn, "play", playPendingIntent)
                .addAction(R.drawable.ic_forward_10, "fast_forward", forwardPendingIntent)
                .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                        .setMediaSession(mediaSession.getSessionToken()))
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setContentIntent(contentIntent)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .build();
        startForeground(1, notification);
    }

    private void getAudioFocus() {
        AudioFocusRequest focusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(playbackAttributes)
                .setAcceptsDelayedFocusGain(true)
                .setOnAudioFocusChangeListener(audioFocusChangeListener)
                .build();
        audioFocusRequest = audioManager.requestAudioFocus(focusRequest);
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
            Uri uri = audiobook.getCurrentChapter().getPath();
            playMedia(uri);
        }
    }

    public void playSelectedChapter(String chapterUid) {
        if (this.audiobook == null) return;
        audiobook.setChapterByUid(chapterUid);
        Uri uri = audiobook.getCurrentChapter().getPath();
        playMedia(uri);
    }

    public void playMedia(Uri uri) {
        if (mediaUri == null || !mediaUri.equals(uri)) {
            mediaUri = uri;
            if (mediaPlayer != null) {
                setNewMedia(uri);
            }
            else {
                createMediaPlayer(mediaUri);
            }
            updateAudiobookInDatabase();
            setPlaybackSpeed(playbackSpeed);
        }
    }

    private void setNewMedia(Uri uri) {
        try {
            mediaPlayer.stop();
            mediaPlayer.reset();
            mediaPlayer.setDataSource(this, uri);
            mediaPlayer.prepare();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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
        mediaPlayer.seekTo((int)audiobook.getCurrentChapter().getCurrentPosition());
    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        if (!playNextChapter()) {
            cancelTimeout();
            stopForeground(Service.STOP_FOREGROUND_REMOVE);
            stopSelf();
        }
    }

    @Override
    public void onDestroy() {
        releaseMediaPlayer();
        unbindDatabaseService();
        stopSelf();
        mediaSession.release();
        super.onDestroy();
    }

    public void createMediaPlayer(Uri uri) {
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        playbackAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build();
        getAudioFocus();

        mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioAttributes(playbackAttributes);
        try {
            mediaPlayer.setDataSource(getApplicationContext(), uri);
            mediaPlayer.setOnPreparedListener(this);
            mediaPlayer.setOnCompletionListener(this);
            mediaPlayer.prepare();
        } catch (IOException ignored) {
        }
    }

    public Audiobook getCurrentAudiobook() {
        return audiobook;
    }

    public void releaseMediaPlayer() {
        stopUpdatingAudiobook();
        if (timer != null) timer.shutdown();
        cancelTimeout();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        audiobook = null;
    }

    public void cancelTimeout() {
        if (timeout != null) {
            timeout.cancel();
            timeout = null;
        }
    }

    public void playOrPause() {
        if (mediaPlayer != null) {
            getAudioFocus();
            if (mediaPlayer.isPlaying()) {
                pauseMediaPlayer();
            } else {
                startMediaPlayer();
            }
        }
    }

    public void startMediaPlayer() {
        mediaPlayer.start();
        showNotification(R.drawable.ic_pause);
    }

    public void pauseMediaPlayer() {
        mediaPlayer.pause();
        showNotification(R.drawable.ic_play);
        stopSelf();
    }

    public void fastForward() {
        if (mediaPlayer != null) {
            int new_position = mediaPlayer.getCurrentPosition() + 10000;
            if (new_position > mediaPlayer.getDuration()) {
                new_position = getCurrentPosition() + 10000;
                seekMediaPlayer(new_position);
            } else {
                mediaPlayer.seekTo(new_position);
            }
        }
    }

    public void fastRewind() {
        if (mediaPlayer != null) {
            int new_position = mediaPlayer.getCurrentPosition() - 10000;
            if (new_position < 0) {
                new_position = Math.max(0, getCurrentPosition() - 10000);
                seekMediaPlayer(new_position);
            } else {
                mediaPlayer.seekTo(new_position);
            }
        }
    }

    public boolean playNextChapter() {
        mediaPlayer.pause();
        Chapter nextChapter = audiobook.getNextChapter();
        if (audiobook != null && mediaPlayer != null && nextChapter != null) {
            nextChapter.setCurrentPosition(0);
            playMedia(nextChapter.getPath());
            audiobook.setNextChapterAsCurrent();
            return true;
        }
        return false;
    }

    public boolean playPreviousChapter() {
        mediaPlayer.pause();
        if (audiobook != null && mediaPlayer != null) {
            Chapter chapter = audiobook.getPreviousChapter();
            if (chapter != null) {
                chapter.setCurrentPosition(0);
                playMedia(chapter.getPath());
                audiobook.setPreviousChapterAsCurrent();
                return true;
            }
        }
        return false;
    }

    public void seekMediaPlayer(int position) {
        if (mediaPlayer != null) {
            if (position > audiobook.getTotalDuration()) position = (int)audiobook.getTotalDuration();
            if (position <= 0) position = 0;
            if (getCurrentChapter().positionOutOfChapterBounds(position)) {
                audiobook.setChapterByPosition(position);
                audiobook.getCurrentChapter().setCurrentPosition(position - audiobook.getCurrentChapter().getChapterStart());
                playMedia(audiobook.getCurrentChapter().getPath());
                return;
            }
            mediaPlayer.seekTo(position - (int)audiobook.getCurrentChapter().getChapterStart());
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
            return (int)audiobook.getTotalDuration();
        }
        return 0;
    }

    public int getCurrentPosition() {
        if (mediaPlayer != null) {
            return (int)getCurrentChapter().getChapterStart() + mediaPlayer.getCurrentPosition();
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
        if (mediaPlayer != null) {
            mediaPlayer.setPlaybackParams(mediaPlayer.getPlaybackParams().setSpeed(speed));
            playbackSpeed = speed;
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
                        mediaPlayer.pause();
                    }
                }
            }
        };
        timeout.start();
    }

    public int getTimeToTheEndOfChapter() {
        if (mediaPlayer != null) {
            return mediaPlayer.getDuration() - mediaPlayer.getCurrentPosition();
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
                audiobook.getCurrentChapter().setCurrentPosition(mediaPlayer.getCurrentPosition());
                databaseService.updateAudiobookInDatabase(audiobook);
            }
        }, 10, 10, TimeUnit.SECONDS);
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
