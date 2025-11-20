package com.pawansingh.vp.fragments;

import android.app.Activity;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.pawansingh.vp.R;
import com.pawansingh.vp.activities.MainActivity;
import com.pawansingh.vp.adapters.VideoFilesAdapter;
import com.pawansingh.vp.models.MediaFiles;

import java.util.ArrayList;

public class VideosFragment extends Fragment {

    private RecyclerView recyclerView;
    private ArrayList<MediaFiles> mediaFilesArrayList = new ArrayList<>();
    private VideoFilesAdapter adapter;
    private SwipeRefreshLayout swipeRefreshLayout;

    private ActivityResultLauncher<IntentSenderRequest> deleteLauncher;
    private ActivityResultLauncher<IntentSenderRequest> renameLauncher;
    private MediaFiles renameVideo;
    private int renamePosition = -1;
    private String newRenameName = "";

    public VideosFragment() {}

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Register launcher early (before any launch)
        deleteLauncher = registerForActivityResult(new ActivityResultContracts.StartIntentSenderForResult(),
                result -> {
                    // result.getResultCode() corresponds to Activity.RESULT_OK etc.
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        // The system confirmed deletion.
                        // Trigger a refresh (give MediaStore a little time)
                        requireContext().getContentResolver().notifyChange(
                                MediaStore.Video.Media.EXTERNAL_CONTENT_URI, null);

                        // short delay to let MediaStore update
                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            recyclerView.getRecycledViewPool().clear();
                            refreshList();
                        }, 150);
                    } else {
                        // Deletion cancelled or failed
                        Toast.makeText(getContext(), "Video not deleted", Toast.LENGTH_SHORT).show();
                    }

                    // Clear selectedVideo info in MainActivity to keep parity (MainActivity also clears in onActivityResult)
                    Activity act = requireActivity();
                    if (act instanceof MainActivity) {
                        ((MainActivity) act).selectedVideo = null;
                        ((MainActivity) act).selectedPosition = RecyclerView.NO_POSITION;
                    }
                });

        renameLauncher = registerForActivityResult(new ActivityResultContracts.StartIntentSenderForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {

                        // APPLY RENAME IN MediaStore
                        Uri uri = ContentUris.withAppendedId(
                                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                                Long.parseLong(renameVideo.getId())
                        );

                        ContentValues values = new ContentValues();
                        values.put(MediaStore.Video.Media.DISPLAY_NAME, newRenameName);

                        int rows = requireContext().getContentResolver().update(uri, values, null, null);

                        if (rows > 0) {
                            Toast.makeText(getContext(), "Renamed successfully", Toast.LENGTH_SHORT).show();

                            // Update list item and refresh UI
                            renameVideo.setDisplayName(newRenameName);
                            adapter.notifyItemChanged(renamePosition);

                        } else {
                            Toast.makeText(getContext(), "Rename failed", Toast.LENGTH_SHORT).show();
                        }

                    } else {
                        Toast.makeText(getContext(), "Rename cancelled", Toast.LENGTH_SHORT).show();
                    }

                    renameVideo = null;
                    renamePosition = -1;
                    newRenameName = "";
                }
        );
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_videos, container, false);

        recyclerView = view.findViewById(R.id.recyclerView);
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh_fragment_vid);

        showFolders();
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                showFolders();
                swipeRefreshLayout.setRefreshing(false);
            }
        });
        return view;
    }

    private void showFolders() {

        mediaFilesArrayList = fetchVideos();
        adapter = new VideoFilesAdapter(mediaFilesArrayList, getContext(), 0);

        // Set adapter's delete listener so fragment handles delete flow
        adapter.setOnVideoActionListener(new VideoFilesAdapter.OnVideoActionListener() {

            @Override
            public void onRenameRequested(MediaFiles video, int position, String currentName) {
                handleRename(video, position, currentName);
            }
            @Override
            public void onDeleteRequested(MediaFiles video, int position) {
                handleDelete(video, position);
            }
        });


        int spanCount = calculateNoOfColumns(getContext(), 200);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), spanCount));

