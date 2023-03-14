package com.melody.musicplayer;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements BroadcastListenerInterface{

    TextView songscounttextview, songscantextview,uisongtitle,uisongartist,
            elapsedduration,totalduration;
    SeekBar musicplayerseekbar;
    FloatingActionButton floatingActionButton;
    LinearLayout progresslayout;
    RelativeLayout musicplayeruilayout;
    ImageView closeuilayout,uishuffleSong,uiprevSong,uipausePlaySong,uinextSong,uirepeatSong;
    ProgressBar progressBar;
    Snackbar snackbar;
    RecyclerView tracklistrecyclerview;
    MyRecyclerViewAdapter myRecyclerViewAdapter;
    ArrayList<String> songTitle = new ArrayList<>();
    ArrayList<String> songData = new ArrayList<>();
    Cursor cursor,settingsCursor;
    String shuffleSong,repeatSong;
    SongSQLiteDBHelper songSQLiteDBHelper;
    String[] songDetails = {
            MediaStore.Audio.Media.TITLE,                        //0
            MediaStore.Audio.Media.DISPLAY_NAME,       //1
            MediaStore.Audio.Media.ARTIST,                     //2
            MediaStore.Audio.Media.ALBUM,                       //3
            MediaStore.Audio.Media.DURATION,                //4
            MediaStore.Audio.Media.DATA,                          //5           // filepath of the audio file
            MediaStore.Audio.Media.ALBUM_ID                  //6           // album id of the file
    };

    private static final int PERMISSION_REQUEST_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        songscounttextview = findViewById(R.id.songscounttextview);
        songscantextview = findViewById(R.id.songscantextview);
        floatingActionButton = findViewById(R.id.floatingActionButton);
        progresslayout = findViewById(R.id.progresslayout);
        progressBar = findViewById(R.id.progressbar);
        tracklistrecyclerview = findViewById(R.id.tracklistrecyclerview);
        musicplayeruilayout = findViewById(R.id. musicplayeruilayout);
        closeuilayout = findViewById(R.id.closeuilayout);
        uisongtitle = findViewById(R.id.uisongtitle);
        uisongartist = findViewById(R.id.uisongartist);
        elapsedduration = findViewById(R.id.elapsedduration);
        totalduration = findViewById(R.id.totalduration);
        musicplayerseekbar = findViewById(R.id.musicplayerseekbar);
        uishuffleSong = findViewById(R.id.uishuffleSong);
        uiprevSong = findViewById(R.id.uiprevSong);
        uipausePlaySong = findViewById(R.id.uipausePlaySong);
        uinextSong = findViewById(R.id.uinextSong);
        uirepeatSong = findViewById(R.id.uirepeatSong);

        progresslayout.setVisibility(View.INVISIBLE);

        ConstantValues.mainActivity = MainActivity.this;

        if (ContextCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                !=PackageManager.PERMISSION_GRANTED)
            checkPermission();

        songSQLiteDBHelper = new SongSQLiteDBHelper(MainActivity.this);
        cursor = songSQLiteDBHelper.readSongsFromDB();
        if (cursor!=null){
            if (cursor.getCount()>0){
                tracklistrecyclerview.setLayoutManager(new LinearLayoutManager(MainActivity.this));
                myRecyclerViewAdapter = new MyRecyclerViewAdapter(MainActivity.this,cursor);
                tracklistrecyclerview.setAdapter(myRecyclerViewAdapter);
                songscounttextview.setText(""+cursor.getCount());
            }else {
                if (ContextCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        ==PackageManager.PERMISSION_GRANTED)
                    readSongsFromPhone();
            }
        }

        settingsCursor = songSQLiteDBHelper.readSettingsValuesFromDB();
        if (settingsCursor!=null){
            if (settingsCursor.getCount()==0){
                songSQLiteDBHelper.addSettingsValuesToDB("Shuffle","N");
                shuffleSong = "N";
                songSQLiteDBHelper.addSettingsValuesToDB("Repeat","N");
                repeatSong = "N";
                songSQLiteDBHelper.addSettingsValuesToDB("Playing","0");
                settingsCursor = songSQLiteDBHelper.readSettingsValuesFromDB();
            }else {
                settingsCursor.moveToPosition(0);
                shuffleSong = settingsCursor.getString(1);
                if (shuffleSong.equals("Y")){
                    uishuffleSong.setImageResource(R.drawable.ic_baseline_shuffle_selected2_24);
                }
                settingsCursor.moveToPosition(1);
                repeatSong = settingsCursor.getString(1);
                if (repeatSong.equals("Y")){
                    uirepeatSong.setImageResource(R.drawable.ic_baseline_repeat_one_selected2_24);
                }
                settingsCursor.moveToPosition(2);
                ConstantValues.currentSongPosition = Integer.parseInt(settingsCursor.getString(1));
            }
        }

        Intent serviceIntent = new Intent(MainActivity.this, MusicPlayerService.class);
        Intent broadcastIntent = new Intent(this,MusicControlBroadcastReceiver.class);

        progresslayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(MainActivity.this, "Please wait while we scan your phone for songs!!!", Toast.LENGTH_SHORT).show();
            }
        });

        songscantextview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ContextCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        !=PackageManager.PERMISSION_GRANTED){
                    checkPermission();
                    return;
                }
                readSongsFromPhone();
            }
        });

        Handler handler = new Handler();
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                musicplayerseekbar.setProgress(ConstantValues.mediaPlayer.getCurrentPosition()/1000);
                elapsedduration.setText(convertMillisecToMinSec(""+ConstantValues.mediaPlayer.getCurrentPosition(),1));
                handler.postDelayed(this,1000);
            }
        };

        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                cursor = songSQLiteDBHelper.readSongsFromDB();
                if (ContextCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        !=PackageManager.PERMISSION_GRANTED){
                    checkPermission();
                    return;
                }else if (cursor.getCount()==0){
                    readSongsFromPhone();
                    return;
                }
                cursor = songSQLiteDBHelper.readSettingsValuesFromDB();
                if (cursor!=null){
                    cursor.moveToPosition(2);
                    ConstantValues.currentSongPosition = Integer.parseInt(cursor.getString(1));
                }
                cursor = songSQLiteDBHelper.readSongsFromDB();
                if (cursor!=null){
                    getSongDataFromDB(ConstantValues.currentSongPosition);
                    uisongtitle.setText(songData.get(0));
                    uisongartist.setText(songData.get(1));
                    totalduration.setText(songData.get(3));
                }
                musicplayerseekbar.setMax(ConstantValues.mediaPlayer.getDuration()/1000);
                handler.post(runnable);
                ConstantValues.uihandler = handler;
                ConstantValues.uirunnable = runnable;
                musicplayeruilayout.setVisibility(View.VISIBLE);
                floatingActionButton.setVisibility(View.INVISIBLE);
            }
        });

        musicplayerseekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (isMusicServiceStopped()){
                    serviceIntent.setAction("PlaySong");
                    ContextCompat.startForegroundService(MainActivity.this, serviceIntent);
                }else {
                    ConstantValues.remoteViews.setProgressBar(R.id.songplayprogressbar,ConstantValues.mediaPlayer.getDuration()/1000,
                            ConstantValues.mediaPlayer.getCurrentPosition()/1000,false);
                    ConstantValues.notificationManager.notify(404,ConstantValues.notification);
                }
                ConstantValues.mediaPlayer.seekTo(seekBar.getProgress()*1000);
                elapsedduration.setText(convertMillisecToMinSec(""+ConstantValues.mediaPlayer.getCurrentPosition(),1));
            }
        });

        closeuilayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                musicplayeruilayout.setVisibility(View.INVISIBLE);
                floatingActionButton.setVisibility(View.VISIBLE);
                handler.removeCallbacks(runnable);
            }
        });

        uishuffleSong.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isMusicServiceStopped()){
                    serviceIntent.setAction("ShuffleClicked");
                    ContextCompat.startForegroundService(MainActivity.this, serviceIntent);
                }else
                    sendBroadcastForMusic(broadcastIntent,"ShuffleClicked");
            }
        });

        uiprevSong.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isMusicServiceStopped()){
                    serviceIntent.setAction("PrevSong");
                    ContextCompat.startForegroundService(MainActivity.this, serviceIntent);
                }else
                    sendBroadcastForMusic(broadcastIntent,"PrevSong");
            }
        });

        uipausePlaySong.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isMusicServiceStopped()){
                    serviceIntent.setAction("PlayPause");
                    ContextCompat.startForegroundService(MainActivity.this, serviceIntent);
                }else
                    sendBroadcastForMusic(broadcastIntent,"PlayPause");
            }
        });

        uinextSong.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isMusicServiceStopped()){
                    serviceIntent.setAction("NextSong");
                    ContextCompat.startForegroundService(MainActivity.this, serviceIntent);
                }else
                    sendBroadcastForMusic(broadcastIntent,"NextSong");
            }
        });

        uirepeatSong.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isMusicServiceStopped()){
                    serviceIntent.setAction("RepeatClicked");
                    ContextCompat.startForegroundService(MainActivity.this, serviceIntent);
                }else
                    sendBroadcastForMusic(broadcastIntent,"RepeatClicked");
            }
        });

        musicplayeruilayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                System.out.println("musicplayeruilayout is Clicked");
            }
        });
    }

    private boolean isMusicServiceStopped() {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (MusicPlayerService.class.getName().equals(service.service.getClassName())) {
                return false;
            }
        }
        return true;
    }

    public void sendBroadcastForMusic(Intent broadcastIntent, String action) {
        broadcastIntent.setAction(action);
        sendBroadcast(broadcastIntent);
    }

    public void getSongDataFromDB(int position){
        songData.clear();
        if (cursor!=null){
            cursor.moveToPosition(position);
            songData.add(cursor.getString(2));      //0. Song Title
            songData.add(cursor.getString(3));      //1. Song Artist
            songData.add(cursor.getString(6));      //2. Song Path
            songData.add(cursor.getString(5));      //3. Song Duration
        }
    }

    public void readSongsFromPhone(){
        floatingActionButton.setClickable(false);
        floatingActionButton.setVisibility(View.INVISIBLE);
        songTitle.clear();
        songSQLiteDBHelper = new SongSQLiteDBHelper(MainActivity.this);
        songSQLiteDBHelper.deleteSongDatabase();
        progresslayout.setVisibility(View.VISIBLE);
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                cursor = MainActivity.this.getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        songDetails, null, null, MediaStore.Audio.Media.TITLE);
                if(cursor!=null){
                    cursor.moveToPosition(-1);
                    while(cursor.moveToNext()){
                        songTitle.add(cursor.getString(0));
                        songSQLiteDBHelper.addSongsToDB(cursor.getPosition()+1,cursor.getString(0).trim(),cursor.getString(1).trim(),cursor.getString(2).trim(),
                                cursor.getString(3).trim(),convertMillisecToMinSec(cursor.getString(4),0),cursor.getString(5), cursor.getLong(6));
                    }
                }
                Log.e("No. of Songs Found : "+songTitle.size(),"songTitle : "+songTitle);
                cursor = songSQLiteDBHelper.readSongsFromDB();
                tracklistrecyclerview.setLayoutManager(new LinearLayoutManager(MainActivity.this));
                myRecyclerViewAdapter = new MyRecyclerViewAdapter(MainActivity.this,cursor);
                tracklistrecyclerview.setAdapter(myRecyclerViewAdapter);
                songscounttextview.setText(""+songTitle.size());
                Toast.makeText(MainActivity.this, "Songs Scan Completed", Toast.LENGTH_SHORT).show();
                progresslayout.setVisibility(View.INVISIBLE);
                floatingActionButton.setVisibility(View.VISIBLE);
                floatingActionButton.setClickable(true);
            }
        }, 2000);
    }

    public String convertMillisecToMinSec(String milliTime,int source){
        String timeMin,timeSec;
        timeMin = String.valueOf((Long.parseLong(milliTime)/1000)/60);
        timeSec = String.valueOf((Long.parseLong(milliTime)/1000)%60);
        if (timeMin.length()==1)
            timeMin = "0"+timeMin;
        if (timeSec.length()==1)
            timeSec = "0"+timeSec;
        if (source==0){
            if(timeMin.equals("00") && timeSec.equals("00"))
                timeSec = "01";
        }
        return (timeMin+":"+timeSec);
    }

    @Override
    public void updateUIMusicControls(String action) {
        getSongDataFromDB(ConstantValues.currentSongPosition);
        switch (action){
            case "PlaySong":
                uipausePlaySong.setImageResource(R.drawable.ic_baseline_pause_24);
                uisongtitle.setText(songData.get(0));
                uisongartist.setText(songData.get(1));
                totalduration.setText(songData.get(3));
                break;
            case "SongChange":
                uisongtitle.setText(songData.get(0));
                uisongartist.setText(songData.get(1));
                totalduration.setText(songData.get(3));
                break;
            case "PlayPause":
                if (ConstantValues.mediaPlayer.isPlaying()){
                    uipausePlaySong.setImageResource(R.drawable.ic_baseline_pause_24);
                }else {
                    uipausePlaySong.setImageResource(R.drawable.ic_baseline_play_arrow_24);
                }
                break;
            case "ShuffleClicked":
                if (shuffleSong.equals("N")){
                    uishuffleSong.setImageResource(R.drawable.ic_baseline_shuffle_selected2_24);
                    shuffleSong = "Y";
                }else {
                    uishuffleSong.setImageResource(R.drawable.ic_baseline_shuffle_24);
                    shuffleSong = "N";
                }
                break;
            case "RepeatClicked":
                if (repeatSong.equals("N")){
                    uirepeatSong.setImageResource(R.drawable.ic_baseline_repeat_one_selected2_24);
                    repeatSong = "Y";
                }else {
                    uirepeatSong.setImageResource(R.drawable.ic_baseline_repeat_one_24);
                    repeatSong = "N";
                }
                break;
        }
    }

    public void checkPermission(){
        snackbar = Snackbar.make(findViewById(R.id.snackbarlayout),
                "Please allow Permission to read Phone Storage/Memory Card for Songs!!\n\n" +
                        "Click Settings -> Permissions -> Files and media -> Allow access",
                Snackbar.LENGTH_INDEFINITE);
        View snackbarView = snackbar.getView();
        TextView snackbarTextView = snackbarView.findViewById(com.google.android.material.R.id.snackbar_text);
        snackbarTextView.setMaxLines(7);
        snackbar.setAction("SETTINGS", new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.setData(Uri.fromParts("package", getPackageName(), null));
                startActivity(intent);
                snackbar.dismiss();
            }
        });
        snackbar.show();
        ActivityCompat.requestPermissions(MainActivity.this, new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE},
                PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length>0){
            if (grantResults[0]==PackageManager.PERMISSION_GRANTED){
                snackbar.dismiss();
                readSongsFromPhone();
            }
        }
    }
}