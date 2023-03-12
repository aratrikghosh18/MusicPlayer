package com.melody.musicplayer;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

public class MyRecyclerViewAdapter extends RecyclerView.Adapter<MyRecyclerViewAdapter.ViewHolder> {

    private final Cursor mCursor;
    private final LayoutInflater mInflater;
    private final Context mContext;

    MyRecyclerViewAdapter(Context context, Cursor cursor) {
        this.mInflater = LayoutInflater.from(context);
        this.mCursor = cursor;
        this.mContext = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.tracklistrecyclerview_field, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, @SuppressLint("RecyclerView") int position) {
        mCursor.moveToPosition(position);
        holder.songlisttitleview.setText(mCursor.getString(1));
        holder.songlistartistview.setText(mCursor.getString(3));
        holder.songlistduration.setText(mCursor.getString(5));
        holder.songlistalbumart.setImageResource(R.drawable.ic_baseline_audiotrack_24);
        ConstantValues.recyclerViewItemCount = mCursor.getCount();
    }

    @Override
    public int getItemCount() {
        return mCursor.getCount();
    }

    public class ViewHolder extends RecyclerView.ViewHolder{
        ImageView songlistalbumart;
        TextView songlisttitleview, songlistartistview,songlistduration;
        Button recyclerviewclicklbutton;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            songlistalbumart = itemView.findViewById(R.id.songlistalbumart);
            songlisttitleview = itemView.findViewById(R.id.songlisttitleview);
            songlistartistview = itemView.findViewById(R.id.songlistartistview);
            songlistduration = itemView.findViewById(R.id.songlistduration);
            recyclerviewclicklbutton = itemView.findViewById(R.id.recyclerviewclicklbutton);
            recyclerviewclicklbutton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent serviceIntent = new Intent(mContext, MusicPlayerService.class);
                    ConstantValues.currentSongPosition = getAdapterPosition();
                    SongSQLiteDBHelper songSQLiteDBHelper = new SongSQLiteDBHelper(mContext);
                    songSQLiteDBHelper.updateSettingsValue("Playing", String.valueOf(ConstantValues.currentSongPosition));
                    serviceIntent.setAction("PlaySong");
                    ContextCompat.startForegroundService(mContext, serviceIntent);
                }
            });
        }
    }
}
