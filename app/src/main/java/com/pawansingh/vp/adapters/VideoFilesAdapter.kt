package com.pawansingh.vp.adapters

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.pawansingh.vp.R
import com.pawansingh.vp.activities.VideoFilesActivity
import com.pawansingh.vp.activities.VideoPlayerActivity
import com.pawansingh.vp.models.MediaFiles
import com.pawansingh.vp.models.Utility
import java.io.File

class VideoFilesAdapter(
    private var videoList: ArrayList<MediaFiles?>,
    private val context: Context,
    private val viewType: Int
) : RecyclerView.Adapter<VideoFilesAdapter.ViewHolder>() {

    interface OnVideoActionListener {
        fun onDeleteRequested(video: MediaFiles?, position: Int)
        fun onRenameRequested(video: MediaFiles?, position: Int, currentName: String?)
    }

    private var actionListener: OnVideoActionListener? = null
    fun setOnVideoActionListener(listener: OnVideoActionListener?) {
        this.actionListener = listener
    }

    private var bottomSheetDialog: BottomSheetDialog? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view =
            LayoutInflater.from(context).inflate(R.layout.video_grid_view, parent, false)
        return ViewHolder(view)
    }

    @SuppressLint("RecyclerView")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val item = videoList[position] ?: return

        // BASIC DATA
        holder.videoName.text = item.displayName
        holder.videoSize.text = Formatter.formatFileSize(context, item.size.toLong())

        val durationMs = item.duration.toDoubleOrNull() ?: 0.0
        holder.videoDuration.text = Utility.timeConversion(durationMs.toLong())

        // FAST GLIDE LOAD (LAG FIX)
        Glide.with(context)
            .asBitmap()
            .load(Uri.fromFile(File(item.path)))
            .frame(1_000_000)
            .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
//            .thumbnail(0.2f)
            .into(holder.thumbnail)

        // Local videos mode
        if (viewType == 0) setupMenu(holder, item, position)
        else {
            // Folder mode
            holder.menu_more.visibility = View.GONE
            holder.videoName.setTextColor(Color.WHITE)
            holder.videoSize.setTextColor(Color.WHITE)
        }

        // Click → open video player
        holder.itemView.setOnClickListener {
            val intent = Intent(context, VideoPlayerActivity::class.java)
            intent.putExtra("position", position)
            intent.putExtra("video_title", item.displayName)

            val bundle = Bundle()
            bundle.putParcelableArrayList("videoArrayList", videoList)
            intent.putExtras(bundle)

            context.startActivity(intent)
            if (viewType == 1) (context as Activity).finish()
        }
    }

    private fun setupMenu(holder: ViewHolder, item: MediaFiles, position: Int) {
        holder.menu_more.setOnClickListener {
            showBottomSheet(holder, item, position)
        }
    }

    private fun showBottomSheet(holder: ViewHolder, item: MediaFiles, position: Int) {

        bottomSheetDialog = BottomSheetDialog(context, R.style.BottomSheetTheme)

        val view = LayoutInflater.from(context)
            .inflate(R.layout.video_bs_layout, null)

        // PLAY
        view.findViewById<View>(R.id.bs_play).setOnClickListener {
            holder.itemView.performClick()
            bottomSheetDialog?.dismiss()
        }

        // RENAME
        view.findViewById<View>(R.id.bs_rename).setOnClickListener {
            actionListener?.onRenameRequested(item, position, item.displayName)
            bottomSheetDialog?.dismiss()
        }

        // SHARE
        view.findViewById<View>(R.id.bs_share).setOnClickListener {
            val uri = Uri.parse(item.path)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "video/*"
                putExtra(Intent.EXTRA_STREAM, uri)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share via"))
            bottomSheetDialog?.dismiss()
        }

        // DELETE
        view.findViewById<View>(R.id.bs_delete).setOnClickListener {
            confirmDelete(item, position)
        }

        // PROPERTIES
        view.findViewById<View>(R.id.bs_properties).setOnClickListener {
            showProperties(item)
            bottomSheetDialog?.dismiss()
        }

        bottomSheetDialog!!.setContentView(view)
        bottomSheetDialog!!.show()
    }

    private fun confirmDelete(video: MediaFiles, position: Int) {
        val alert = AlertDialog.Builder(context)
        alert.setTitle("Delete")
        alert.setMessage("Do you want to delete this video?")

        alert.setPositiveButton("Delete") { _, _ ->
            if (actionListener != null) {
                actionListener!!.onDeleteRequested(video, position)
            } else {
                fallbackDelete(video, position)
            }
        }

        alert.setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }

        alert.show()
        bottomSheetDialog?.dismiss()
    }

    // Fallback for pre-Android 11
    private fun fallbackDelete(video: MediaFiles, position: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Toast.makeText(context, "Delete not supported here", Toast.LENGTH_SHORT).show()
            return
        }

        val contentUri = ContentUris.withAppendedId(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            video.id.toLong()
        )

        val rows = context.contentResolver.delete(contentUri, null, null)

        if (rows > 0) {
            videoList.removeAt(position)
            notifyItemRemoved(position)
            Toast.makeText(context, "Video deleted", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Video not deleted", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showProperties(item: MediaFiles) {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(item.path)

        val height =
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT) ?: "?"
        val width =
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH) ?: "?"

        retriever.release()

        val info =
            "File: ${item.displayName}\n\n" +
                    "Path: ${item.path.substringBeforeLast("/")}\n\n" +
                    "Size: ${Formatter.formatFileSize(context, item.size.toLong())}\n\n" +
                    "Length: ${Utility.timeConversion(item.duration.toLong())}\n\n" +
                    "Format: ${item.displayName.substringAfterLast(".")}\n\n" +
                    "Resolution: ${width}x${height}"

        AlertDialog.Builder(context)
            .setTitle("Properties")
            .setMessage(info)
            .setPositiveButton("OK", null)
            .show()
    }

    override fun getItemCount(): Int = videoList.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val thumbnail: ImageView = itemView.findViewById(R.id.thumbnail)
        val menu_more: ImageView = itemView.findViewById(R.id.video_menu_more)
        val videoName: TextView = itemView.findViewById(R.id.video_name)
        val videoSize: TextView = itemView.findViewById(R.id.video_size)
        val videoDuration: TextView = itemView.findViewById(R.id.video_duration)
    }

    fun updateVideoFiles(files: ArrayList<MediaFiles?>) {
        videoList = ArrayList(files)
        notifyDataSetChanged()
    }
}