//        int spacingInPixels = getResources().getDimensionPixelSize(R.dimen.grid_spacing);
//        recyclerView.addItemDecoration(new GridSpacingItemDecoration(spanCount, spacingInPixels, true));

        recyclerView.setAdapter(adapter);
        adapter.notifyDataSetChanged();
    }

    private void refreshList() {
        mediaFilesArrayList.clear();
        mediaFilesArrayList.addAll(fetchVideos());
        adapter.notifyDataSetChanged();
    }

    private void handleRename(MediaFiles video, int position, String currentName) {

        // Show rename dialog
        android.app.AlertDialog.Builder alertDialog = new android.app.AlertDialog.Builder(getContext());
        alertDialog.setTitle("Rename Video");

        final android.widget.EditText input = new android.widget.EditText(getContext());
        input.setText(currentName.replace(".mp4", ""));
        alertDialog.setView(input);

        alertDialog.setPositiveButton("Rename", (dialog, which) -> {
            String newName = input.getText().toString().trim();

            if (newName.isEmpty()) {
                Toast.makeText(getContext(), "Name cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }

            // Apply ".mp4" extension
            newName = newName + ".mp4";

            renameVideo = video;
            renamePosition = position;
            newRenameName = newName;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Uri videoUri = ContentUris.withAppendedId(
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                        Long.parseLong(video.getId())
                );

                ArrayList<Uri> uriList = new ArrayList<>();
                uriList.add(videoUri);

                try {
                    android.app.PendingIntent editIntent =
                            MediaStore.createWriteRequest(
                                    requireContext().getContentResolver(),
                                    uriList
                            );

                    IntentSenderRequest req =
                            new IntentSenderRequest.Builder(editIntent.getIntentSender()).build();

                    renameLauncher.launch(req);

                } catch (Exception e) {
                    Toast.makeText(getContext(), "Rename request failed", Toast.LENGTH_SHORT).show();
                }

            } else {
                // Pre-Android 11 direct rename
                Uri uri = ContentUris.withAppendedId(
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                        Long.parseLong(video.getId())
                );

                ContentValues values = new ContentValues();
                values.put(MediaStore.Video.Media.DISPLAY_NAME, newName);

                int rows = requireContext().getContentResolver().update(uri, values, null, null);

                if (rows > 0) {
                    video.setDisplayName(newName);
                    adapter.notifyItemChanged(position);
                    Toast.makeText(getContext(), "Renamed successfully", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getContext(), "Rename failed", Toast.LENGTH_SHORT).show();
                }
            }
        });

        alertDialog.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        alertDialog.show();
    }

    private void handleDelete(MediaFiles video, int position) {
        // Show delete dialog
        // Save selection in MainActivity for compatibility (MainActivity expects this)
        if (requireActivity() instanceof MainActivity) {
            ((MainActivity) requireActivity()).prepareForDelete(video, position);
        }

        // For Android R+ use createDeleteRequest
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Uri contentUri = ContentUris.withAppendedId(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    Long.parseLong(video.getId())
            );

            ArrayList<Uri> uriList = new ArrayList<>();
            uriList.add(contentUri);

            android.app.PendingIntent deleteIntent = MediaStore.createDeleteRequest(
                    requireContext().getContentResolver(), uriList);

            try {
                IntentSenderRequest req = new IntentSenderRequest.Builder(deleteIntent.getIntentSender()).build();
                deleteLauncher.launch(req);
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(getContext(), "Could not start delete request", Toast.LENGTH_SHORT).show();
            }
        } else {
            // Pre-R: perform immediate delete and refresh
            Uri contentUri = ContentUris.withAppendedId(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    Long.parseLong(video.getId())
            );
            int rowsDeleted = requireContext().getContentResolver().delete(contentUri, null, null);
            if (rowsDeleted > 0) {
                // immediate refresh
                recyclerView.getRecycledViewPool().clear();
                refreshList();
                Toast.makeText(getContext(), "Video deleted successfully", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "Video not deleted", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private int calculateNoOfColumns(Context context, float columnWidthDp) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        float screenWidthDp = displayMetrics.widthPixels / displayMetrics.density;
        int noOfColumns = (int) (screenWidthDp / columnWidthDp + 0.5); // +0.5 for rounding
        return noOfColumns >= 2 ? noOfColumns : 2; // minimum 2 columns
    }

    private ArrayList<MediaFiles> fetchVideos() {

        ArrayList<MediaFiles> list = new ArrayList<>();
        Uri uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        String orderBy = MediaStore.Video.Media.DATE_ADDED + " DESC";
        Cursor cursor = getContext().getContentResolver().query(
                uri,
                null,
                null,
                null,
                orderBy
        );

        if (cursor != null && cursor.moveToFirst()) {
            do {
                String id = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media._ID));
                String title = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.TITLE));
                String displayName = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME));
                String size = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.SIZE));
                String duration = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.DURATION));
                String path = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.DATA));
                String dateAdded = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.DATE_ADDED));

                MediaFiles mediaFile = new MediaFiles(id, title, displayName, size, duration, path, dateAdded);
                list.add(mediaFile);

            } while (cursor.moveToNext());

            cursor.close();
        }
        return list;
    }

    public void updateRenamedItem(MediaFiles renamedVideo, int position) {
        if (mediaFilesArrayList != null && position >= 0 && position < mediaFilesArrayList.size()) {
            mediaFilesArrayList.set(position, renamedVideo);
            adapter.notifyItemChanged(position);
        }
    }
}