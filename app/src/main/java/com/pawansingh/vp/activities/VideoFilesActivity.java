package com.pawansingh.vp.activities;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.SearchView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.pawansingh.vp.R;
import com.pawansingh.vp.adapters.VideoDeleteListener;
import com.pawansingh.vp.adapters.VideoFilesAdapter;
import com.pawansingh.vp.models.MediaFiles;
import com.pawansingh.vp.newthing.FoldersFragment;
import com.pawansingh.vp.newthing.MediaActionHandler;

import java.util.ArrayList;
import java.util.Objects;

public class VideoFilesActivity extends AppCompatActivity implements SearchView.OnQueryTextListener , MediaActionHandler {

    public MediaFiles selectedVideo = null;
    public int selectedPosition = RecyclerView.NO_POSITION;
    private ActivityResultLauncher<IntentSenderRequest> deleteVideoLauncher;

    private MediaFiles videoToRename;
    private int renamePosition;


    public static final String MY_PREF = "my pref";
    RecyclerView recyclerView;
    String folder_name;
    SwipeRefreshLayout swipeRefreshLayout;
    String sortOrder;
    private ArrayList<MediaFiles> videoFilesArrayList = new ArrayList<>();
    public static VideoFilesAdapter videoFilesAdapter;
    Toolbar toolbar;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_files);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_back);
        toolbar.setNavigationOnClickListener(view -> finish());

        deleteVideoLauncher = registerForActivityResult(
                new ActivityResultContracts.StartIntentSenderForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        if (selectedVideo != null && selectedPosition != RecyclerView.NO_POSITION) {
                            videoFilesArrayList.remove(selectedPosition);
                            videoFilesAdapter.notifyItemRemoved(selectedPosition);
                            Toast.makeText(this, "Video deleted successfully", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this, "Video not deleted", Toast.LENGTH_SHORT).show();
                    }

                    selectedVideo = null;
                    selectedPosition = RecyclerView.NO_POSITION;
                }
        );

        folder_name = getIntent().getStringExtra("folderName");
        Objects.requireNonNull(getSupportActionBar()).setTitle(folder_name);  //L-7
        recyclerView = findViewById(R.id.videos_rv);
        swipeRefreshLayout = findViewById(R.id.swipe_refresh_videos);

        SharedPreferences.Editor editor = getSharedPreferences(MY_PREF,MODE_PRIVATE).edit();
        editor.putString("playlistFolderName",folder_name);
        editor.apply();

        showVideoFiles();
        swipeRefreshLayout.setOnRefreshListener(() -> {
            showVideoFiles();
            swipeRefreshLayout.setRefreshing(false);
        });
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

    }

    @SuppressLint("NotifyDataSetChanged")
    private void showVideoFiles() {
        videoFilesArrayList = fetchMedia(folder_name);
        videoFilesAdapter = new VideoFilesAdapter(videoFilesArrayList, this, 0);

        int noOfColumns = calculateNoOfColumns(this, 180);
        GridLayoutManager gridLayoutManager = new GridLayoutManager(this, noOfColumns); // 2 columns
        recyclerView.setLayoutManager(gridLayoutManager);

        recyclerView.setAdapter(videoFilesAdapter);
        videoFilesAdapter.notifyDataSetChanged();
        videoFilesAdapter.setDeleteListener(new VideoDeleteListener() {
            @Override
            public void onDeleteRequested(MediaFiles video, int position) {
                Uri contentUri = ContentUris.withAppendedId(
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                        Long.parseLong(video.getId())
                );

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    ArrayList<Uri> uriList = new ArrayList<>();
                    uriList.add(contentUri);
                    PendingIntent deleteIntent = MediaStore.createDeleteRequest(getContentResolver(), uriList);

                    try {
                        startIntentSenderForResult(
                                deleteIntent.getIntentSender(),
                                108, null, 0, 0, 0
                        );
                        selectedVideo = video;
                        selectedPosition = position;
                    } catch (IntentSender.SendIntentException e) {
                        e.printStackTrace();
                    }

                } else {
                    int rowsDeleted = getContentResolver().delete(contentUri, null, null);
                    if (rowsDeleted > 0) {
                        videoFilesArrayList.remove(position);
                        videoFilesAdapter.notifyItemRemoved(position);
                        Toast.makeText(VideoFilesActivity.this, "Video deleted successfully", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(VideoFilesActivity.this, "Video not deleted", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
    }

    private int calculateNoOfColumns(Context context, float columnWidthDp) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        float screenWidthDp = displayMetrics.widthPixels / displayMetrics.density;
        int noOfColumns = (int) (screenWidthDp / columnWidthDp + 0.5); // +0.5 for rounding
        return noOfColumns >= 2 ? noOfColumns : 2; // minimum 2 columns
    }

    private ArrayList<MediaFiles> fetchMedia(String folderName) {

        SharedPreferences preferences =getSharedPreferences(MY_PREF,MODE_PRIVATE);
        String sort_value = preferences.getString("sort","abcd");

        ArrayList<MediaFiles> videoFiles = new ArrayList<>();
        Uri uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        if (sort_value.equals("sortName")){
            sortOrder = MediaStore.MediaColumns.DISPLAY_NAME+" ASC";
        } else if (sort_value.equals("sortSize")) {
            sortOrder = MediaStore.MediaColumns.SIZE+" DESC";
        } else if (sort_value.equals("sortDate")) {
            sortOrder =MediaStore.MediaColumns.DATE_ADDED+" DESC";
        }else {
            sortOrder = MediaStore.Video.Media.DURATION+" DESC";
        }

        String selection = MediaStore.Video.Media.DATA+" like?";
        String[] selectionArg = new String[]{"%"+folderName+"%"};
        Cursor cursor = getContentResolver().query(uri,null,
                selection, selectionArg,sortOrder);
        if (cursor!=null && cursor.moveToNext()){
            do {
                String id = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media._ID));
                String title = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.TITLE));
                String displayName = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME));
                String size = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.SIZE));
                String duration = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.DURATION));
                String path = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.DATA));
                String dateAdded = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.DATE_ADDED));

                MediaFiles mediaFiles = new MediaFiles(id,title,displayName,size,duration,path, dateAdded);

                videoFiles.add(mediaFiles);

            }while (cursor.moveToNext());
            cursor.close();
        }
        return videoFiles;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.video_menu,menu);
        MenuItem menuItem = menu.findItem(R.id.search_video);
        SearchView searchView = (SearchView) menuItem.getActionView();
        searchView.setOnQueryTextListener(this);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        SharedPreferences preferences = getSharedPreferences(MY_PREF,MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();

        int id = item.getItemId();
        if (id == R.id.refresh_files){
            finish();
            startActivity(getIntent());
        } else if (id == R.id.sort_by) {
            AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
            alertDialog.setTitle("Sort by");

            alertDialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    editor.apply();
                    finish();
                    startActivity(getIntent());
                    dialogInterface.dismiss();
                }
            });
            String[] items = {"Name (A to Z)","Size (Large to Small)","Date (New to Old)","Length (Long to Short)"};
            alertDialog.setSingleChoiceItems(items, 2, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    switch (i){
                        case 0:
                            editor.putString("sort","sortName");
                            break;
                        case 1:
                            editor.putString("sort","sortSize");
                            break;
                        case 2:
                            editor.putString("sort","sortDate");
                            break;
                        case 3:
                            editor.putString("sort","sortLength");
                            break;
                    }
                }
            });
            alertDialog.create().show();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onQueryTextSubmit(String s) {
        return false;
    }

    @Override
    public boolean onQueryTextChange(String s) {
        String inputs = s.toLowerCase();
        ArrayList<MediaFiles> mediaFiles = new ArrayList<>();
        for (MediaFiles media:videoFilesArrayList){
            if (media.getTitle().toLowerCase().contains(inputs)){
                mediaFiles.add(media);
            }
        }
        VideoFilesActivity.videoFilesAdapter.updateVideoFiles(mediaFiles);
        return true;
    }

    public void prepareForDelete(MediaFiles video, int position) {
        selectedVideo = video;
        selectedPosition = position;
    }


    public void prepareForRename(MediaFiles video, int position, String newName) {
        this.videoToRename = new MediaFiles(
                video.getId(), video.getTitle(), newName,
                video.getSize(), video.getDuration(),
                video.getPath(), video.getDateAdded()
        );
        this.renamePosition = position;
    }

    public void updateRenamedItem(MediaFiles renamedVideo, int position) {
        FoldersFragment fragment = (FoldersFragment) getSupportFragmentManager()
                .findFragmentByTag("your_fragment_tag"); // or by id if using container
        if (fragment != null) {
            fragment.updateRenamedItem(renamedVideo, position);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 109) { // Rename request code
            if (resultCode == RESULT_OK && videoToRename != null) {
                // Now actually perform the rename
                Uri contentUri = ContentUris.withAppendedId(
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                        Long.parseLong(videoToRename.getId())
                );

                ContentValues values = new ContentValues();
                values.put(MediaStore.Video.Media.DISPLAY_NAME, videoToRename.getDisplayName());

                try {
                    int rowsUpdated = getContentResolver().update(contentUri, values, null, null);
                    if (rowsUpdated > 0) {
                        // Update in our list
                        videoFilesArrayList.set(renamePosition, videoToRename);
                        videoFilesAdapter.notifyItemChanged(renamePosition);
                        Toast.makeText(this, "Video renamed successfully", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Rename failed", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Rename cancelled", Toast.LENGTH_SHORT).show();
            }
            videoToRename = null;
        }
    }
}



