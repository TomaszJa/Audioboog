package com.example.audioboog;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.MenuItem;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.session.MediaController;
import androidx.media3.session.SessionToken;
import androidx.media3.ui.DefaultTimeBar;
import androidx.media3.ui.PlayerView;

import com.example.audioboog.dialogs.OptionsPicker;
import com.example.audioboog.services.ActionPlaying;
import com.example.audioboog.services.MediaPlayerService;
import com.example.audioboog.services.PlaybackService;
import com.example.audioboog.source.Audiobook;
import com.example.audioboog.source.Chapter;
import com.example.audioboog.source.ChaptersCollection;
import com.example.audioboog.source.PlaybackSpeed;
import com.example.audioboog.source.Timeout;
import com.example.audioboog.utils.Utils;
import com.google.android.material.navigation.NavigationView;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@UnstableApi public class PlayerActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener, ActionPlaying {
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

    boolean isMediaControllerBound;
    MediaController mediaController;
    ListenableFuture<MediaController> mediaControllerFuture;
    SessionToken sessionToken;
    Audiobook audiobook;

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
            if (isMediaControllerBound) {
                playOrPause();
                setUiPlayingState();
            }
        });
        nextButton.setOnClickListener(v -> {
            if (isMediaControllerBound) {
                setGuiMediaPaused();
                if (mediaController.hasNextMediaItem()) {
                    mediaController.seekToNextMediaItem();
                    setUiForNewAudio();
                    setGuiMediaPlaying();
                }
            }
        });
        previousButton.setOnClickListener(v -> {
            if (isMediaControllerBound) {
                setGuiMediaPaused();
                if (mediaController.hasPreviousMediaItem()) {
                    mediaController.seekToPreviousMediaItem();
                    setUiForNewAudio();
                    setGuiMediaPlaying();
                }
            }
        });
        playbackSpeedButton.setOnClickListener(v -> {
            if (isMediaControllerBound) {
                pickPlaybackSpeed();
            }
        });
        timeoutButton.setOnClickListener(v -> {
            if (isMediaControllerBound) {
                pickTimeoutDuration();
            }
        });

        fastForwardButton.setOnClickListener(v -> {
            if (isMediaControllerBound) {
                mediaController.seekForward();
            }
        });

        fastRewindButton.setOnClickListener(v -> {
            if (isMediaControllerBound) {
                mediaController.seekBack();
            }
        });
    }

    private void setUiPlayingState() {
        if (mediaController.isPlaying()) {
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
                if (!isMediaControllerBound) return;
                startTxt.setText(Utils.convertPlayingTimeToString(progress));
                setPercentageProgress(seekBar, progress);
                calculateTimeout();
                setChapterInfo();
            }

            private void setChapterInfo() {
                if (audiobook == null) return;
                if (chapterName != null && !chapterName.equals(audiobook.getCurrentChapter().getName())) setUiForNewAudio();
                String chapterTimeoutText = "Ends in: " + Utils.convertPlayingTimeToString(getRemainingTimeInChapter());
                chapterTimeoutTxt.setText(chapterTimeoutText);
            }

            private void calculateTimeout() {
//                if (mediaPlayerService.timeoutSet()) { TODO
//                    timeoutDurationTxt.setText(Utils.convertPlayingTimeToString((int)mediaPlayerService.getRemainingTimeout()));
//                }
            }

            private void setPercentageProgress(SeekBar seekBar, long progress) {
                try {
                    String percentage = progress * 100 / (long) seekBar.getMax() + "%";
                    percentageTxt.setText(percentage);
                } catch (ArithmeticException ignored) {}
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                if (!isMediaControllerBound) return;
                setGuiMediaPaused();
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (!isMediaControllerBound) return;
//                mediaPlayerService.seekMediaPlayer(seekBar.getProgress()); TODO
                setGuiMediaPlaying();
            }
        });
    }

    private void startSeekBar() {
        seekbarTimer = Executors.newScheduledThreadPool(1);
        seekbarTimer.scheduleWithFixedDelay(() -> {
            if (isMediaControllerBound) {
                if (!seekBar.isPressed()) {
                    try {
                        long x = mediaController.getCurrentPosition();

                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                    Log.i("position", String.valueOf(mediaController.getCurrentPosition()));
                    seekBar.setProgress((int)mediaController.getCurrentPosition());
                }
            }
        }, 1000, 300, TimeUnit.MILLISECONDS);
    }

    private void setUiForNewAudio() {
        if (!isMediaControllerBound) return;
        int duration = (int)mediaController.getDuration();
        seekBar.setMax(duration);
        stopTxt.setText(Utils.convertPlayingTimeToString(duration));
        chapterName = audiobook.getCurrentChapter().getName();
        chapterNameTxt.setText(chapterName);
        byte[] coverImage = audiobook.getEmbeddedPicture();
        if (coverImage != null) {
            bookCoverImgView.setImageBitmap(BitmapFactory.decodeByteArray(coverImage, 0, coverImage.length));
        }
        String playbackSpeed = mediaController.getPlaybackParameters().speed + "x";
        playbackSpeedTxt.setText(playbackSpeed);
//        if (mediaPlayerService.timeoutSet()) { TODO
//            timeoutDurationTxt.setText(Utils.convertPlayingTimeToString((int)mediaPlayerService.getRemainingTimeout()));
//        }
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

        PlaybackSpeed currentPlaybackSpeed = PlaybackSpeed.getByValue(mediaController.getPlaybackParameters().speed);
        if (currentPlaybackSpeed != null) pickPlaybackSpeed.setDefaultValue(currentPlaybackSpeed.getId());
        pickPlaybackSpeed.setValueSetListener(v -> {
            PlaybackSpeed speed = playbackValues.get(pickPlaybackSpeed.getPickedValue());
            if (isMediaControllerBound) {
                mediaController.setPlaybackSpeed(speed.getValue());
                playbackSpeedTxt.setText(speed.getPlaybackString());
                pickPlaybackSpeed.dismiss();
            }
        });
        pickPlaybackSpeed.show();
    }

    private void pickChapter()
    {
        if (!isMediaControllerBound) return;
        ChaptersCollection.initChaptersCollection(audiobook.getChapters());
        ArrayList<ChaptersCollection> chaptersValues = ChaptersCollection.getChaptersCollections();
        final OptionsPicker pickChapter = new OptionsPicker(PlayerActivity.this, ChaptersCollection.chaptersNumbers(), "Pick Chapter");

        ChaptersCollection currentChapter = ChaptersCollection.getByUid(audiobook.getCurrentChapter().getUid());
        if (currentChapter != null) pickChapter.setDefaultValue(currentChapter.getId());
        pickChapter.setValueSetListener(v -> {
            ChaptersCollection chapter = chaptersValues.get(pickChapter.getPickedValue());
            if (isMediaControllerBound) {
                setGuiMediaPaused();
                mediaController.seekTo(chapter.getChapter().getChapterNumber() - 1, 0);
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
            if (isMediaControllerBound) {
//                mediaPlayerService.setTimeout(timeout.getValue()); TODO
                timeoutDurationTxt.setText(Utils.convertPlayingTimeToString(timeout.getValue()*60000));
                pickTimeout.dismiss();
            }
        });
        pickTimeout.setCancelListener(v -> {
            if (!isMediaControllerBound) return;
//            mediaPlayerService.cancelTimeout(); TODO
            timeoutDurationTxt.setText(Utils.convertPlayingTimeToString((0)));
        });
        pickTimeout.show();
    }

    @Override
    protected void onStop() {
        super.onStop();
        MediaController.releaseFuture(mediaControllerFuture);
        isMediaControllerBound = false;
        pauseSeekBar();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
//        if (mediaPlayerService != null) {
//            if (mediaServiceBound) {
//                unbindService(connection);
//                mediaServiceBound = false;
//            }
//        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        initializeMediaController();
    }

    @OptIn(markerClass = UnstableApi.class) private void initializeMediaController() {
        sessionToken =
                new SessionToken(this, new ComponentName(this, PlaybackService.class));
        mediaControllerFuture = new MediaController.Builder(this, sessionToken).buildAsync();
        mediaControllerFuture.addListener(() -> {
            try {
                mediaController = mediaControllerFuture.get();
                isMediaControllerBound = true;
                audiobook = mediaController.getSessionExtras().getParcelable("audiobook", Audiobook.class);
                setUiForNewAudio();
                if (seekbarTimer == null || seekbarTimer.isShutdown()) startSeekBar();
            } catch (ExecutionException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        }, MoreExecutors.directExecutor());
    }

    @Override
    public void forwardClicked() {
        if (!isMediaControllerBound) return;
        mediaController.seekForward();
    }

    @Override
    public void rewindClicked() {
        if (!isMediaControllerBound) return;
        mediaController.seekBack();
    }

    @Override
    public void playClicked() {
        if (!isMediaControllerBound) return;
        playOrPause();
        setUiPlayingState();
    }

    private void playOrPause() {
        if (mediaController.isPlaying()) {
            mediaController.pause();
        } else {
            mediaController.play();
        }
    }

    private int getRemainingTimeInChapter() {
        if (!isMediaControllerBound) return 0;
        return (int)(mediaController.getDuration() - mediaController.getCurrentPosition());

    }
}