package com.example.audioboog;

import static com.example.audioboog.services.ApplicationClass.ACTION_FORWARD;
import static com.example.audioboog.services.ApplicationClass.ACTION_PLAY;
import static com.example.audioboog.services.ApplicationClass.ACTION_REVERT;
import static com.example.audioboog.services.ApplicationClass.CHANNEL_ID_2;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.media.session.MediaSession;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.OpenableColumns;
import android.support.v4.media.session.MediaSessionCompat;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;
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
import androidx.core.app.NotificationCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import android.Manifest;
import androidx.appcompat.widget.SearchView;
import android.widget.TextView;

import com.example.audioboog.dialogs.AudiobookOptions;
import com.example.audioboog.services.ActionPlaying;
import com.example.audioboog.services.DatabaseService;
import com.example.audioboog.services.MediaPlayerService;
import com.example.audioboog.services.NotificationReceiver;
import com.example.audioboog.source.Audiobook;
import com.example.audioboog.source.Chapter;
import com.google.android.material.navigation.NavigationView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    private DrawerLayout drawerLayout;
    NavigationView navigationView;
    Toolbar toolbar;
    SearchView searchView;

    ListView listView;
    ImageButton buttonForward, buttonRewind, hbtnpause;
    TextView txtnp;
    int songId;
    private ArrayList<Audiobook> audiobooks;
    private String currentAudiobookUid;

    DatabaseService databaseService;
    MediaPlayerService mediaPlayerService;
    SharedPreferences sharedPreferences;
    boolean databaseServiceBound;
    boolean mediaServiceBound;

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
        txtnp = findViewById(R.id.txtnp);

        audiobooks = new ArrayList<>();

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        drawerLayout = findViewById(R.id.drawer_layout);

        navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        listView = findViewById(R.id.listViewSong);
        requestPermissionLauncher.launch(Manifest.permission.READ_MEDIA_AUDIO);
        requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            requestPermissionLauncher.launch(Manifest.permission.FOREGROUND_SERVICE_LOCATION);
        }

        buttonForward = findViewById(R.id.buttonForward);
        buttonRewind = findViewById(R.id.buttonRewind);
        hbtnpause = findViewById(R.id.hbtnpause);
        searchView=findViewById(R.id.searchView);

        searchView.setOnClickListener(v -> searchView.onActionViewExpanded());


        if (savedInstanceState != null) {
            Intent i = getIntent();
            Bundle bundle = i.getExtras();
        }

        txtnp.setOnClickListener(v -> {
            if (mediaServiceBound) {
                startPlayerActivity();
            }
        });


        hbtnpause.setOnClickListener(v -> {
            if (mediaServiceBound) {
                mediaPlayerService.playOrPause();
                setPlayOrPause();
            }
        });


        buttonForward.setOnClickListener(v -> {
            if (mediaServiceBound) {
                mediaPlayerService.fastForward();
            }
        });
        buttonRewind.setOnClickListener(v -> {
            if (mediaServiceBound) {
                mediaPlayerService.fastRewind();
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
        CustomAdapter customAdapter = new CustomAdapter(getApplicationContext(), android.R.layout.simple_list_item_1, audiobooks);
        listView.setAdapter(customAdapter);

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                customAdapter.getFilter().filter(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                customAdapter.getFilter().filter(newText);
                return false;
            }
        });

        listView.setOnItemClickListener((parent, view, position, id) -> {
            songId = position;
            String chosenAudiobookUid = audiobooks.get(position).getUid();
            sharedPreferences.edit().putString("currently_playing", chosenAudiobookUid).apply();
            playMedia();
            startPlayerActivity();
        });
        listView.setOnItemLongClickListener((parent, view, position, id) -> {
            Audiobook chosenAudiobook = audiobooks.get(position);
            AudiobookOptions audiobookOptions = new AudiobookOptions(MainActivity.this, chosenAudiobook);
            audiobookOptions.setDeleteButtonListener(v -> {
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setCancelable(true);
                builder.setTitle("Delete Audiobook");
                builder.setMessage("Are you sure you want to delete " + chosenAudiobook.getName() + "?");
                builder.setPositiveButton(android.R.string.ok, (dialog, which) -> {
                            databaseService.deleteAudiobook(chosenAudiobook);
                            audiobooks.remove(chosenAudiobook);
                            audiobookOptions.dismiss();
                            displaySongs();
                        });
                builder.setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.dismiss());
                AlertDialog dialog = builder.create();
                dialog.show();
            });
            audiobookOptions.setResetButtonListener(v -> {
                if (mediaServiceBound) {
                    Audiobook audiobook = mediaPlayerService.getCurrentAudiobook();
                    if (Objects.equals(audiobook.getUid(), chosenAudiobook.getUid())) {
                        mediaPlayerService.releaseMediaPlayer();
                    }
                }
                chosenAudiobook.resetAudiobook();
                databaseService.updateAudiobook(chosenAudiobook);
                audiobookOptions.dismiss();
                displaySongs();
            });
            audiobookOptions.show();
            return true;
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
            setSongName(mediaPlayerService.getCurrentAudiobook().getName());
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

    class CustomAdapter extends ArrayAdapter<Audiobook> implements Filterable {

        public CustomAdapter(@NonNull Context context, int resource, @NonNull List<Audiobook> objects) {
            super(context, resource, objects);
        }

        @Override
        public int getCount() {
            return audiobooks.size();
        }

        @Override
        public Audiobook getItem(int position) {
            return audiobooks.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            @SuppressLint("ViewHolder") View myView = getLayoutInflater().inflate(R.layout.list_item, null);
            Audiobook audiobook = audiobooks.get(position);
            ImageView imageView = myView.findViewById(R.id.coverImage);
            TextView textSong = myView.findViewById(R.id.audiobookName);
            TextView percentageView = myView.findViewById(R.id.percentageCompletion);
            byte[] art = audiobook.getEmbeddedPicture();
            if (art != null) {
                imageView.setImageBitmap(BitmapFactory.decodeByteArray(art, 0, art.length));
            }
            String percentage;
            try {
                percentage = audiobook.getCurrentPosition() * 100 / audiobook.getTotalDuration() + "%";
            } catch (ArithmeticException ex) {
                percentage = "Unknown";
            }
            percentageView.setText(percentage);

            textSong.setSelected(true);
            textSong.setText(audiobook.getName());

            return myView;
        }

        @NonNull
        @Override
        public Filter getFilter() {
            return new Filter() {
                @Override
                protected FilterResults performFiltering(CharSequence constraint) {
                    FilterResults results = new FilterResults();
                    if (constraint == null || constraint.length() == 0) {
                        audiobooks = databaseService.getAudiobooks();
                        results.values = audiobooks;
                        results.count = audiobooks.size();
                    }
                    else {
                        ArrayList<Audiobook> filteredAudiobooks = new ArrayList<>();
                        for (Audiobook audiobook : audiobooks) {
                            if (audiobook.getName().toUpperCase().contains( constraint.toString().toUpperCase())) {
                                filteredAudiobooks.add(audiobook);
                            }
                        }
                        results.values = filteredAudiobooks;
                        results.count = filteredAudiobooks.size();
                    }
                    return results;
                }
                @Override
                protected void publishResults(CharSequence constraint, FilterResults results) {
                    audiobooks = (ArrayList<Audiobook>) results.values;
                    notifyDataSetChanged();
                }
            };
        }
    }

}