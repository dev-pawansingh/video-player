package com.pawansingh.vp.models

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.pawansingh.vp.R
import com.pawansingh.vp.activities.VideoFilesActivity
import com.pawansingh.vp.adapters.VideoFilesAdapter

class PlaylistDialog(
    arrayList: ArrayList<MediaFiles?>?,
    videoFilesAdapter: VideoFilesAdapter
) : BottomSheetDialogFragment() {

    var arrayList: ArrayList<MediaFiles?>? = ArrayList()

    var videoFilesAdapter: VideoFilesAdapter =
        videoFilesAdapter

    var bottomSheetDialog: BottomSheetDialog? = null

    var recyclerView: RecyclerView? = null

    var folder: TextView? = null

    init {
        this.arrayList = arrayList
    }

    override fun onCreateDialog(
        savedInstanceState: Bundle?
    ): Dialog {

        bottomSheetDialog =
            super.onCreateDialog(
                savedInstanceState
            ) as BottomSheetDialog

        val view = LayoutInflater.from(requireContext())
            .inflate(
                R.layout.playlist_bs_layout,
                null
            )

        bottomSheetDialog?.setContentView(view)

        recyclerView =
            view.findViewById(R.id.playlist_rv)

        folder =
            view.findViewById(R.id.playlist_name)

        val preferences =
            requireActivity().getSharedPreferences(
                VideoFilesActivity.MY_PREF,
                Context.MODE_PRIVATE
            )

        val folderName =
            preferences.getString(
                "playlistFolderName",
                "abc"
            ) ?: "abc"

        folder?.text = folderName

        arrayList =
            fetchMedia(folderName)

        videoFilesAdapter =
            VideoFilesAdapter(
                arrayList ?: ArrayList(),
                requireContext(),
                1
            )

        val layoutManager =
            LinearLayoutManager(
                requireContext(),
                LinearLayoutManager.VERTICAL,
                false
            )

        recyclerView?.layoutManager =
            layoutManager

        recyclerView?.adapter =
            videoFilesAdapter

        videoFilesAdapter.notifyDataSetChanged()

        return bottomSheetDialog!!
    }

    private fun fetchMedia(
        folderName: String?
    ): ArrayList<MediaFiles?> {

        val videoFiles =
            ArrayList<MediaFiles?>()

        val uri =
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI

        val selection =
            MediaStore.Video.Media.DATA + " like?"

        val selectionArg: Array<String?> =
            arrayOf("%$folderName%")

        val cursor =
            requireContext()
                .contentResolver
                .query(
                    uri,
                    null,
                    selection,
                    selectionArg,
                    null
                )

        if (
            cursor != null &&
            cursor.moveToFirst()
        ) {

            do {

                val id =
                    cursor.getString(
                        cursor.getColumnIndexOrThrow(
                            MediaStore.Video.Media._ID
                        )
                    )

                val title =
                    cursor.getString(
                        cursor.getColumnIndexOrThrow(
                            MediaStore.Video.Media.TITLE
                        )
                    )

                val displayName =
                    cursor.getString(
                        cursor.getColumnIndexOrThrow(
                            MediaStore.Video.Media.DISPLAY_NAME
                        )
                    )

                val size =
                    cursor.getString(
                        cursor.getColumnIndexOrThrow(
                            MediaStore.Video.Media.SIZE
                        )
                    )

                val duration =
                    cursor.getString(
                        cursor.getColumnIndexOrThrow(
                            MediaStore.Video.Media.DURATION
                        )
                    )

                val path =
                    cursor.getString(
                        cursor.getColumnIndexOrThrow(
                            MediaStore.Video.Media.DATA
                        )
                    )

                val dateAdded =
                    cursor.getString(
                        cursor.getColumnIndexOrThrow(
                            MediaStore.Video.Media.DATE_ADDED
                        )
                    )

                val mediaFiles =
                    MediaFiles(
                        id,
                        title,
                        displayName,
                        size,
                        duration,
                        path,
                        dateAdded
                    )

                videoFiles.add(mediaFiles)

            } while (cursor.moveToNext())

            cursor.close()
        }

        return videoFiles
    }
}