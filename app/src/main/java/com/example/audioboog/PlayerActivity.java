package com.example.audioboog;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.IBinder;
import android.view.MenuItem;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.example.audioboog.dialogs.OptionsPicker;
import com.example.audioboog.services.ActionPlaying;
import com.example.audioboog.services.MediaPlayerService;
import com.example.audioboog.source.ChaptersCollection;
import com.example.audioboog.source.PlaybackSpeed;
import com.example.audioboog.source.Timeout;
import com.example.audioboog.utils.Utils;
import com.google.android.material.navigation.NavigationView;

import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class PlayerActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener, ActionPlaying {
    private DrawerLayout playerDrawerLayout;
    NavigationView navigationView;
    Toolbar toolbar;

    ImageButton playButton, previousButton, nextButton, fastForwardButton, fastRewindButton,
            playbackSpeedButton, timeoutButton;
    TextView chapterNameTxt, startTxt, stopTxt, percentageTxt, playbackSpeedTxt, timeoutDurationTxt, chapterTimeoutTxt;
    SeekBar seekBar;
    ScheduledExecutorService seekbarTimer;

    String chapterName;
    ImageView bookCoverImgView;

    MediaPlayerService mediaPlayerService;
    boolean mediaServiceBound;

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

        InitializeGuiElements();
        initializeSeekBar();
        setClickListeners();
    }

    private void setClickListeners() {
        chapterNameTxt.setOnClickListener(v -> pickChapter());
        chapterTimeoutTxt.setOnClickListener(v -> pickChapter());
        playButton.setOnClickListener(v -> {
            if (mediaServiceBound) {
                mediaPlayerService.playOrPause();
                setUiPlayingState();
            }
        });
        nextButton.setOnClickListener(v -> {
            if (mediaServiceBound) {
                setGuiMediaPaused();
                if (mediaPlayerService.playNextChapter()) {
                    setUiForNewAudio();
                    setGuiMediaPlaying();
                }
            }
        });
        previousButton.setOnClickListener(v -> {
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

        fastForwardButton.setOnClickListener(v -> {
            if (mediaServiceBound) {
                mediaPlayerService.fastForward();
            }
        });

        fastRewindButton.setOnClickListener(v -> {
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
        playButton.setImageResource(R.drawable.ic_play);
        pauseSeekBar();
    }

    private void setGuiMediaPlaying() {
        playButton.setImageResource(R.drawable.ic_pause);
        startSeekBar();
    }

    private void initializeSeekBar() {
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (!mediaServiceBound) return;
                startTxt.setText(Utils.convertPlayingTimeToString(progress));
                setPercentageProgress(seekBar, progress);
                calculateTimeout();
                setChapterInfo();
            }

            private void setChapterInfo() {
                if (chapterName != null && !chapterName.equals(mediaPlayerService.getCurrentChapter().getName())) setUiForNewAudio();
                String chapterTimeoutText = "Ends in: " + Utils.convertPlayingTimeToString(mediaPlayerService.getTimeToTheEndOfChapter());
                chapterTimeoutTxt.setText(chapterTimeoutText);
            }

            private void calculateTimeout() {
                if (mediaPlayerService.timeoutSet()) {
                    timeoutDurationTxt.setText(Utils.convertPlayingTimeToString((int)mediaPlayerService.getRemainingTimeout()));
                }
            }

            private void setPercentageProgress(SeekBar seekBar, long progress) {
                try {
                    String percentage = progress * 100 / (long) seekBar.getMax() + "%";
                    percentageTxt.setText(percentage);
                } catch (ArithmeticException ignored) {}
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
        }, 10, 300, TimeUnit.MILLISECONDS);
    }

    private void setUiForNewAudio() {
        if (!mediaServiceBound) return;
        int duration = mediaPlayerService.getDuration();
        seekBar.setMax(duration);
        stopTxt.setText(Utils.convertPlayingTimeToString(duration));
        chapterName = mediaPlayerService.getCurrentChapter().getName();
        chapterNameTxt.setText(chapterName);
        byte[] coverImage = mediaPlayerService.getCover();
        if (coverImage != null) {
            bookCoverImgView.setImageBitmap(BitmapFactory.decodeByteArray(coverImage, 0, coverImage.length));
        }
        String playbackSpeed = mediaPlayerService.getPlaybackSpeed() + "x";
        playbackSpeedTxt.setText(playbackSpeed);
        if (mediaPlayerService.timeoutSet()) {
            timeoutDurationTxt.setText(Utils.convertPlayingTimeToString((int)mediaPlayerService.getRemainingTimeout()));
        }
    }

    private void pauseSeekBar() {
        if (seekbarTimer != null) {
            seekbarTimer.shutdown();
            try {
                if (!seekbarTimer.isShutdown()) {
                    boolean shutdown = true;
                    while (shutdown) shutdown = !seekbarTimer.awaitTermination(1, TimeUnit.SECONDS);
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void InitializeGuiElements() {
        playButton = findViewById(R.id.playButton);
        previousButton = findViewById(R.id.previous_button);
        nextButton = findViewById(R.id.next_button);
        fastForwardButton = findViewById(R.id.fast_forward_button);
        fastRewindButton = findViewById(R.id.fast_rewind_button);
        playbackSpeedButton = findViewById(R.id.playback_speed_button);
        timeoutButton = findViewById(R.id.timeoutButton);
        chapterNameTxt = findViewById(R.id.txtsn);
        startTxt = findViewById(R.id.txtsstart);
        percentageTxt = findViewById(R.id.text_percentage);
        stopTxt = findViewById(R.id.txtsstop);
        seekBar = findViewById(R.id.seekbar);
        playbackSpeedTxt = findViewById(R.id.playbackSpeedText);
        timeoutDurationTxt = findViewById(R.id.timeoutDuration);
        bookCoverImgView = findViewById(R.id.imageview);
        chapterTimeoutTxt = findViewById(R.id.chapterTimeoutText);
        toolbar = findViewById(R.id.player_toolbar);
        playerDrawerLayout = findViewById(R.id.player_drawer_layout);
        navigationView = findViewById(R.id.player_nav_view);

        chapterNameTxt.setSelected(true);
        setSupportActionBar(toolbar);
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
                playbackSpeedTxt.setText(speed.getPlaybackString());
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
                chapterNameTxt.setText(chapter.getChapter().getName());
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
                timeoutDurationTxt.setText(Utils.convertPlayingTimeToString(timeout.getValue()*60000));
                pickTimeout.dismiss();
            }
        });
        pickTimeout.setCancelListener(v -> {
            if (!mediaServiceBound) return;
            mediaPlayerService.cancelTimeout();
            timeoutDurationTxt.setText(Utils.convertPlayingTimeToString((0)));
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
}