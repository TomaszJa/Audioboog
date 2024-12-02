package com.example.audioboog;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
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
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.OpenableColumns;
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

import android.Manifest;

import androidx.appcompat.widget.SearchView;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.session.MediaController;
import androidx.media3.session.SessionToken;

import android.widget.TextView;

import com.example.audioboog.dialogs.AudiobookOptions;
import com.example.audioboog.services.DatabaseService;
import com.example.audioboog.services.MediaPlayerService;
import com.example.audioboog.services.PlaybackService;
import com.example.audioboog.source.Audiobook;
import com.example.audioboog.source.Chapter;
import com.google.android.material.navigation.NavigationView;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

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
    SharedPreferences sharedPreferences;
    boolean databaseServiceBound;

    boolean isMediaControllerBound;
    MediaController mediaController;
    ListenableFuture<MediaController> mediaControllerFuture;
    SessionToken sessionToken;

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
        audiobooks = new ArrayList<>();
        sharedPreferences = getSharedPreferences("sp", MODE_PRIVATE);
        currentAudiobookUid = sharedPreferences.getString("currently_playing", "");
        bindDatabaseService();
        initializeUi();
        requestPermissions();
        setClickListeners();
    }

    private void initializeUi() {
        txtnp = findViewById(R.id.txtnp);
        toolbar = findViewById(R.id.toolbar);
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        listView = findViewById(R.id.listViewSong);
        buttonForward = findViewById(R.id.buttonForward);
        buttonRewind = findViewById(R.id.buttonRewind);
        hbtnpause = findViewById(R.id.hbtnpause);
        searchView = findViewById(R.id.searchView);

        setSupportActionBar(toolbar);
        navigationView.setNavigationItemSelectedListener(this);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
    }

    private void requestPermissions() {
        requestPermissionLauncher.launch(Manifest.permission.READ_MEDIA_AUDIO);
        requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
        requestPermissionLauncher.launch(Manifest.permission.FOREGROUND_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            requestPermissionLauncher.launch(Manifest.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK);
            requestPermissionLauncher.launch(Manifest.permission.FOREGROUND_SERVICE_LOCATION);
        }
    }

    private void setClickListeners() {
        searchView.setOnClickListener(v -> searchView.onActionViewExpanded());
        txtnp.setOnClickListener(v -> {
            if (isMediaControllerBound) {
                startPlayerActivity();
            }
        });
        hbtnpause.setOnClickListener(v -> {
            if (isMediaControllerBound) {
                playOrPause();
                setPlayOrPause();
            }
        });
        buttonForward.setOnClickListener(v -> {
            if (isMediaControllerBound) {
                mediaController.seekForward();
//                mediaPlayerService.fastForward();
            }
        });
        buttonRewind.setOnClickListener(v -> {
            if (isMediaControllerBound) {
                mediaController.seekBack();
//                mediaPlayerService.fastRewind();
            }
        });
    }

    private void playOrPause() {
        if (mediaController.isPlaying()) {
            mediaController.pause();
        } else {
            mediaController.play();
        }
    }

    private void setPlayOrPause() {
        if (mediaController.isPlaying()) {
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

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new RequestPermission(), isGranted -> {
                if (isGranted) {
                    Log.i("Permission: ", "Granted");
                } else {
                    Log.i("Permission: ", "Denied");
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

        listView.setOnItemClickListener((parent, view, position, id) -> listViewClickListener(position));
        listView.setOnItemLongClickListener((parent, view, position, id) -> longListViewClickListener(position));
    }

    private void listViewClickListener(int position) {
        songId = position;
        String chosenAudiobookUid = audiobooks.get(position).getUid();
        sharedPreferences.edit().putString("currently_playing", chosenAudiobookUid).apply();
        playMedia();
        startPlayerActivity();
    }

    @OptIn(markerClass = UnstableApi.class) private boolean longListViewClickListener(int position) {
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
            if (isMediaControllerBound) {
                Audiobook audiobook = mediaController.getSessionExtras().getParcelable("audiobook", Audiobook.class);
                if (audiobook != null && Objects.equals(audiobook.getUid(), chosenAudiobook.getUid())) {
//                    mediaController.seekTo(0, 0);
                    mediaController.release();
                }
            }
            chosenAudiobook.resetAudiobook();
            databaseService.updateAudiobook(chosenAudiobook);
            audiobookOptions.dismiss();
            displaySongs();
        });
        audiobookOptions.show();
        return true;
    }

    private void playMedia() {
        Audiobook audiobook = audiobooks.get(songId);
        setSongName(audiobook.getName());

        if (!isMediaControllerBound) {
            initializeMediaController(audiobook);
        } else {
            playAudiobook(audiobook);
        }
        setGuiMediaPlaying();
    }

    @OptIn(markerClass = UnstableApi.class) private void startPlayerActivity() {
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
            for (int i = 0; i < audiobooks.size(); i++) {
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

    ActivityResultLauncher<Intent> addAudioFilesResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
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
//        mediaServiceBound = unbindService(mediaPlayerService, mediaServiceBound, mediaServiceConnection);
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
        initializeMediaController(null);
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
        MediaController.releaseFuture(mediaControllerFuture);
        isMediaControllerBound = false;
    }

    @Override
    protected void onResume() {
        if (isMediaControllerBound && mediaController != null && mediaController.getCurrentMediaItem() != null) {
            setSongName((String) mediaController.getCurrentMediaItem().mediaMetadata.albumTitle);
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
                    } else {
                        ArrayList<Audiobook> filteredAudiobooks = new ArrayList<>();
                        for (Audiobook audiobook : audiobooks) {
                            if (audiobook.getName().toUpperCase().contains(constraint.toString().toUpperCase())) {
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

    private void initializeMediaController(@Nullable Audiobook audiobook) {
        sessionToken =
                new SessionToken(this, new ComponentName(this, PlaybackService.class));
        mediaControllerFuture = new MediaController.Builder(this, sessionToken).buildAsync();
        mediaControllerFuture.addListener(() -> {
            try {
                mediaController = mediaControllerFuture.get();
                isMediaControllerBound = true;
                if (audiobook != null) {
                    playAudiobook(audiobook);
                }
            } catch (ExecutionException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        }, MoreExecutors.directExecutor());
    }

    private void playAudiobook(Audiobook audiobook) {
        if (mediaController != null) {
            List<MediaItem> mediaItems = new ArrayList<>();
            for (Chapter chapter : audiobook.getChapters()) {
                Uri uri = chapter.getPath();
                Bundle bundle = new Bundle();
                bundle.putString("audiobook-uid", audiobook.getUid());
                MediaItem mediaItem =
                        new MediaItem.Builder()
                                .setUri(uri)
                                .setMediaMetadata(new MediaMetadata.Builder()
                                        .setExtras(bundle).build())
                                .build();
                mediaItems.add(mediaItem);
            }
            mediaController.addMediaItems(mediaItems);
            mediaController.prepare();
            mediaController.play();
        }
    }


}