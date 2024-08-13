package com.example.audioboog;

import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.ServiceConnection;
import android.graphics.PorterDuff;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.view.MenuItem;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.audioboog.dialogs.OptionsPicker;
import com.example.audioboog.services.MediaPlayerService;
import com.example.audioboog.source.PlaybackSpeed;
import com.example.audioboog.source.Timeout;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class PlayerActivity extends AppCompatActivity {
    ImageButton play_button, previous_button, next_button, fast_forward_button, fast_rewind_button,
            playbackSpeedButton, timeoutButton;
    TextView txtsname, txtsstart, txtsstop, txtPercentage, playbackSpeedText, timeoutDuration;
    SeekBar seekBar;
    ScheduledExecutorService seekbarTimer;

    String songName;
    ImageView imageView;
    public static final String EXTRA_NAME = "song_name";

    MediaPlayerService mediaPlayerService;
    SharedPreferences sharedPreferences;
    boolean mediaServiceBound;
    int position;
    Uri mediaUri;

    ArrayList<File> mySongs;

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            Intent mIntent = new Intent(PlayerActivity.this, MainActivity.class);
            songName = mySongs.get(position).getName().toString();
            mIntent.putExtra(EXTRA_NAME, songName);
            startActivity(mIntent);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_player);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        InitializeGuiElements();

        Intent i = getIntent();
        Bundle bundle = i.getExtras();

        String selectedSongName = i.getStringExtra("songname");
        txtsname.setSelected(true);

        sharedPreferences = getSharedPreferences("sp", MODE_PRIVATE);

        if (bundle != null) {
            mySongs = bundle.getParcelableArrayList("songs", File.class);
            position = bundle.getInt("pos", 0);

            if (savedInstanceState == null) initializeMediaPlayerService();
        }
        initializeSeekBar();

        play_button.setOnClickListener(v -> {
            if (mediaServiceBound) {
                mediaPlayerService.playOrPause();
                if (mediaPlayerService.isPlaying()) {
                    setGuiMediaPlaying();
                } else {
                    setGuiMediaPaused();
                }
            }
        });
        next_button.setOnClickListener(v -> {
            position = ((position + 1) % mySongs.size());
            playMedia();
            setSeekBarMax();

        });
        previous_button.setOnClickListener(v -> {
            position = ((position - 1) < 0) ? (mySongs.size() - 1) : (position - 1);
            playMedia();
            setSeekBarMax();
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
//        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
//            @Override
//            public void onCompletion(MediaPlayer mp) {
//                next_button.performClick();
//            }
//        });

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

    private void setGuiMediaPaused() {
        play_button.setImageResource(R.drawable.ic_play);
        pauseSeekBar();
    }

    private void setGuiMediaPlaying() {
        play_button.setImageResource(R.drawable.ic_pause);
        startSeekBar();
    }

    private void initializeMediaPlayerService() {
        mediaUri = Uri.parse(mySongs.get(position).toString());
        Intent intent = new Intent(getApplicationContext(), MediaPlayerService.class);
        intent.setData(mediaUri);
        startService(intent);
        bindService(intent, connection, Context.BIND_AUTO_CREATE);
        sharedPreferences.edit().putString("created", "true").apply();
        songName = mySongs.get(position).getName();
        txtsname.setText(songName);
    }

    private void initializeSeekBar() {
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (!mediaServiceBound) return;
                txtsstart.setText(convertPlayingTimeToString(progress));
                String percentage = Math.round((float) (progress * 100) /seekBar.getMax()) + "%";
                txtPercentage.setText(percentage);
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

    private void playMedia() {
        if (!mediaServiceBound) return;
        Uri uri = Uri.parse(mySongs.get(position).toString());
        songName = mySongs.get(position).getName();
        txtsname.setText(songName);

        mediaPlayerService.playMedia(uri);
        setGuiMediaPlaying();
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

    private void setSeekBarMax() {
        if (!mediaServiceBound) return;
        int duration = mediaPlayerService.getDuration();
        seekBar.setMax(duration);
        txtsstop.setText(convertPlayingTimeToString(duration));
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

        pickTimeout.show();
    }

    private final ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MediaPlayerService.LocalBinder binder = (MediaPlayerService.LocalBinder) service;
            mediaPlayerService = binder.getService();
            if (mediaPlayerService != null) {
                mediaServiceBound = true;
                setSeekBarMax();
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
            if (sharedPreferences.getString("created", "").equals("true")) {
                Intent intent = new Intent(PlayerActivity.this, MediaPlayerService.class);
                bindService(intent, connection, Context.BIND_AUTO_CREATE);
            }
        }
        if (seekbarTimer == null || seekbarTimer.isShutdown()) startSeekBar();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putInt("position", position);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        position = savedInstanceState.getInt("position");
        initializeMediaPlayerService();
        super.onRestoreInstanceState(savedInstanceState);
    }
}