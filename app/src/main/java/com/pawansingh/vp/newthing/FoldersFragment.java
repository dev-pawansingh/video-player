package com.pawansingh.vp.newthing;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.pawansingh.vp.R;
import com.pawansingh.vp.adapters.VideoFoldersAdapter;
import com.pawansingh.vp.models.MediaFiles;

import java.util.ArrayList;

public class FoldersFragment extends Fragment {

    private RecyclerView recyclerView;
    private ArrayList<MediaFiles> mediaFilesArrayList = new ArrayList<>();
    private final ArrayList<String> allFolderList = new ArrayList<>();
    private VideoFoldersAdapter adapter;
    private SwipeRefreshLayout swipeRefreshLayout;

    public FoldersFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_folders, container, false);
        recyclerView = view.findViewById(R.id.recyclerView);
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh_fragment_fldr);

        mediaFilesArrayList = fetchMedia();
        adapter = new VideoFoldersAdapter(mediaFilesArrayList, allFolderList, getContext());
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                mediaFilesArrayList = fetchMedia();
                adapter = new VideoFoldersAdapter(mediaFilesArrayList, allFolderList, getContext());
                recyclerView.setAdapter(adapter);
                recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
                swipeRefreshLayout.setRefreshing(false);
            }
        });
        return view;
    }

    private ArrayList<MediaFiles> fetchMedia() {
        ArrayList<MediaFiles> list = new ArrayList<>();
        Uri uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;

        Cursor cursor = getContext().getContentResolver().query(uri, null, null, null, null);
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

                int index = path.lastIndexOf("/");
                String folderPath = path.substring(0, index);
                // 🚨 Skip /storage/emulated/0 folder
                if (folderPath.equals("/storage/emulated/0")) {
                    continue;
                }

                // Add folder to folder list if not already added
                if (!allFolderList.contains(folderPath)) {
                    allFolderList.add(folderPath);
                }

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