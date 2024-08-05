package com.example.audioboog;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
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

import com.google.android.material.navigation.NavigationView;

import java.io.File;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener  {
    private DrawerLayout drawerLayout;
    ListView listView;
    String[] items;
    Button hbtnnext,hbtnprev,hbtnpause;
    TextView txtnp;
    int position;

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

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        drawerLayout = findViewById(R.id.drawer_layout);

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this,drawerLayout,toolbar,R.string.navigation_drawer_open,R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        listView = findViewById(R.id.listViewSong);
        requestPermissionLauncher.launch(Manifest.permission.READ_MEDIA_AUDIO);

        hbtnnext = findViewById(R.id.hbtnnext);
        hbtnprev = findViewById(R.id.hbtnprev);
        hbtnpause = findViewById(R.id.hbtnpause);

//        txtnp.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                if (PlayerActivity.mediaPlayer==null || txtnp.getText().toString().equals(""))
//                {
//                    Toast.makeText(MainActivity.this, "No song is playing", Toast.LENGTH_SHORT).show();
//                }
//                else
//                {
//                    Intent mIntent=new Intent(MainActivity.this, PlayerActivity.class);
//                    mIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//                    mIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
//                    startActivity(mIntent);
//                }
//            }
//        });



        hbtnpause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (PlayerActivity.mediaPlayer!=null)
                {
                    if (PlayerActivity.mediaPlayer.isPlaying())
                    {
                        hbtnpause.setBackgroundResource(R.drawable.ic_play_circle);
                        PlayerActivity.mediaPlayer.pause();
                    }
                    else
                    {
                        hbtnpause.setBackgroundResource(R.drawable.ic_pause_circle);
                        PlayerActivity.mediaPlayer.start();
                    }
                }
            }
        });


        hbtnnext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (PlayerActivity.mediaPlayer!=null)
                {

                }
            }
        });
        hbtnprev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (PlayerActivity.mediaPlayer!=null)
                {

                }
            }
        });
    }

    // Register the permissions callback, which handles the user's response to the
    // system permissions dialog. Save the return value, an instance of
    // ActivityResultLauncher, as an instance variable.
    private ActivityResultLauncher<String> requestPermissionLauncher =
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
        final ArrayList<File> mySongs = findSongs(Environment.getExternalStorageDirectory());

        items = new String[mySongs.size()];
        for (int i = 0; i < mySongs.size(); i++) {
            items[i] = mySongs.get(i).getName().replace(".mp3", "").replace(".wav", "");
        }

        CustomAdapter customAdapter = new CustomAdapter();
        listView.setAdapter(customAdapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String songName = listView.getItemAtPosition(position).toString();
                startActivity(new Intent(getApplicationContext(), PlayerActivity.class)
                        .putExtra("songs", mySongs)
                        .putExtra("songname", songName)
                        .putExtra("pos", position));
            }
        });
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
        int x = menuItem.getItemId();
        switch (menuItem.getItemId())
        {
//            case R.id.navalbums:
//                Toast.makeText(this, "Albums here", Toast.LENGTH_SHORT).show();
//                break;
//            case R.id.navartists:
//                Toast.makeText(this, "Artists here", Toast.LENGTH_SHORT).show();
//                break;
//            case R.id.navsongs:
//                Toast.makeText(this, "All songs here", Toast.LENGTH_SHORT).show();
//                break;
//            case R.id.navonline:
//                Toast.makeText(this, "Online Library here", Toast.LENGTH_SHORT).show();
//                break;
            case 1:
                Toast.makeText(this, "Albums here", Toast.LENGTH_SHORT).show();
                break;
            case 2:
                Toast.makeText(this, "Artists here", Toast.LENGTH_SHORT).show();
                break;
            case 3:
                Toast.makeText(this, "All songs here", Toast.LENGTH_SHORT).show();
                break;
            case 4:
                Toast.makeText(this, "Online Library here", Toast.LENGTH_SHORT).show();
                break;
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
    protected void onResume() {
        if (PlayerActivity.mediaPlayer!=null)
        {
            if (PlayerActivity.mediaPlayer.isPlaying())
            {
                hbtnpause.setBackgroundResource(R.drawable.ic_pause_circle);
            }
            else {
                hbtnpause.setBackgroundResource(R.drawable.ic_play_circle);
            }
            Intent i = getIntent();
            String songName = i.getStringExtra(PlayerActivity.EXTRA_NAME);
//            txtnp.setText(songName);
//            txtnp.setSelected(true);
        }
        else
        {
            hbtnpause.setBackgroundResource(R.drawable.ic_play_circle);
        }
        super.onResume();
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