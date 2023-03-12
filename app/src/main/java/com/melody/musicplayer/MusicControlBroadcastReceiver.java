package com.melody.musicplayer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

public class MusicControlBroadcastReceiver extends BroadcastReceiver {

    Context mContext;
    SongSQLiteDBHelper songSQLiteDBHelper;
    Cursor cursor,settingsCursor;
    ArrayList<String> songDetails;
    String shuffleSong, repeatSong;
    Random random;
    BroadcastListenerInterface broadcastListenerInterface;

    @Override
    public void onReceive(Context context, Intent intent) {
        mContext = context;
        songSQLiteDBHelper = new SongSQLiteDBHelper(mContext);
        cursor = songSQLiteDBHelper.readSongsFromDB();
        settingsCursor = songSQLiteDBHelper.readSettingsValuesFromDB();
        settingsCursor.moveToPosition(0);
        shuffleSong = settingsCursor.getString(1);
        settingsCursor.moveToPosition(1);
        repeatSong = settingsCursor.getString(1);
        songDetails = new ArrayList<>();
        random = new Random();
        String musicControlClicked = intent.getAction();
        broadcastListenerInterface = ConstantValues.mainActivity;
        switch (musicControlClicked){
            case "PlaySong":
                getSongDataFromDB(ConstantValues.currentSongPosition);
                try {
                    playSongFile(songDetails.get(2));
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (broadcastListenerInterface!=null){
                    broadcastListenerInterface.updateUIMusicControls("PlaySong");
                }
                break;
            case "PrevSong":
                try {
                    playPreviousOrNextSong(0);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (broadcastListenerInterface!=null){
                    broadcastListenerInterface.updateUIMusicControls("SongChange");
                }
                break;
            case "PlayPause":
                playPauseSong();
                if (broadcastListenerInterface!=null){
                    broadcastListenerInterface.updateUIMusicControls("PlayPause");
                }
                break;
            case "NextSong":
                try {
                    playPreviousOrNextSong(1);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (broadcastListenerInterface!=null){
                    broadcastListenerInterface.updateUIMusicControls("SongChange");
                }
                break;
            case "ShuffleClicked":
                settingsCursor.moveToPosition(0);
                if (shuffleSong.equals("N")){
                    songSQLiteDBHelper.updateSettingsValue("Shuffle","Y");
                    ConstantValues.remoteViews.setImageViewResource(R.id.shuffleSong,R.drawable.ic_baseline_shuffle_selected_24);
                    shuffleSong = "Y";
                }else {
                    songSQLiteDBHelper.updateSettingsValue("Shuffle","N");
                    ConstantValues.remoteViews.setImageViewResource(R.id.shuffleSong,R.drawable.ic_baseline_shuffle_24);
                    shuffleSong = "N";
                }
                ConstantValues.notificationManager.notify(404,ConstantValues.notification);
                if (broadcastListenerInterface!=null){
                    broadcastListenerInterface.updateUIMusicControls("ShuffleClicked");
                }
                break;
            case "RepeatClicked":
                settingsCursor.moveToPosition(1);
                if (repeatSong.equals("N")){
                    songSQLiteDBHelper.updateSettingsValue("Repeat","Y");
                    ConstantValues.remoteViews.setImageViewResource(R.id.repeatSong,R.drawable.ic_baseline_repeat_one_selected_24);
                    repeatSong = "Y";
                }else {
                    songSQLiteDBHelper.updateSettingsValue("Repeat","N");
                    ConstantValues.remoteViews.setImageViewResource(R.id.repeatSong,R.drawable.ic_baseline_repeat_one_24);
                    repeatSong = "N";
                }
                ConstantValues.notificationManager.notify(404,ConstantValues.notification);
                if (broadcastListenerInterface!=null){
                    broadcastListenerInterface.updateUIMusicControls("RepeatClicked");
                }
                break;
            case "AutoNextSong":
                if (repeatSong.equals("Y"))
                    ConstantValues.mediaPlayer.start();
                else {
                    try {
                        playPreviousOrNextSong(1);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (broadcastListenerInterface!=null){
                    broadcastListenerInterface.updateUIMusicControls("SongChange");
                }
                break;
            case "CloseNotification":
                closeNotification();
                if (broadcastListenerInterface!=null){
                    broadcastListenerInterface.updateUIMusicControls("PlayPause");
                }
                break;
        }
    }

    public void playPreviousOrNextSong(int selection) throws IOException {
        if(shuffleSong.equals("Y")){
            ConstantValues.currentSongPosition = random.nextInt(ConstantValues.recyclerViewItemCount);
        }else {
            if (selection==0){
                if (ConstantValues.currentSongPosition ==0){
                    ConstantValues.currentSongPosition = ConstantValues.recyclerViewItemCount-1;
                }else
                    ConstantValues.currentSongPosition--;
            }
            else{
                if (ConstantValues.currentSongPosition ==ConstantValues.recyclerViewItemCount-1){
                    ConstantValues.currentSongPosition = 0;
                }else
                    ConstantValues.currentSongPosition++;
            }
        }
        getSongDataFromDB(ConstantValues.currentSongPosition);
        ConstantValues.mediaPlayer.reset();
        ConstantValues.mediaPlayer.setDataSource(songDetails.get(2));
        ConstantValues.mediaPlayer.prepare();
        ConstantValues.mediaPlayer.start();
        songSQLiteDBHelper.updateSettingsValue("Playing", String.valueOf(ConstantValues.currentSongPosition));
        ConstantValues.remoteViews.setImageViewResource(R.id.pausePlaySong,R.drawable.ic_baseline_pause_24);
        ConstantValues.remoteViews.setTextViewText(R.id.songtitle,songDetails.get(0));
        ConstantValues.remoteViews.setTextViewText(R.id.songartist,songDetails.get(1));
        ConstantValues.notificationManager.notify(404,ConstantValues.notification);
    }

    public void playPauseSong(){
        if (ConstantValues.mediaPlayer.isPlaying()){
            ConstantValues.mediaPlayer.pause();
            ConstantValues.remoteViews.setImageViewResource(R.id.pausePlaySong,R.drawable.ic_baseline_play_arrow_24);
        }
        else{
            ConstantValues.mediaPlayer.start();
            ConstantValues.remoteViews.setImageViewResource(R.id.pausePlaySong,R.drawable.ic_baseline_pause_24);
        }
        ConstantValues.notificationManager.notify(404,ConstantValues.notification);
    }

    public void closeNotification(){
        ConstantValues.mediaPlayer.stop();
        Intent serviceIntent = new Intent(mContext, MusicPlayerService.class);
        mContext.stopService(serviceIntent);
        ConstantValues.handler.removeCallbacks(ConstantValues.runnable);
        if (ConstantValues.uihandler!=null && ConstantValues.uirunnable!=null)
            ConstantValues.uihandler.removeCallbacks(ConstantValues.uirunnable);
        ConstantValues.notificationManager.cancel(ConstantValues.notificationID);
    }

    public void getSongDataFromDB(int position){
        songDetails.clear();
        if (cursor!=null){
            cursor.moveToPosition(position);
            songDetails.add(cursor.getString(2));      //0. Song Title
            songDetails.add(cursor.getString(3));      //1. Song Artist
            songDetails.add(cursor.getString(6));      //2. Song Path
            songDetails.add(cursor.getString(5));      //3. Song Duration
        }
    }

    public void playSongFile(String songPathFromDB) throws IOException {
        ConstantValues.mediaPlayer.reset();
        ConstantValues.mediaPlayer.setDataSource(songPathFromDB);
        ConstantValues.mediaPlayer.prepare();
        ConstantValues.mediaPlayer.start();
        songSQLiteDBHelper.updateSettingsValue("Playing", String.valueOf(ConstantValues.currentSongPosition));
    }
}
