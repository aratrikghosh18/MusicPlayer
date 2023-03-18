package com.melody.musicplayer;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.widget.RemoteViews;

import java.util.ArrayList;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

public class MusicPlayerService extends Service {

    public static final String CHANNEL_ID = "SongServiceChannel";
    SongSQLiteDBHelper songSQLiteDBHelper;
    Cursor cursor;
    ArrayList<String> songDetails;
    ArrayList<String> settingsValue;
    NotificationManager manager;
    Context mContext;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        songSQLiteDBHelper = new SongSQLiteDBHelper(getApplication().getApplicationContext());
        cursor = songSQLiteDBHelper.readSettingsValuesFromDB();
        if (cursor!=null){
            cursor.moveToPosition(2);
            ConstantValues.currentSongPosition = Integer.parseInt(cursor.getString(1));
        }
        cursor = songSQLiteDBHelper.readSongsFromDB();
        songDetails = new ArrayList<>();
        settingsValue = new ArrayList<>();
        mContext = this;
        getSongDataFromDB(ConstantValues.currentSongPosition);
        createNotificationChannel();
        Intent broadcastIntent = new Intent(this,MusicControlBroadcastReceiver.class);
        cursor = songSQLiteDBHelper.readSettingsValuesFromDB();
        if (cursor!=null){
            cursor.moveToPosition(-1);
            while (cursor.moveToNext()){
                settingsValue.add(cursor.getString(1));
            }
        }
        RemoteViews remoteViews = new RemoteViews(getApplicationContext().getPackageName(),R.layout.notification_layout);
        remoteViews.setTextViewText(R.id.songtitle,songDetails.get(0));
        remoteViews.setTextViewText(R.id.songartist,songDetails.get(1));
        if (settingsValue.get(0).equals("N"))
            remoteViews.setImageViewResource(R.id.shuffleSong,R.drawable.ic_baseline_shuffle_24);
        else
            remoteViews.setImageViewResource(R.id.shuffleSong,R.drawable.ic_baseline_shuffle_selected_24);
        if (settingsValue.get(1).equals("N"))
            remoteViews.setImageViewResource(R.id.repeatSong,R.drawable.ic_baseline_repeat_one_24);
        else
            remoteViews.setImageViewResource(R.id.repeatSong,R.drawable.ic_baseline_repeat_one_selected_24);
        remoteViews.setOnClickPendingIntent(R.id.prevSong,getPendingSelfIntent(this, broadcastIntent,"PrevSong"));
        remoteViews.setOnClickPendingIntent(R.id.pausePlaySong,getPendingSelfIntent(this, broadcastIntent,"PlayPause"));
        remoteViews.setOnClickPendingIntent(R.id.nextSong,getPendingSelfIntent(this, broadcastIntent,"NextSong"));
        remoteViews.setOnClickPendingIntent(R.id.closeNotification,getPendingSelfIntent(this,broadcastIntent,"CloseNotification"));
        remoteViews.setOnClickPendingIntent(R.id.shuffleSong,getPendingSelfIntent(this,broadcastIntent,"ShuffleClicked"));
        remoteViews.setOnClickPendingIntent(R.id.repeatSong,getPendingSelfIntent(this,broadcastIntent,"RepeatClicked"));
        remoteViews.setProgressBar(R.id.songplayprogressbar,ConstantValues.mediaPlayer.getDuration()/1000,
                ConstantValues.mediaPlayer.getCurrentPosition()/1000,false);

        ConstantValues.remoteViews = remoteViews;

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this,CHANNEL_ID)
                .setSmallIcon(R.drawable.baseline_music_note_24)
                .setContent(remoteViews);
        Notification notification = mBuilder.build();
        notification.deleteIntent = getPendingSelfIntent(this,broadcastIntent,"CloseNotification");
        ConstantValues.notification = notification;
        startForeground(ConstantValues.notificationID, notification);

        ConstantValues.mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                Intent intent = new Intent(mContext,MusicControlBroadcastReceiver.class);
                intent.setAction("AutoNextSong");
                sendBroadcast(intent);
            }
        });

        Handler handler = new Handler();
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                remoteViews.setProgressBar(R.id.songplayprogressbar,ConstantValues.mediaPlayer.getDuration()/1000,
                        ConstantValues.mediaPlayer.getCurrentPosition()/1000,false);
                ConstantValues.notificationManager.notify(404,ConstantValues.notification);
                handler.postDelayed(this,1000);
            }
        };
        handler.post(runnable);
        ConstantValues.handler = handler;
        ConstantValues.runnable = runnable;

        broadcastIntent.setAction(intent.getAction());
        sendBroadcast(broadcastIntent);

        return START_NOT_STICKY;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Foreground Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT);
            serviceChannel.setSound(null,null);
            manager = getSystemService(NotificationManager.class);
            ConstantValues.notificationManager = manager;
            manager.createNotificationChannel(serviceChannel);
        }
    }

    private PendingIntent getPendingSelfIntent(Context context, Intent broadcastIntent, String action) {
        broadcastIntent.setAction(action);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            return PendingIntent.getBroadcast(context, 0, broadcastIntent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        else
            return PendingIntent.getBroadcast(context, 0, broadcastIntent, PendingIntent.FLAG_UPDATE_CURRENT);
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
}
