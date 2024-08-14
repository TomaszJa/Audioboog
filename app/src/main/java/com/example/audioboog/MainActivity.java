package com.example.audioboog;

import android.annotation.SuppressLint;
import android.app.Activity;
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
import android.os.Environment;
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
import androidx.documentfile.provider.DocumentFile;
import androidx.drawerlayout.widget.DrawerLayout;

import android.Manifest;
import android.widget.TextView;
import android.widget.Toast;

import com.example.audioboog.services.MediaPlayerService;
import com.example.audioboog.source.Audiobook;
import com.example.audioboog.source.Chapter;
import com.google.android.material.navigation.NavigationView;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener  {
    private DrawerLayout drawerLayout;
    NavigationView navigationView;
    Toolbar toolbar;

    ListView listView;
    String[] items;
    ImageButton hbtnnext,hbtnprev,hbtnpause;
    TextView txtnp;
    int songId;
    private ArrayList<Audiobook> audiobooks;

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

        audiobooks = new ArrayList<>();

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
        Audiobook audiobook = audiobooks.get(songId);
        mediaUri = audiobook.getCurrentChapter().getPath();
        Intent intent = new Intent(getApplicationContext(), MediaPlayerService.class);
        intent.putExtra("audiobook", audiobook);
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
                    String x = singlefile.getName();
                    foundSongs.addAll(findSongs(singlefile));
                } else if (singlefile.getName().endsWith(".mp3") || singlefile.getName().endsWith(".wav")) {
                    foundSongs.add(singlefile);
                }
            }
        }
        return foundSongs;
    }

    void displaySongs() {
//        mySongs = findSongs(Environment.getExternalStorageDirectory());
//
//        items = new String[mySongs.size()];
//        for (int i = 0; i < mySongs.size(); i++) {
//            items[i] = mySongs.get(i).getName().replace(".mp3", "").replace(".wav", "");
//        }

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
        mediaUri = audiobooks.get(songId).getCurrentChapter().getPath();
        setSongName(audiobooks.get(songId).getCurrentChapter().getName());

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
        startActivity(mIntent);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
        int x = menuItem.getItemId();
        int itemId = menuItem.getItemId();
        if (itemId == R.id.navalbums) {
            addAudioFiles();
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
                        Uri uri;
                        ArrayList<Chapter> chapters = new ArrayList<>();
                        if(null != data.getClipData()) {
                            for (int i = 0; i < data.getClipData().getItemCount(); i++) {
                                uri = data.getClipData().getItemAt(i).getUri();
                                chapters.add(getChapter(uri));
                            }
                        } else {
                            uri = data.getData();
                            chapters.add(getChapter(uri));
                        }
                        Collections.sort(chapters);
                        Audiobook audiobook = new Audiobook(chapters.get(0).getBookName(), chapters, 0);
                        audiobooks.add(audiobook);
                        displaySongs();
                    }
                }
            });

    private Chapter getChapter(Uri uri) {
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
            return new Chapter(chapterNumber, name, bookName, uri, art, 0, timeInMillisec);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String getNameFromUri(Uri uri){
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
        initializeMediaPlayerService();
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