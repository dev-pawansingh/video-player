package com.pawansingh.vp.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.pawansingh.vp.R;
import com.pawansingh.vp.activities.VideoFilesActivity;
import com.pawansingh.vp.models.MediaFiles;

import java.io.File;
import java.util.ArrayList;

public class VideoFoldersAdapter extends RecyclerView.Adapter<VideoFoldersAdapter.ViewHolder> {

    private final ArrayList<MediaFiles> mediaFiles;
    private final ArrayList<String> folderPath;
    private final Context context;

    public VideoFoldersAdapter(ArrayList<MediaFiles> mediaFiles, ArrayList<String> folderPath, Context context) {
        this.mediaFiles = mediaFiles;
        this.folderPath = folderPath;
        this.context = context;
    }

    @NonNull
    @Override
    public VideoFoldersAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.folder_item,parent,false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VideoFoldersAdapter.ViewHolder holder, int position) {

        int indexPath = folderPath.get(position).lastIndexOf("/");
        String nameOfFolder = folderPath.get(position).substring(indexPath+1);
        holder.folderName.setText(nameOfFolder);
        holder.folderPath.setText(folderPath.get(position));
        //holder.noOfFiles.setText(noOfFolders(folderPath.get(position))+" Videos");
        holder.noOfFiles.setText(noOfFolders(folderPath.get(position)) + " Videos");

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(context, VideoFilesActivity.class);
                intent.putExtra("folderName",nameOfFolder);
                context.startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return folderPath.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView folderName,folderPath,noOfFiles;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            folderName = itemView.findViewById(R.id.foldername);
            folderPath = itemView.findViewById(R.id.folderpath);
            noOfFiles = itemView.findViewById(R.id.noOfFiles);
        }
    }
    int noOfFolders(String folder_name) {
        int files_no = 0;
        for (MediaFiles mf : mediaFiles) {
            if (mf == null) continue;
            String path = mf.getPath(); // use getter (Kotlin -> Java)
            if (path == null || path.isEmpty()) continue;
            File f = new File(path);
            String parent = f.getParent(); // safer than substring
            if (parent != null && parent.endsWith(folder_name)) {
                files_no++;
            }
        }
        return files_no;
    }
}
