package com.example.audioboog;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.ServiceConnection;
import android.graphics.PorterDuff;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.audioboog.services.MediaPlayerService;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class PlayerActivity extends AppCompatActivity {
    Button play_button, previous_button, next_button, fast_forward_button, fast_rewind_button;
    TextView txtsname, txtsstart, txtsstop;
    SeekBar seekBar;
    ScheduledExecutorService timer;

    String songName;
    ImageView imageView;
    public static final String EXTRA_NAME = "song_name";

    MediaPlayerService mediaPlayerService;
    SharedPreferences sharedPreferences;
    boolean mediaServiceBound;
    static MediaPlayer mediaPlayer;
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
        play_button.setBackgroundResource(R.drawable.ic_play);
        pauseSeekBar();
    }

    private void setGuiMediaPlaying() {
        play_button.setBackgroundResource(R.drawable.ic_pause);
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
        seekBar.getProgressDrawable().setColorFilter(getResources().getColor(R.color.black), PorterDuff.Mode.MULTIPLY);
        seekBar.getThumb().setColorFilter(getResources().getColor(R.color.white), PorterDuff.Mode.SRC_IN);

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (!mediaServiceBound) return;
                txtsstart.setText(convertPlayingTimeToString(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                if (!mediaServiceBound) return;
                mediaPlayerService.playOrPause();
                setGuiMediaPaused();
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (!mediaServiceBound) return;
                mediaPlayerService.seekMediaPlayer(seekBar.getProgress());
                mediaPlayerService.playOrPause();
                setGuiMediaPlaying();
            }
        });
        startSeekBar();
    }

    private void playMedia() {
        if (!mediaServiceBound) return;
        Uri uri = Uri.parse(mySongs.get(position).toString());
        songName = mySongs.get(position).getName();
        txtsname.setText(songName);
        play_button.setBackgroundResource(R.drawable.ic_pause);

        mediaPlayerService.playMedia(uri);
        startSeekBar();
    }

    private void startSeekBar() {
        timer = Executors.newScheduledThreadPool(1);
        timer.scheduleWithFixedDelay(() -> {
            if (mediaServiceBound) {
                if (!seekBar.isPressed()) {
                    seekBar.setProgress(mediaPlayerService.getMediaPlayer().getCurrentPosition());
                }
            }
        }, 10, 300, TimeUnit.MILLISECONDS);
    }

    private String convertPlayingTimeToString(int milliseconds) {
        long total_secs = TimeUnit.SECONDS.convert(milliseconds, TimeUnit.MILLISECONDS);
        long mins = TimeUnit.MINUTES.convert(total_secs, TimeUnit.SECONDS);
        long secs = total_secs - (mins * 60);
        return mins + ":" + ((secs < 10) ? ("0" + secs) : secs);
    }

    private void setSeekBarMax() {
        if (!mediaServiceBound) return;
        int duration = mediaPlayerService.getMediaPlayer().getDuration();
        seekBar.setMax(duration);
        txtsstop.setText(convertPlayingTimeToString(duration));
    }

    private void pauseSeekBar() {
        if (timer != null) {
            timer.shutdown();
            try {
                if (!timer.isShutdown()) while (!timer.awaitTermination(1, TimeUnit.SECONDS)) ;
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void InitializeGuiElements() {
        play_button = findViewById(R.id.play_button);
        previous_button = findViewById(R.id.previous_button);
        next_button = findViewById(R.id.next_button);
        fast_forward_button = findViewById(R.id.fast_forward_button);
        fast_rewind_button = findViewById(R.id.fast_rewind_button);
        txtsname = findViewById(R.id.txtsn);
        txtsstart = findViewById(R.id.txtsstart);
        txtsstop = findViewById(R.id.txtsstop);
        seekBar = findViewById(R.id.seekbar);
    }

    private ServiceConnection connection = new ServiceConnection() {
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
    protected void onStop() {
        super.onStop();
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