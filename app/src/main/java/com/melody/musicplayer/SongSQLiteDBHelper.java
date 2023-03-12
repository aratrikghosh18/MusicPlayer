package com.melody.musicplayer;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

public class SongSQLiteDBHelper extends SQLiteOpenHelper {

    //Song Table
    public static final String SONG_DB_Name = "Songs_Database.sqlite";
    public static final int SONG_DB_Version = 1;
    public static final String SONG_TABLE_NAME = "Songs_Table";
    public static final String SONG_ID = "ID";
    public static final String SONG_TITLE = "TITLE";
    public static final String SONG_DISPLAY_NAME = "DISPLAY_NAME";
    public static final String SONG_ARTIST = "ARTIST";
    public static final String SONG_ALBUM = "ALBUM";
    public static final String SONG_DURATION = "DURATION";
    public static final String SONG_FILE_PATH = "FILE_PATH";
    public static final String SONG_ALBUM_ID = "ALBUM_ID";

    //Settings Table
    public static final String SETTINGS_TABLE_NAME = "Settings_Table";
    public static final String SETTINGS_NAME = "Settings_Name";
    public static final String SETTINGS_VALUE = "Settings_Value";

    SQLiteDatabase sqLiteDatabase;
    ContentValues contentValues;

    public SongSQLiteDBHelper(@Nullable Context context) {
        super(context, SONG_DB_Name, null, SONG_DB_Version);
        sqLiteDatabase = this.getWritableDatabase();
        contentValues = new ContentValues();
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        String query = "CREATE TABLE " + SONG_TABLE_NAME + " ("
                + SONG_ID + " INTEGER, "
                + SONG_TITLE + " TEXT,"
                + SONG_DISPLAY_NAME + " TEXT,"
                + SONG_ARTIST + " TEXT,"
                + SONG_ALBUM + " TEXT,"
                + SONG_DURATION + " TEXT,"
                + SONG_FILE_PATH + " TEXT,"
                + SONG_ALBUM_ID + " INTEGER)";
        String settingsQuery =  "CREATE TABLE " + SETTINGS_TABLE_NAME + " ("
                + SETTINGS_NAME + " TEXT,"
                + SETTINGS_VALUE + " TEXT)";
        sqLiteDatabase.execSQL(query);
        sqLiteDatabase.execSQL(settingsQuery);
    }

    public void addSongsToDB(int songID, String songTitle, String songDisplayName, String songArtist, String songAlbum,
                             String songDuration, String songFilePath, Long songAlbumID){
        contentValues.clear();
        contentValues.put(SONG_ID,songID);                                                          //0
        contentValues.put(SONG_TITLE,songTitle);                                                //1
        contentValues.put(SONG_DISPLAY_NAME,songDisplayName);                   //2
        contentValues.put(SONG_ARTIST,songArtist);                                            //3
        contentValues.put(SONG_ALBUM,songAlbum);                                             //4
        contentValues.put(SONG_DURATION,songDuration);                                  //5
        contentValues.put(SONG_FILE_PATH,songFilePath);                                   //6
        contentValues.put(SONG_ALBUM_ID,songAlbumID);                                   //7

        sqLiteDatabase.insert(SONG_TABLE_NAME,null,contentValues);
    }

    public Cursor readSongsFromDB(){
        return sqLiteDatabase.rawQuery("SELECT * FROM "+SONG_TABLE_NAME,null);
    }

    public void deleteSongDatabase(){
        sqLiteDatabase.delete(SONG_TABLE_NAME,null,null);
    }

    public Cursor readSettingsValuesFromDB(){
        return sqLiteDatabase.rawQuery("SELECT * FROM "+SETTINGS_TABLE_NAME,null);
    }

    public void addSettingsValuesToDB(String settingsName,String settingsValue){
        contentValues.clear();
        contentValues.put(SETTINGS_NAME,settingsName);                                                 //0
        contentValues.put(SETTINGS_VALUE,settingsValue);                                                 //1

        sqLiteDatabase.insert(SETTINGS_TABLE_NAME,null,contentValues);
    }

    public void updateSettingsValue(String settingsName,String settingsValue){
        contentValues.clear();
        contentValues.put(SETTINGS_NAME,settingsName);                                                 //0
        contentValues.put(SETTINGS_VALUE,settingsValue);                                                 //1

        sqLiteDatabase.update(SETTINGS_TABLE_NAME,contentValues,"SETTINGS_NAME = ?",new String[]{settingsName});
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + SONG_TABLE_NAME);
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + SETTINGS_TABLE_NAME);
        onCreate(sqLiteDatabase);
    }
}
