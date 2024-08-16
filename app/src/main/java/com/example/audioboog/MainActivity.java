package com.example.audioboog;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
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
import androidx.room.Room;

import android.Manifest;
import android.widget.TextView;
import android.widget.Toast;

import com.example.audioboog.database.AppDatabase;
import com.example.audioboog.database.dao.AudiobookDao;
import com.example.audioboog.database.dao.ChapterDao;
import com.example.audioboog.database.relationships.AudiobookWithChapters;
import com.example.audioboog.services.DatabaseService;
import com.example.audioboog.services.MediaPlayerService;
import com.example.audioboog.source.Audiobook;
import com.example.audioboog.source.Chapter;
import com.google.android.material.navigation.NavigationView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    private DrawerLayout drawerLayout;
    NavigationView navigationView;
    Toolbar toolbar;

    ListView listView;
    String[] items;
    ImageButton hbtnnext, hbtnprev, hbtnpause;
    TextView txtnp;
    int songId;
    private ArrayList<Audiobook> audiobooks;
    private String currentAudiobookUid;

    DatabaseService databaseService;
    MediaPlayerService mediaPlayerService;
    SharedPreferences sharedPreferences;
    boolean databaseServiceBound;
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
        currentAudiobookUid = sharedPreferences.getString("currently_playing", "");
        bindDatabaseService();

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        drawerLayout = findViewById(R.id.drawer_layout);
        txtnp = findViewById(R.id.txtnp);

        audiobooks = new ArrayList<>();

        navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
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
                mediaPlayerService.playNextChapter();
                setSongName(mediaPlayerService.getFilename());
            }
        });
        hbtnprev.setOnClickListener(v -> {
            if (mediaServiceBound) {
                mediaPlayerService.playPreviousChapter();
                setSongName(mediaPlayerService.getFilename());
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
        hbtnpause.setImageResource(R.drawable.ic_play);
    }

    private void setGuiMediaPlaying() {
        hbtnpause.setImageResource(R.drawable.ic_pause);
    }

    private void initializeMediaPlayerService() {
        Intent intent = new Intent(getApplicationContext(), MediaPlayerService.class);
        startService(intent);
        bindService(intent, mediaServiceConnection, Context.BIND_AUTO_CREATE);
    }

    private final ServiceConnection mediaServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MediaPlayerService.LocalBinder binder = (MediaPlayerService.LocalBinder) service;
            mediaPlayerService = binder.getService();
            if (mediaPlayerService != null) {
                mediaServiceBound = true;
                mediaPlayerService.playMedia(audiobooks.get(songId));
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mediaServiceBound = false;
        }
    };

    private final ServiceConnection databaseConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            DatabaseService.LocalBinder binder = (DatabaseService.LocalBinder) service;
            databaseService = binder.getService();
            if (databaseService != null) {
                databaseServiceBound = true;
                audiobooks = databaseService.getAudiobooks();
                displaySongs();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            databaseServiceBound = false;
        }
    };

    private void bindDatabaseService() {
        Intent intent = new Intent(getApplicationContext(), DatabaseService.class);
        startService(intent);
        bindService(intent, databaseConnection, Context.BIND_AUTO_CREATE);
    }

    // Register the permissions callback, which handles the user's response to the
    // system permissions dialog. Save the return value, an instance of
    // ActivityResultLauncher, as an instance variable.
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new RequestPermission(), isGranted -> {
                if (isGranted) {
                    Log.i("Permission: ", "Granted");
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

    void displaySongs() {
        CustomAdapter customAdapter = new CustomAdapter();
        listView.setAdapter(customAdapter);

        listView.setOnItemClickListener((parent, view, position, id) -> {
            songId = position;
            String chosenAudiobookUid = audiobooks.get(position).getUid();
            sharedPreferences.edit().putString("currently_playing", chosenAudiobookUid).apply();
            playMedia();
            startPlayerActivity();
        });
    }

    private void playMedia() {
        Audiobook audiobook = audiobooks.get(songId);
        setSongName(audiobook.getName());

        if (!mediaServiceBound) {
            initializeMediaPlayerService();
        } else {
            mediaPlayerService.playMedia(audiobooks.get(songId));
        }
        setGuiMediaPlaying();
    }

    private void startPlayerActivity() {
        Intent mIntent = new Intent(MainActivity.this, PlayerActivity.class);
        mIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        mIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(mIntent);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
        int itemId = menuItem.getItemId();
        if (itemId == R.id.navLibrary) {
            displaySongs();
        } else if (itemId == R.id.navCurrentBook) {
            for (int i=0; i<audiobooks.size(); i++) {
                currentAudiobookUid = sharedPreferences.getString("currently_playing", "");
                if (Objects.equals(audiobooks.get(i).getUid(), currentAudiobookUid)) {
                    songId = i;

                    playMedia();
                    startPlayerActivity();
                }
            }
        } else if (itemId == R.id.navAddBook) {
            addAudioFiles();
        }
        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    private void addAudioFiles() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("audio/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
        addAudioFilesResultLauncher.launch(intent);
    }

    // You can do the assignment inside onAttach or onCreate, i.e, before the activity is displayed
    ActivityResultLauncher<Intent> addAudioFilesResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    // There are no request codes
                    Intent data = result.getData();
                    if (data != null) {
                        Audiobook audiobook = new Audiobook();
                        Uri uri;
                        ArrayList<Chapter> chapters = new ArrayList<>();
                        if (null != data.getClipData()) {
                            for (int i = 0; i < data.getClipData().getItemCount(); i++) {
                                uri = data.getClipData().getItemAt(i).getUri();
                                chapters.add(getChapter(uri, audiobook));
                            }
                        } else {
                            uri = data.getData();
                            chapters.add(getChapter(uri, audiobook));
                        }
                        audiobook.updateWithChapters(chapters);
                        audiobooks.add(audiobook);
                        displaySongs();
                        if (databaseServiceBound) {
                            databaseService.updateAudiobook(audiobook);
                        }
                    }
                }
            });

    private Chapter getChapter(Uri uri, Audiobook audiobook) {
        String name = getNameFromUri(uri);

        try (final MediaMetadataRetriever retriever = new MediaMetadataRetriever()) {
            retriever.setDataSource(this, uri);
            byte[] art = retriever.getEmbeddedPicture();
            String time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            String chapterNumberString = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER);
            int chapterNumber = 0;
            if (chapterNumberString != null) {
                chapterNumber = Integer.parseInt(chapterNumberString.replaceAll("\\D", ""));
            }
            String bookName = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM);
            int timeInMillisec = Integer.parseInt(time);
            retriever.release();
            return new Chapter(audiobook.getUid(), chapterNumber, name, bookName, uri, art, 0, timeInMillisec);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String getNameFromUri(Uri uri) {
        String file = "";
        if (uri == null) return file;
        Cursor cursor =
                getContentResolver().query(uri, null, null, null, null);
        if (cursor != null) {
            int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
            cursor.moveToFirst();
            file = cursor.getString(nameIndex);
            cursor.close();
        } else {
            String[] splittedUri = uri.toString().split("/");
            file = splittedUri[splittedUri.length - 1];
        }
        file = file.replace(".mp3", "").replace(".wav", "");
        return file;
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            Intent startMain = new Intent(Intent.ACTION_MAIN);
            startMain.addCategory(Intent.CATEGORY_HOME);
            startActivity(startMain);
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mediaServiceBound = unbindService(mediaPlayerService, mediaServiceBound, mediaServiceConnection);
        databaseServiceBound = unbindService(databaseService, databaseServiceBound, databaseConnection);
    }

    private boolean unbindService(Service service, boolean serviceBound, ServiceConnection serviceConnection) {
        if (service != null && serviceBound) {
            unbindService(serviceConnection);
        }
        return false;
    }

    @Override
    protected void onStart() {
        super.onStart();
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
        if (mediaServiceBound && mediaPlayerService != null) {
            setSongName(mediaPlayerService.getFilename());
            setPlayOrPause();
        }
        super.onResume();
    }

    private void setSongName(String songName) {
        txtnp.setText(songName);
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
        super.onRestoreInstanceState(savedInstanceState);
    }

    class CustomAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return audiobooks.size();
        }

        @Override
        public Object getItem(int position) {
            return audiobooks.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            @SuppressLint("ViewHolder") View myView = getLayoutInflater().inflate(R.layout.list_item, null);
            TextView textSong = myView.findViewById(R.id.txtsongname);
            ImageView imageView = myView.findViewById(R.id.imgsong);
            byte[] art = audiobooks.get(position).getEmbeddedPicture();
            if (art != null) {
                imageView.setImageBitmap(BitmapFactory.decodeByteArray(art, 0, art.length));
            }

            textSong.setSelected(true);
            textSong.setText(audiobooks.get(position).getName());

            return myView;
        }
    }

}