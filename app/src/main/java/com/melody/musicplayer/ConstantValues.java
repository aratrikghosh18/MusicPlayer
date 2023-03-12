package com.melody.musicplayer;

import android.app.Notification;
import android.app.NotificationManager;
import android.media.MediaPlayer;
import android.os.Handler;
import android.widget.RemoteViews;

public class ConstantValues {
    public static MediaPlayer mediaPlayer = new MediaPlayer();
    public static int currentSongPosition = 0;
    public static RemoteViews remoteViews;
    public static int notificationID = 404;
    public static Notification notification;
    public static NotificationManager notificationManager;
    public static int recyclerViewItemCount = 0;
    public static Handler handler,uihandler;
    public static Runnable runnable,uirunnable;
    public static MainActivity mainActivity;
}