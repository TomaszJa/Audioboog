package com.example.audioboog;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import android.Manifest;
import android.widget.TextView;
import android.widget.Toast;

import com.example.audioboog.services.MediaPlayerService;
import com.google.android.material.navigation.NavigationView;

import java.io.File;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener  {
    private DrawerLayout drawerLayout;
    NavigationView navigationView;
    Toolbar toolbar;

    ListView listView;
    String[] items;
    Button hbtnnext,hbtnprev,hbtnpause;
    TextView txtnp;
    int songId;
    String chosenSongName;
    private ArrayList<File> mySongs;

    MediaPlayerService mediaPlayerService;
    SharedPreferences sharedPreferences;
    boolean mediaServiceBound;
    private Uri mediaUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.drawer_layout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        sharedPreferences = getSharedPreferences("sp", MODE_PRIVATE);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        drawerLayout = findViewById(R.id.drawer_layout);
        txtnp = findViewById(R.id.txtnp);

        navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this,drawerLayout,toolbar,R.string.navigation_drawer_open,R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        listView = findViewById(R.id.listViewSong);
        requestPermissionLauncher.launch(Manifest.permission.READ_MEDIA_AUDIO);

        hbtnnext = findViewById(R.id.hbtnnext);
        hbtnprev = findViewById(R.id.hbtnprev);
        hbtnpause = findViewById(R.id.hbtnpause);



        if (savedInstanceState != null) {
            Intent i = getIntent();
            Bundle bundle = i.getExtras();
            if (bundle != null) {
                songId = bundle.getInt("pos", 0);
            }
            initializeMediaPlayerService();
        }

        txtnp.setOnClickListener(v -> {
            if (mediaServiceBound) {
                if (mediaPlayerService.isPlaying()) {
                    startPlayerActivity();
                } else {
                    Toast.makeText(MainActivity.this, "No song is playing", Toast.LENGTH_SHORT).show();
                }
            }
        });



        hbtnpause.setOnClickListener(v -> {
            if (mediaServiceBound) {
                mediaPlayerService.playOrPause();
                setPlayOrPause();
            }
        });


        hbtnnext.setOnClickListener(v -> {
            if (mediaServiceBound) {
                songId = ((songId + 1) % mySongs.size());
                playMedia();
            }
        });
        hbtnprev.setOnClickListener(v -> {
            if (mediaServiceBound) {
                songId = ((songId - 1) < 0) ? (mySongs.size() - 1) : (songId - 1);
                playMedia();
            }
        });
    }

    private void setPlayOrPause() {
        if (mediaPlayerService.isPlaying()) {
            setGuiMediaPlaying();
        } else {
            setGuiMediaPaused();
        }
    }

    private void setGuiMediaPaused() {
        hbtnpause.setBackgroundResource(R.drawable.ic_play_circle);
    }

    private void setGuiMediaPlaying() {
        hbtnpause.setBackgroundResource(R.drawable.ic_pause_circle);
    }

    private void initializeMediaPlayerService() {
        mediaUri = Uri.parse(mySongs.get(songId).toString());
        Intent intent = new Intent(getApplicationContext(), MediaPlayerService.class);
        intent.setData(mediaUri);
        startService(intent);
        bindService(intent, connection, Context.BIND_AUTO_CREATE);
        sharedPreferences.edit().putString("created", "true").apply();
    }

    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MediaPlayerService.LocalBinder binder = (MediaPlayerService.LocalBinder) service;
            mediaPlayerService = binder.getService();
            if (mediaPlayerService != null) {
                mediaServiceBound = true;
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mediaServiceBound = false;
        }
    };

    // Register the permissions callback, which handles the user's response to the
    // system permissions dialog. Save the return value, an instance of
    // ActivityResultLauncher, as an instance variable.
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new RequestPermission(), isGranted -> {
                if (isGranted) {
                    Log.i("Permission: ", "Granted");
                    displaySongs();
                    // Permission is granted. Continue the action or workflow in your
                    // app.
                } else {
                    Log.i("Permission: ", "Denied");
                    // Explain to the user that the feature is unavailable because the
                    // feature requires a permission that the user has denied. At the
                    // same time, respect the user's decision. Don't link to system
                    // settings in an effort to convince the user to change their
                    // decision.
                }
            });

    public ArrayList<File> findSongs(File file) {
        ArrayList<File> foundSongs = new ArrayList<>();

        File[] files = file.listFiles();

        if (files != null) {
            for (File singlefile : files) {
                if (singlefile.isDirectory() && !singlefile.isHidden()) {
                    foundSongs.addAll(findSongs(singlefile));
                } else if (singlefile.getName().endsWith(".mp3") || singlefile.getName().endsWith(".wav")) {
                    foundSongs.add(singlefile);
                }
            }
        }
        return foundSongs;
    }

    void displaySongs() {
        mySongs = findSongs(Environment.getExternalStorageDirectory());

        items = new String[mySongs.size()];
        for (int i = 0; i < mySongs.size(); i++) {
            items[i] = mySongs.get(i).getName().replace(".mp3", "").replace(".wav", "");
        }

        CustomAdapter customAdapter = new CustomAdapter();
        listView.setAdapter(customAdapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                songId = position;
                playMedia();
                startPlayerActivity();
            }
        });
    }

    private void playMedia() {
        mediaUri = Uri.parse(mySongs.get(songId).toString());
        setSongName(listView.getItemAtPosition(songId).toString());

        if (!mediaServiceBound) {
            initializeMediaPlayerService();
        } else {
            mediaPlayerService.playMedia(mediaUri);
        }
        setGuiMediaPlaying();
    }

    private void startPlayerActivity() {
        Intent mIntent=new Intent(MainActivity.this, PlayerActivity.class);
        mIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        mIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(mIntent
                .putExtra("songs", mySongs)
                .putExtra("songname", chosenSongName)
                .putExtra("pos", songId));
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
        int x = menuItem.getItemId();
        int itemId = menuItem.getItemId();
        if (itemId == R.id.navalbums) {
            Toast.makeText(this, "Albums here", Toast.LENGTH_SHORT).show();
        } else if (itemId == R.id.navartists) {
            Toast.makeText(this, "Artists here", Toast.LENGTH_SHORT).show();
        } else if (itemId == R.id.navsongs) {
            Toast.makeText(this, "All songs here", Toast.LENGTH_SHORT).show();
        } else if (itemId == R.id.navonline) {
            Toast.makeText(this, "Online Library here", Toast.LENGTH_SHORT).show();
        }
        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START))
        {
            drawerLayout.closeDrawer(GravityCompat.START);
        }
        else
        {
            Intent startMain = new Intent(Intent.ACTION_MAIN);
            startMain.addCategory(Intent.CATEGORY_HOME);
            startActivity(startMain);
            super.onBackPressed();
        }
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
                Intent intent = new Intent(MainActivity.this, MediaPlayerService.class);
                bindService(intent, connection, Context.BIND_AUTO_CREATE);
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onResume() {
        if (mediaServiceBound) {
            setSongName(mediaPlayerService.getFilename());
            setPlayOrPause();
        }
        super.onResume();
    }

    private void setSongName(String songName) {
        chosenSongName = songName;
        txtnp.setText(chosenSongName);
        txtnp.setSelected(true);
    }


    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putInt("position", songId);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        songId = savedInstanceState.getInt("position");
        initializeMediaPlayerService();
        super.onRestoreInstanceState(savedInstanceState);
    }

    class CustomAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return items.length;
        }

        @Override
        public Object getItem(int position) {
            return items[position];
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            @SuppressLint("ViewHolder") View myView = getLayoutInflater().inflate(R.layout.list_item, null);
            TextView textSong = myView.findViewById(R.id.txtsongname);
            textSong.setSelected(true);
            textSong.setText(items[position]);

            return myView;
        }
    }

}