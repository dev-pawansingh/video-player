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

    private var deleteListener: VideoDeleteListener? = null
    var bottomSheetDialog: BottomSheetDialog? = null

    interface OnVideoActionListener {
        fun onDeleteRequested(video: MediaFiles?, position: Int)
        fun onRenameRequested(
            video: MediaFiles?,
            position: Int,
            currentName: String?
        )
    }

    private var actionListener: OnVideoActionListener? = null

    fun setOnVideoActionListener(listener: OnVideoActionListener?) {
        this.actionListener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context)
            .inflate(R.layout.video_grid_view, parent, false)

        return ViewHolder(view)
    }

    @SuppressLint("RecyclerView")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val video = videoList[position] ?: return
        holder.videoName.text = video.displayName
        val size = video.size
        holder.videoSize.text = Formatter.formatFileSize(
            context,
            size?.toLong() ?: return
        )

        if (video.duration != null) {

            val milliSeconds = video.duration!!.toDouble()
            holder.videoDuration.text = Utility.timeConversion(milliSeconds.toLong())
            Glide.with(context)
                .load(File(video.path))
                .into(holder.thumbnail)

            if (viewType == 0) {
                holder.menu_more.setOnClickListener { view ->
                    bottomSheetDialog = BottomSheetDialog(context, R.style.BottomSheetTheme)
                    val bsView = LayoutInflater.from(context).inflate(
                        R.layout.video_bs_layout,
                        view.findViewById<ViewGroup>(R.id.bottom_sheet)
                    )

                    // PLAY
                    bsView.findViewById<View>(R.id.bs_play)
                        .setOnClickListener {
                            holder.itemView.performClick()
                            bottomSheetDialog?.dismiss()
                        }

                    // RENAME
                    bsView.findViewById<View>(R.id.bs_rename)
                        .setOnClickListener {
                            actionListener?.onRenameRequested(
                                videoList[position],
                                position,
                                video.displayName
                            )
                            bottomSheetDialog?.dismiss()
                        }

                    // SHARE
                    bsView.findViewById<View>(R.id.bs_share)
                        .setOnClickListener {
                            val uri = Uri.parse(video.path)
                            val shareIntent = Intent(Intent.ACTION_SEND)
                            shareIntent.type = "video/*"
                            shareIntent.putExtra(Intent.EXTRA_STREAM, uri)
                            context.startActivity(
                                Intent.createChooser(
                                    shareIntent,
                                    "Share video via?"
                                )
                            )
                            bottomSheetDialog?.dismiss()
                        }

                    // DELETE
                    bsView.findViewById<View>(R.id.bs_delete)
                        .setOnClickListener {
                            val alertDialog = AlertDialog.Builder(context)
                            alertDialog.setTitle("Delete")
                            alertDialog.setMessage("Do you want to delete this video?")
                            alertDialog.setPositiveButton("Delete") { _, _ ->
                                if (actionListener != null) {
                                    actionListener?.onDeleteRequested(
                                        videoList[position],
                                        position
                                    )
                                } else {
                                    if (Build.VERSION.SDK_INT <
                                        Build.VERSION_CODES.R
                                    ) {
                                        val contentUri = ContentUris.withAppendedId(
                                                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                                            video.id?.toLong() ?: return@setPositiveButton
                                            )
                                        val rowsDeleted = context.contentResolver.delete(
                                                contentUri,
                                                null,
                                                null
                                            )
                                        if (rowsDeleted > 0) {
                                            videoList.removeAt(position)
                                            notifyItemRemoved(position)
                                            Toast.makeText(context, "Video deleted successfully", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "Video not deleted", Toast.LENGTH_SHORT).show()
                                        }

                                    } else {
                                        Toast.makeText(context, "Delete not supported here", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }

                            alertDialog.setNegativeButton(
                                "Cancel"
                            ) { dialog, _ ->
                                dialog.dismiss()
                            }
                            alertDialog.show()
                            bottomSheetDialog?.dismiss()
                        }

                    // PROPERTIES
                    bsView.findViewById<View>(R.id.bs_properties)
                        .setOnClickListener {
                            val alertDialog = AlertDialog.Builder(context)
                            alertDialog.setTitle("Properties")
                            val one = "File: ${video.displayName}"
                            val path = video.path
                            val indexOfPath = path?.lastIndexOf("/")
                            val two = "Path: ${path?.substring(0, indexOfPath!!)}"
                            val three = "Size: ${Formatter.formatFileSize(context,
                                video.size?.toLong() ?: return@setOnClickListener
                            )}"
                            val four = "Length: ${Utility.timeConversion(milliSeconds.toLong())}"
                            val nameWithFormat = video.displayName
                            val index = nameWithFormat?.lastIndexOf(".")
                            val format = nameWithFormat?.substring(index?.plus(1) ?: 1)
                            val five = "Format: $format"
                            val mediaMetadataRetriever = MediaMetadataRetriever()
                            mediaMetadataRetriever.setDataSource(
                                video.path
                            )

                            val height = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                            val width = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                            val six = "Resolution: ${width}x${height}"

                            alertDialog.setMessage(
                                "$one\n\n$two\n\n$three\n\n$four\n\n$five\n\n$six"
                            )

                            alertDialog.setPositiveButton("OK") { dialog, _ ->
                                dialog.dismiss()
                            }

                            alertDialog.show()
                            bottomSheetDialog?.dismiss()
                        }

                    bottomSheetDialog?.setContentView(bsView)
                    bottomSheetDialog?.show()
                }

            } else {
                holder.menu_more.visibility = View.GONE
                holder.videoName.setTextColor(Color.WHITE)
                holder.videoSize.setTextColor(Color.WHITE)
            }

            holder.itemView.setOnClickListener {
                val intent = Intent(context, VideoPlayerActivity::class.java)
                intent.putExtra("position", position)
                intent.putExtra("video_title", video.displayName)
                val bundle = Bundle()
                bundle.putParcelableArrayList("videoArrayList", videoList)
                intent.putExtras(bundle)
                context.startActivity(intent)
                if (viewType == 1) {
                    (context as Activity).finish()
                }
            }

        } else {
            holder.videoDuration.text = video.duration

            Glide.with(context)
                .load(File(video.path))
                .into(holder.thumbnail)

            if (viewType == 0) {
                holder.menu_more.setOnClickListener { view ->
                    bottomSheetDialog = BottomSheetDialog(context, R.style.BottomSheetTheme)

                    val bsView = LayoutInflater.from(context).inflate(
                        R.layout.video_bs_layout,
                        view.findViewById<ViewGroup>(R.id.bottom_sheet)
                    )

                    // PLAY
                    bsView.findViewById<View>(R.id.bs_play)
                        .setOnClickListener {
                            holder.itemView.performClick()
                            bottomSheetDialog?.dismiss()
                        }

                    // RENAME
                    bsView.findViewById<View>(R.id.bs_rename)
                        .setOnClickListener {
                            val alertDialog = AlertDialog.Builder(context)
                            alertDialog.setTitle("Rename Video")
                            val input = EditText(context)
                            val currentName = video.displayName
                            input.setText(
                                currentName?.substring(
                                    0,
                                    currentName.lastIndexOf(".")
                                )
                            )
                            alertDialog.setView(input)
                            alertDialog.setPositiveButton("Rename") { _, _ ->

                                val newName = input.text.toString().trim()
                                if (newName.isNotEmpty()) {
                                    val extension = currentName?.substring(
                                        currentName.lastIndexOf(".")
                                    )

                                    val fullNewName = newName + extension
                                    if (context is VideoFilesActivity) {
                                        context.prepareForRename(
                                            video,
                                            position,
                                            fullNewName
                                        )
                                        val videoUri = ContentUris.withAppendedId(
                                                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                                            video.id?.toLong() ?: return@setPositiveButton
                                            )

                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                            val uriList = ArrayList<Uri?>()
                                            uriList.add(videoUri)

                                            val editIntent = MediaStore.createWriteRequest(
                                                    context.contentResolver,
                                                    uriList
                                                )

                                            try {

                                                (context as Activity)
                                                    .startIntentSenderForResult(
                                                        editIntent.intentSender,
                                                        109,
                                                        null,
                                                        0,
                                                        0,
                                                        0
                                                    )

                                            } catch (e: Exception) {
                                                Toast.makeText(context, "Error starting rename", Toast.LENGTH_SHORT).show()
                                            }

                                        } else {
                                            val values = ContentValues()
                                            values.put(
                                                MediaStore.Video.Media.DISPLAY_NAME,
                                                fullNewName
                                            )

                                            val rows = context.contentResolver.update(
                                                    videoUri,
                                                    values,
                                                    null,
                                                    null
                                                )

                                            if (rows > 0) {
                                                video.displayName = fullNewName
                                                notifyItemChanged(position)
                                            }
                                        }
                                    }
                                }
                            }

                            alertDialog.setNegativeButton(
                                "Cancel",
                                null
                            )
                            alertDialog.show()
                            bottomSheetDialog?.dismiss()
                        }

                    // SHARE
                    bsView.findViewById<View>(R.id.bs_share)
                        .setOnClickListener {
                            val uri = Uri.parse(video.path)
                            val shareIntent = Intent(Intent.ACTION_SEND)
                            shareIntent.type = "video/*"
                            shareIntent.putExtra(
                                Intent.EXTRA_STREAM,
                                uri
                            )

                            context.startActivity(
                                Intent.createChooser(
                                    shareIntent,
                                    "Share video via?"
                                )
                            )

                            bottomSheetDialog?.dismiss()
                        }

                    // DELETE
                    bsView.findViewById<View>(R.id.bs_delete)
                        .setOnClickListener {

                            if (context is Activity) {
                                val contentUri = ContentUris.withAppendedId(
                                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                                    video.id?.toLong() ?: return@setOnClickListener
                                    )

                                val intent = Intent(Intent.ACTION_DELETE)
                                intent.data = contentUri
                                intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                                if (context is VideoFilesActivity) {
                                    context.prepareForDelete(
                                        videoList[position],
                                        position
                                    )
                                }

                                context.startActivityForResult(
                                    intent,
                                    108
                                )
                            }

                            bottomSheetDialog?.dismiss()
                        }

                    // PROPERTIES
                    bsView.findViewById<View>(R.id.bs_properties)
                        .setOnClickListener {
                            val alertDialog = AlertDialog.Builder(context)
                            alertDialog.setTitle("Properties")
                            val one = "File: ${video.displayName}"
                            val path = video.path
                            val indexOfPath = path?.lastIndexOf("/")
                            val two = "Path: ${
                                path?.substring(
                                    0,
                                    indexOfPath!!
                                )
                                }"

                            val three = "Size: ${
                                    Formatter.formatFileSize(
                                        context,
                                        video.size?.toLong() ?: return@setOnClickListener 
                                    )
                                }"

                            val nameWithFormat = video.displayName
                            val index = nameWithFormat?.lastIndexOf(".")
                            val format = nameWithFormat?.substring(
                                index?.plus(1) ?: 1
                            )

                            val five = "Format: $format"
                            val mediaMetadataRetriever = MediaMetadataRetriever()
                            mediaMetadataRetriever.setDataSource(
                                video.path
                            )

                            val height = mediaMetadataRetriever.extractMetadata(
                                    MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT
                                )

                            val width = mediaMetadataRetriever.extractMetadata(
                                    MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH
                                )

                            val six = "Resolution: ${width}x${height}"

                            alertDialog.setMessage(
                                "$one\n\n$two\n\n$three\n\n$five\n\n$six"
                            )

                            alertDialog.setPositiveButton(
                                "OK"
                            ) { dialog, _ ->
                                dialog.dismiss()
                            }
                            alertDialog.show()
                            bottomSheetDialog?.dismiss()
                        }

                    bottomSheetDialog?.setContentView(bsView)
                    bottomSheetDialog?.show()
                }

            } else {
                holder.menu_more.visibility = View.GONE
                holder.videoName.setTextColor(Color.WHITE)
                holder.videoSize.setTextColor(Color.WHITE)
            }

            holder.itemView.setOnClickListener {

                val intent = Intent(context, VideoPlayerActivity::class.java)
                intent.putExtra("position", position)
                intent.putExtra("video_title", video.displayName)
                val bundle = Bundle()
                bundle.putParcelableArrayList("videoArrayList", videoList)
                intent.putExtras(bundle)
                context.startActivity(intent)

                if (viewType == 1) {
                    (context as Activity).finish()
                }
            }
        }
    }

    fun setDeleteListener(listener: VideoDeleteListener?) {
        this.deleteListener = listener
    }

    override fun getItemCount(): Int {
        return videoList.size
    }

     class ViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {

        var thumbnail: ImageView
        var menu_more: ImageView
        var thumbnail_card: CardView?
        var videoName: TextView
        var videoSize: TextView
        var videoDuration: TextView

        init {
            thumbnail = itemView.findViewById(R.id.thumbnail)
            thumbnail_card = itemView.findViewById(R.id.thumbnail_card)
            menu_more = itemView.findViewById(R.id.video_menu_more)
            videoName = itemView.findViewById(R.id.video_name)
            videoSize = itemView.findViewById(R.id.video_size)
            videoDuration = itemView.findViewById(R.id.video_duration)
        }
    }

    fun updateVideoFiles(files: ArrayList<MediaFiles?>) {
        videoList = ArrayList()
        videoList.addAll(files)
        notifyDataSetChanged()
    }
}