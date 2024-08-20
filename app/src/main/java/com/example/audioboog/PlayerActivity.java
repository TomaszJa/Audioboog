package com.example.audioboog;

import static com.example.audioboog.services.ApplicationClass.ACTION_FORWARD;
import static com.example.audioboog.services.ApplicationClass.ACTION_PLAY;
import static com.example.audioboog.services.ApplicationClass.ACTION_REVERT;
import static com.example.audioboog.services.ApplicationClass.CHANNEL_ID_2;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.media.session.MediaSessionCompat;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.NotificationCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.example.audioboog.dialogs.OptionsPicker;
import com.example.audioboog.services.ActionPlaying;
import com.example.audioboog.services.MediaPlayerService;
import com.example.audioboog.services.NotificationReceiver;
import com.example.audioboog.source.Audiobook;
import com.example.audioboog.source.Chapter;
import com.example.audioboog.source.ChaptersCollection;
import com.example.audioboog.source.PlaybackSpeed;
import com.example.audioboog.source.Timeout;
import com.google.android.material.navigation.NavigationView;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class PlayerActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener, ActionPlaying {
    private DrawerLayout playerDrawerLayout;
    NavigationView navigationView;
    Toolbar toolbar;

    ImageButton play_button, previous_button, next_button, fast_forward_button, fast_rewind_button,
            playbackSpeedButton, timeoutButton;
    TextView txtsname, txtsstart, txtsstop, txtPercentage, playbackSpeedText, timeoutDuration, chapterTimeout;
    SeekBar seekBar;
    ScheduledExecutorService seekbarTimer;

    String songName;
    ImageView imageView;

    MediaPlayerService mediaPlayerService;
    SharedPreferences sharedPreferences;
    boolean mediaServiceBound;
    MediaSessionCompat mediaSession;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_player);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.player_drawer_layout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        mediaSession = new MediaSessionCompat(this, "PlayerAudio");

        InitializeGuiElements();
        txtsname.setSelected(true);

        sharedPreferences = getSharedPreferences("sp", MODE_PRIVATE);
        initializeSeekBar();

        txtsname.setOnClickListener(v -> pickChapter());
        chapterTimeout.setOnClickListener(v -> pickChapter());

        play_button.setOnClickListener(v -> {
            if (mediaServiceBound) {
                mediaPlayerService.playOrPause();
                setUiPlayingState();
            }
        });
        next_button.setOnClickListener(v -> {
            if (mediaServiceBound) {
                setGuiMediaPaused();
                if (mediaPlayerService.playNextChapter()) {
                    setUiForNewAudio();
                    setGuiMediaPlaying();
                }
            }

        });
        previous_button.setOnClickListener(v -> {
            if (mediaServiceBound) {
                setGuiMediaPaused();
                if (mediaPlayerService.playPreviousChapter()) {
                    setUiForNewAudio();
                    setGuiMediaPlaying();
                }
            }
        });
        playbackSpeedButton.setOnClickListener(v -> {
            if (mediaServiceBound) {
                pickPlaybackSpeed();
            }
        });
        timeoutButton.setOnClickListener(v -> {
            if (mediaServiceBound) {
                pickTimeoutDuration();
            }
        });

        fast_forward_button.setOnClickListener(v -> {
            if (mediaServiceBound) {
                mediaPlayerService.fastForward();
            }
        });

        fast_rewind_button.setOnClickListener(v -> {
            if (mediaServiceBound) {
                mediaPlayerService.fastRewind();
            }
        });
    }

    private void setUiPlayingState() {
        if (mediaPlayerService.isPlaying()) {
            setGuiMediaPlaying();
        } else {
            setGuiMediaPaused();
        }
    }

    private void setGuiMediaPaused() {
        play_button.setImageResource(R.drawable.ic_play);
        showNotification(R.drawable.ic_play);
        pauseSeekBar();
    }

    private void setGuiMediaPlaying() {
        play_button.setImageResource(R.drawable.ic_pause);
        showNotification(R.drawable.ic_pause);
        startSeekBar();
    }

    private void initializeSeekBar() {
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (!mediaServiceBound) return;
                txtsstart.setText(convertPlayingTimeToString(progress));
                try {
                    String percentage = (long) progress * 100 / (long) seekBar.getMax() + "%";
                    txtPercentage.setText(percentage);
                } catch (ArithmeticException ignored) {}
                if (mediaPlayerService.timeoutSet()) {
                    timeoutDuration.setText(convertPlayingTimeToString((int)mediaPlayerService.getRemainingTimeout()));
                }
                if (songName != null && !songName.equals(mediaPlayerService.getCurrentChapter().getName())) setUiForNewAudio();
                String chapterTimeoutText = "Ends in: " + convertPlayingTimeToString(mediaPlayerService.getTimeToTheEndOfChapter());
                chapterTimeout.setText(chapterTimeoutText);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                if (!mediaServiceBound) return;
                setGuiMediaPaused();
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (!mediaServiceBound) return;
                mediaPlayerService.seekMediaPlayer(seekBar.getProgress());
                setGuiMediaPlaying();
            }
        });
    }

    private void startSeekBar() {
        seekbarTimer = Executors.newScheduledThreadPool(1);
        seekbarTimer.scheduleWithFixedDelay(() -> {
            if (mediaServiceBound) {
                if (!seekBar.isPressed()) {
                    seekBar.setProgress(mediaPlayerService.getCurrentPosition());
                }
            }
        }, 10, 1000, TimeUnit.MILLISECONDS);
    }

    private String convertPlayingTimeToString(int milliseconds) {
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

    private void setUiForNewAudio() {
        if (!mediaServiceBound) return;
        int duration = mediaPlayerService.getDuration();
        seekBar.setMax(duration);
        txtsstop.setText(convertPlayingTimeToString(duration));
        songName = mediaPlayerService.getCurrentChapter().getName();
        txtsname.setText(songName);
        byte[] coverImage = mediaPlayerService.getCover();
        if (coverImage != null) {
            imageView.setImageBitmap(BitmapFactory.decodeByteArray(coverImage, 0, coverImage.length));
        }
        String playbackSpeed = mediaPlayerService.getPlaybackSpeed() + "x";
        playbackSpeedText.setText(playbackSpeed);
        if (mediaPlayerService.timeoutSet()) {
            timeoutDuration.setText(convertPlayingTimeToString((int)mediaPlayerService.getRemainingTimeout()));
        }
    }

    private void pauseSeekBar() {
        if (seekbarTimer != null) {
            seekbarTimer.shutdown();
            try {
                if (!seekbarTimer.isShutdown()) while (!seekbarTimer.awaitTermination(1, TimeUnit.SECONDS)) ;
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void InitializeGuiElements() {
        play_button = findViewById(R.id.playButton);
        previous_button = findViewById(R.id.previous_button);
        next_button = findViewById(R.id.next_button);
        fast_forward_button = findViewById(R.id.fast_forward_button);
        fast_rewind_button = findViewById(R.id.fast_rewind_button);
        playbackSpeedButton = findViewById(R.id.playback_speed_button);
        timeoutButton = findViewById(R.id.timeoutButton);
        txtsname = findViewById(R.id.txtsn);
        txtsstart = findViewById(R.id.txtsstart);
        txtPercentage = findViewById(R.id.text_percentage);
        txtsstop = findViewById(R.id.txtsstop);
        seekBar = findViewById(R.id.seekbar);
        playbackSpeedText = findViewById(R.id.playbackSpeedText);
        timeoutDuration = findViewById(R.id.timeoutDuration);
        imageView = findViewById(R.id.imageview);
        chapterTimeout = findViewById(R.id.chapterTimeoutText);

        toolbar = findViewById(R.id.player_toolbar);
        setSupportActionBar(toolbar);
        playerDrawerLayout = findViewById(R.id.player_drawer_layout);
        navigationView = findViewById(R.id.player_nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, playerDrawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        playerDrawerLayout.addDrawerListener(toggle);
        toggle.syncState();
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
        int itemId = menuItem.getItemId();
        if (itemId == R.id.navLibrary) {
            getOnBackPressedDispatcher().onBackPressed();
        } else if (itemId == R.id.navCurrentBook) {
            setUiForNewAudio();
        }
        playerDrawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    private void pickPlaybackSpeed()
    {
        PlaybackSpeed.initPlaybackSpeeds();
        ArrayList<PlaybackSpeed> playbackValues = PlaybackSpeed.getPlaybackSpeeds();
        final OptionsPicker pickPlaybackSpeed = new OptionsPicker(PlayerActivity.this, PlaybackSpeed.playbackValues(), "Playback Speed");

        PlaybackSpeed currentPlaybackSpeed = PlaybackSpeed.getByValue(mediaPlayerService.getPlaybackSpeed());
        if (currentPlaybackSpeed != null) pickPlaybackSpeed.setDefaultValue(currentPlaybackSpeed.getId());
        pickPlaybackSpeed.setValueSetListener(v -> {
            PlaybackSpeed speed = playbackValues.get(pickPlaybackSpeed.getPickedValue());
            if (mediaServiceBound) {
                mediaPlayerService.setPlaybackSpeed(speed.getValue());
                playbackSpeedText.setText(speed.getPlaybackString());
                pickPlaybackSpeed.dismiss();
            }
        });
        pickPlaybackSpeed.show();
    }

    private void pickChapter()
    {
        if (!mediaServiceBound) return;
        ChaptersCollection.initChaptersCollection(mediaPlayerService.getCurrentAudiobook().getChapters());
        ArrayList<ChaptersCollection> chaptersValues = ChaptersCollection.getChaptersCollections();
        final OptionsPicker pickChapter = new OptionsPicker(PlayerActivity.this, ChaptersCollection.chaptersNumbers(), "Pick Chapter");

        ChaptersCollection currentChapter = ChaptersCollection.getByUid(mediaPlayerService.getCurrentChapter().getUid());
        if (currentChapter != null) pickChapter.setDefaultValue(currentChapter.getId());
        pickChapter.setValueSetListener(v -> {
            ChaptersCollection chapter = chaptersValues.get(pickChapter.getPickedValue());
            if (mediaServiceBound) {
                setGuiMediaPaused();
                mediaPlayerService.playSelectedChapter(chapter.getChapter().getUid());
                txtsname.setText(chapter.getChapter().getName());
                setGuiMediaPlaying();
                pickChapter.dismiss();
            }
        });
        pickChapter.show();
    }

    private void pickTimeoutDuration()
    {
        Timeout.initTimeouts();
        ArrayList<Timeout> timeoutValues = Timeout.getTimeouts();
        final OptionsPicker pickTimeout = new OptionsPicker(PlayerActivity.this, Timeout.timeoutValues(), "Timeout");

        pickTimeout.setValueSetListener(v -> {
            Timeout timeout = timeoutValues.get(pickTimeout.getPickedValue());
            if (mediaServiceBound) {
                mediaPlayerService.setTimeout(timeout.getValue());
                timeoutDuration.setText(convertPlayingTimeToString(timeout.getValue()*60000));
                pickTimeout.dismiss();
            }
        });
        pickTimeout.setCancelListener(v -> {
            if (!mediaServiceBound) return;
            mediaPlayerService.cancelTimeout();
            timeoutDuration.setText(convertPlayingTimeToString((0)));
        });
        pickTimeout.show();
    }

    private final ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MediaPlayerService.LocalBinder binder = (MediaPlayerService.LocalBinder) service;
            mediaPlayerService = binder.getService();
            if (mediaPlayerService != null) {
                mediaServiceBound = true;
                setUiForNewAudio();
                if (seekbarTimer == null || seekbarTimer.isShutdown()) startSeekBar();
                mediaPlayerService.setCallback(PlayerActivity.this);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mediaServiceBound = false;
        }
    };

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onStop() {
        super.onStop();
        pauseSeekBar();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayerService != null) {
            if (mediaServiceBound) {
                unbindService(connection);
                mediaServiceBound = false;
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!mediaServiceBound) {
            bindMediaPlayerService();

        } else if (mediaPlayerService != null) {
            if (seekbarTimer == null || seekbarTimer.isShutdown()) startSeekBar();
        }
    }

    private void bindMediaPlayerService() {
        Intent intent = new Intent(PlayerActivity.this, MediaPlayerService.class);
        bindService(intent, connection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void forwardClicked() {
        if (!mediaServiceBound) return;
        mediaPlayerService.fastForward();
    }

    @Override
    public void rewindClicked() {
        if (!mediaServiceBound) return;
        mediaPlayerService.fastRewind();
    }

    @Override
    public void playClicked() {
        if (!mediaServiceBound) return;
        mediaPlayerService.playOrPause();
        setUiPlayingState();
    }

    public void showNotification(int playPauseBtn) {
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        Intent rewindIntent = new Intent(this, NotificationReceiver.class).setAction(ACTION_REVERT);
        PendingIntent rewindPendingIntent = PendingIntent.getBroadcast(this, 0, rewindIntent, PendingIntent.FLAG_IMMUTABLE);

        Intent playIntent = new Intent(this, NotificationReceiver.class).setAction(ACTION_PLAY);
        PendingIntent playPendingIntent = PendingIntent.getBroadcast(this, 0, playIntent, PendingIntent.FLAG_IMMUTABLE);

        Intent forwardIntent = new Intent(this, NotificationReceiver.class).setAction(ACTION_FORWARD);
        PendingIntent forwardPendingIntent = PendingIntent.getBroadcast(this, 0, forwardIntent, PendingIntent.FLAG_IMMUTABLE);

        Audiobook audiobook = mediaPlayerService.getCurrentAudiobook();

        byte[] art = audiobook.getEmbeddedPicture();
        Bitmap image = BitmapFactory.decodeByteArray(art, 0, art.length);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID_2)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setLargeIcon(image)
                .setContentTitle(audiobook.getName())
                .setContentText(audiobook.getCurrentChapter().getName())
                .addAction(R.drawable.ic_replay_10, "fast_rewind", rewindPendingIntent)
                .addAction(playPauseBtn, "play", playPendingIntent)
                .addAction(R.drawable.ic_forward_10, "fast_forward", forwardPendingIntent)
                .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                        .setMediaSession(mediaSession.getSessionToken()))
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setContentIntent(contentIntent)
                .setOnlyAlertOnce(true)
                .build();

        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.notify(0, notification);
    }
}