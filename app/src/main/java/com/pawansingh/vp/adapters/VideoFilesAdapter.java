package com.pawansingh.vp.adapters;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.pawansingh.vp.R;
import com.pawansingh.vp.activities.VideoFilesActivity;
import com.pawansingh.vp.activities.VideoPlayerActivity;
import com.pawansingh.vp.models.MediaFiles;
import com.pawansingh.vp.models.Utility;

import java.io.File;
import java.util.ArrayList;

public class VideoFilesAdapter extends RecyclerView.Adapter<VideoFilesAdapter.ViewHolder> {
    private ArrayList<MediaFiles> videoList;
    private VideoDeleteListener deleteListener;
    private final Context context;
    BottomSheetDialog bottomSheetDialog;
    private final int viewType;

    public interface OnVideoActionListener {
        void onDeleteRequested(MediaFiles video, int position);
        void onRenameRequested(MediaFiles video, int position, String currentName);
    }

    private OnVideoActionListener actionListener;

    public void setOnVideoActionListener(OnVideoActionListener listener) {
        this.actionListener = listener;
    }

    public VideoFilesAdapter(ArrayList<MediaFiles> videoList, Context context, int viewType) {
        this.videoList = videoList;
        this.context = context;
        this.viewType = viewType;
    }

    @NonNull
    @Override
    public VideoFilesAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.video_grid_view, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VideoFilesAdapter.ViewHolder holder, @SuppressLint("RecyclerView") int position) {

        holder.videoName.setText(videoList.get(position).getDisplayName());
        String size = videoList.get(position).getSize();
        holder.videoSize.setText(android.text.format.Formatter.formatFileSize(context,
                Long.parseLong(size)));
        if (videoList.get(position).getDuration() != null) {
            double milliSeconds = Double.parseDouble((videoList.get(position).getDuration()));
            holder.videoDuration.setText(Utility.timeConversion((long) milliSeconds));
            Glide.with(context).load(new File(videoList.get(position).getPath())).
                    into(holder.thumbnail);

            if (viewType == 0) {
                holder.menu_more.setOnClickListener(view -> {
                    bottomSheetDialog = new BottomSheetDialog(context, R.style.BottomSheetTheme);
                    View bsView = LayoutInflater.from(context).inflate(R.layout.video_bs_layout,
                            view.findViewById(R.id.bottom_sheet));
                    bsView.findViewById(R.id.bs_play).setOnClickListener(view1 -> {
                        holder.itemView.performClick();
                        bottomSheetDialog.dismiss();
                    });

                    bsView.findViewById(R.id.bs_rename).setOnClickListener(view2 -> {

                        if (actionListener != null) {
                            actionListener.onRenameRequested(
                                    videoList.get(position),
                                    position,
                                    videoList.get(position).getDisplayName()
                            );
                        }

                        bottomSheetDialog.dismiss();
                    });

                    bsView.findViewById(R.id.bs_share).setOnClickListener(view3 -> {
                        Uri uri = Uri.parse(videoList.get(position).getPath());
                        Intent shareIntent = new Intent(Intent.ACTION_SEND);
                        shareIntent.setType("video/*");
                        shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
                        context.startActivity(Intent.createChooser(shareIntent, "Share video via?"));
                        bottomSheetDialog.dismiss();
                    });

                    bsView.findViewById(R.id.bs_delete).setOnClickListener(view4 -> {
                        AlertDialog.Builder alertDialog = new AlertDialog.Builder(context);
                        alertDialog.setTitle("Delete");
                        alertDialog.setMessage("Do you want to delete this video?");

                        alertDialog.setPositiveButton("Delete", (dialogInterface, i) -> {
                            // Notify the fragment/activity that delete was requested
                            if (actionListener != null) {
                                actionListener.onDeleteRequested(videoList.get(position), position);
                            } else {
                                // Fallback: attempt old behavior for safety (shouldn't happen)
                                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                                    Uri contentUri = ContentUris.withAppendedId(
                                            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                                            Long.parseLong(videoList.get(position).getId())
                                    );
                                    int rowsDeleted = context.getContentResolver().delete(contentUri, null, null);
                                    if (rowsDeleted > 0) {
                                        videoList.remove(position);
                                        notifyItemRemoved(position);
                                        Toast.makeText(context, "Video deleted successfully", Toast.LENGTH_SHORT).show();
                                    } else {
                                        Toast.makeText(context, "Video not deleted", Toast.LENGTH_SHORT).show();
                                    }
                                } else {
                                    Toast.makeText(context, "Delete not supported here", Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                        alertDialog.setNegativeButton("Cancel", (dialogInterface, i) -> dialogInterface.dismiss());
                        alertDialog.show();
                        bottomSheetDialog.dismiss();
                    });

                    bsView.findViewById(R.id.bs_properties).setOnClickListener(view5 -> {
                        AlertDialog.Builder alertDialog = new AlertDialog.Builder(context);
                        alertDialog.setTitle("Properties");

                        String one = "File: " + videoList.get(position).getDisplayName();
                        String path = videoList.get(position).getPath();
                        int indexOfPath = path.lastIndexOf("/");
                        String two = "Path: " + path.substring(0, indexOfPath);
                        String three = "Size: " + android.text.format.Formatter
                                .formatFileSize(context, Long.parseLong(videoList.get(position).getSize()));
                        String four = "Length: " + Utility.timeConversion((long) milliSeconds);
                        String nameWithFormat = videoList.get(position).getDisplayName();
                        int index = nameWithFormat.lastIndexOf(".");
                        String format = nameWithFormat.substring(index + 1);
                        String five = "Format: " + format;

                        MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
                        mediaMetadataRetriever.setDataSource(videoList.get(position).getPath());
                        String height = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
                        String width = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);

                        String six = "Resolution: " + width + "x" + height;

                        alertDialog.setMessage(one + "\n\n" + two + "\n\n" + three + "\n\n" + four + "\n\n" + five + "\n\n" + six);
                        alertDialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.dismiss();
                            }
                        });
                        alertDialog.show();
                        bottomSheetDialog.dismiss();
                    });

                    bottomSheetDialog.setContentView(bsView);
                    bottomSheetDialog.show();
                });
            } else {
                holder.menu_more.setVisibility(View.GONE);
                holder.videoName.setTextColor(Color.WHITE);
                holder.videoSize.setTextColor(Color.WHITE);
            }

            holder.itemView.setOnClickListener(view -> {
                Intent intent = new Intent(context, VideoPlayerActivity.class);
                intent.putExtra("position", position);
                intent.putExtra("video_title", videoList.get(position).getDisplayName());
                Bundle bundle = new Bundle();
                bundle.putParcelableArrayList("videoArrayList", videoList);
                intent.putExtras(bundle);
                context.startActivity(intent);
                if (viewType == 1) {
                    ((Activity) context).finish();
                }
            });
        } else {
            holder.videoDuration.setText(videoList.get(position).getDuration());
            Glide.with(context).load(new File(videoList.get(position).getPath())).
                    into(holder.thumbnail);

            if (viewType == 0) {
                holder.menu_more.setOnClickListener(view -> {
                    bottomSheetDialog = new BottomSheetDialog(context, R.style.BottomSheetTheme);
                    View bsView = LayoutInflater.from(context).inflate(R.layout.video_bs_layout,
                            view.findViewById(R.id.bottom_sheet));
                    bsView.findViewById(R.id.bs_play).setOnClickListener(view6 -> {
                        holder.itemView.performClick();
                        bottomSheetDialog.dismiss();
                    });

                    bsView.findViewById(R.id.bs_rename).setOnClickListener(view2 -> {
                        AlertDialog.Builder alertDialog = new AlertDialog.Builder(context);
                        alertDialog.setTitle("Rename Video");

                        EditText input = new EditText(context);
                        String currentName = videoList.get(position).getDisplayName();
                        // Remove extension for editing
                        input.setText(currentName.substring(0, currentName.lastIndexOf(".")));
                        alertDialog.setView(input);

                        alertDialog.setPositiveButton("Rename", (dialog, which) -> {
                            String newName = input.getText().toString().trim();
                            if (!newName.isEmpty()) {
                                // Add extension back
                                String extension = currentName.substring(currentName.lastIndexOf("."));
                                String fullNewName = newName + extension;

                                if (context instanceof VideoFilesActivity) {
                                    ((VideoFilesActivity) context).prepareForRename(
                                            videoList.get(position),
                                            position,
                                            fullNewName
                                    );

                                    // Launch permission intent
                                    Uri videoUri = ContentUris.withAppendedId(
                                            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                                            Long.parseLong(videoList.get(position).getId())
                                    );

                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                        ArrayList<Uri> uriList = new ArrayList<>();
                                        uriList.add(videoUri);
                                        PendingIntent editIntent = MediaStore.createWriteRequest(
                                                context.getContentResolver(),
                                                uriList
                                        );

                                        try {
                                            ((Activity) context).startIntentSenderForResult(
                                                    editIntent.getIntentSender(),
                                                    109, null, 0, 0, 0
                                            );
                                        } catch (Exception e) {
                                            Toast.makeText(context, "Error starting rename", Toast.LENGTH_SHORT).show();
                                        }
                                    } else {
                                        // Direct update for older Android versions
                                        ContentValues values = new ContentValues();
                                        values.put(MediaStore.Video.Media.DISPLAY_NAME, fullNewName);
                                        int rows = context.getContentResolver().update(videoUri, values, null, null);

                                        if (rows > 0) {
                                            videoList.get(position).setDisplayName(fullNewName);
                                            notifyItemChanged(position);
                                        }
                                    }
                                }
                            }
                        });

                        alertDialog.setNegativeButton("Cancel", null);
                        alertDialog.show();
                        bottomSheetDialog.dismiss();
                    });

                    bsView.findViewById(R.id.bs_share).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            Uri uri = Uri.parse(videoList.get(position).getPath());
                            Intent shareIntent = new Intent(Intent.ACTION_SEND);
                            shareIntent.setType("video/*");
                            shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
                            context.startActivity(Intent.createChooser(shareIntent, "Share video via?"));
                            bottomSheetDialog.dismiss();
                        }
                    });

                    bsView.findViewById(R.id.bs_delete).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            if (context instanceof Activity) {
                                Uri contentUri = ContentUris.withAppendedId(
                                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                                        Long.parseLong(videoList.get(position).getId())
                                );

                                Intent intent = new Intent(Intent.ACTION_DELETE);
                                intent.setData(contentUri);
                                intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                                // Save the reference in activity
                                if (context instanceof VideoFilesActivity) {
                                    ((VideoFilesActivity) context).prepareForDelete(videoList.get(position), position);
                                }
                                ((Activity) context).startActivityForResult(intent, 108);
                            }
                            bottomSheetDialog.dismiss();
                        }
                    });

                    bsView.findViewById(R.id.bs_properties).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            AlertDialog.Builder alertDialog = new AlertDialog.Builder(context);
                            alertDialog.setTitle("Properties");

                            String one = "File: " + videoList.get(position).getDisplayName();
                            String path = videoList.get(position).getPath();
                            int indexOfPath = path.lastIndexOf("/");
                            String two = "Path: " + path.substring(0, indexOfPath);
                            String three = "Size: " + android.text.format.Formatter
                                    .formatFileSize(context, Long.parseLong(videoList.get(position).getSize()));
                            //String four = "Length: "+Utility.timeConversion((long)milliSeconds);
                            String nameWithFormat = videoList.get(position).getDisplayName();
                            int index = nameWithFormat.lastIndexOf(".");
                            String format = nameWithFormat.substring(index + 1);
                            String five = "Format: " + format;

                            MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
                            mediaMetadataRetriever.setDataSource(videoList.get(position).getPath());
                            String height = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
                            String width = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);

                            String six = "Resolution: " + width + "x" + height;

                            // below add 4 after 3
                            alertDialog.setMessage(one + "\n\n" + two + "\n\n" + three + "\n\n" + five + "\n\n" + six);
                            alertDialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    dialogInterface.dismiss();
                                }
                            });
                            alertDialog.show();
                            bottomSheetDialog.dismiss();
                        }
                    });

                    bottomSheetDialog.setContentView(bsView);
                    bottomSheetDialog.show();
                });
            } else {
                holder.menu_more.setVisibility(View.GONE);
                holder.videoName.setTextColor(Color.WHITE);
                holder.videoSize.setTextColor(Color.WHITE);
            }

            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(context, VideoPlayerActivity.class);
                    intent.putExtra("position", position);
                    intent.putExtra("video_title", videoList.get(position).getDisplayName());
                    Bundle bundle = new Bundle();
                    bundle.putParcelableArrayList("videoArrayList", videoList);
                    intent.putExtras(bundle);
                    context.startActivity(intent);
                    if (viewType == 1) {
                        ((Activity) context).finish();
                    }
                }
            });
        }
    }
    public void setDeleteListener(VideoDeleteListener listener) {
        this.deleteListener = listener;
    }
    @Override
    public int getItemCount() {
        return videoList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        ImageView thumbnail, menu_more;
        CardView thumbnail_card;
        TextView videoName, videoSize, videoDuration;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            thumbnail = itemView.findViewById(R.id.thumbnail);
            thumbnail_card = itemView.findViewById(R.id.thumbnail_card);
            menu_more = itemView.findViewById(R.id.video_menu_more);
            videoName = itemView.findViewById(R.id.video_name);
            videoSize = itemView.findViewById(R.id.video_size);
            videoDuration = itemView.findViewById(R.id.video_duration);
        }
    }

    public void updateVideoFiles(ArrayList<MediaFiles> files) {
        videoList = new ArrayList<>();
        videoList.addAll(files);
        notifyDataSetChanged();
    }
}