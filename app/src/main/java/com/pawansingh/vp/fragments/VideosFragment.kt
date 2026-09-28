package com.pawansingh.vp.fragments

import android.app.Activity
import android.app.AlertDialog
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts.StartIntentSenderForResult
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.pawansingh.vp.R
import com.pawansingh.vp.activities.MainActivity
import com.pawansingh.vp.adapters.VideoFilesAdapter
import com.pawansingh.vp.adapters.VideoFilesAdapter.OnVideoActionListener
import com.pawansingh.vp.models.MediaFiles

class VideosFragment : Fragment() {

    private var recyclerView: RecyclerView? = null
    private var mediaFilesArrayList = ArrayList<MediaFiles?>()
    private var adapter: VideoFilesAdapter? = null
    private var swipeRefreshLayout: SwipeRefreshLayout? = null
    private var deleteLauncher: ActivityResultLauncher<IntentSenderRequest>? = null
    private var renameLauncher: ActivityResultLauncher<IntentSenderRequest>? = null
    private var renameVideo: MediaFiles? = null
    private var renamePosition = -1
    private var newRenameName = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // DELETE RESULT
        deleteLauncher = registerForActivityResult(
            StartIntentSenderForResult(),
            ActivityResultCallback { result: ActivityResult ->

                if (result.resultCode == Activity.RESULT_OK) {

                    requireContext()
                        .contentResolver
                        .notifyChange(
                            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                            null
                        )

                    Handler(Looper.getMainLooper())
                        .postDelayed({

                            recyclerView
                                ?.recycledViewPool
                                ?.clear()

                            refreshList()

                        }, 150)

                } else {

                    Toast.makeText(
                        requireContext(),
                        "Video not deleted",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                val act = requireActivity()

                if (act is MainActivity) {
                    act.selectedVideo = null
                    act.selectedPosition =
                        RecyclerView.NO_POSITION
                }
            }
        )

        // RENAME RESULT
        renameLauncher = registerForActivityResult(
            StartIntentSenderForResult(),
            ActivityResultCallback { result: ActivityResult ->

                if (result.resultCode == Activity.RESULT_OK) {

                    val video = renameVideo

                    if (video != null) {

                        val videoId =
                            video.id?.toLong()
                                ?: return@ActivityResultCallback

                        val uri =
                            ContentUris.withAppendedId(
                                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                                videoId
                            )

                        val values = ContentValues()

                        values.put(
                            MediaStore.Video.Media.DISPLAY_NAME,
                            newRenameName
                        )

                        val rows =
                            requireContext()
                                .contentResolver
                                .update(
                                    uri,
                                    values,
                                    null,
                                    null
                                )

                        if (rows > 0) {

                            Toast.makeText(
                                requireContext(),
                                "Renamed successfully",
                                Toast.LENGTH_SHORT
                            ).show()

                            video.displayName =
                                newRenameName

                            adapter?.notifyItemChanged(
                                renamePosition
                            )

                        } else {

                            Toast.makeText(
                                requireContext(),
                                "Rename failed",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }

                } else {

                    Toast.makeText(
                        requireContext(),
                        "Rename cancelled",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                renameVideo = null
                renamePosition = -1
                newRenameName = ""
            }
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val view = inflater.inflate(
            R.layout.fragment_videos,
            container,
            false
        )

        recyclerView =
            view.findViewById(R.id.recyclerView)

        swipeRefreshLayout =
            view.findViewById(
                R.id.swipe_refresh_fragment_vid
            )

        showFolders()

        swipeRefreshLayout?.setOnRefreshListener {
            showFolders()
            swipeRefreshLayout?.isRefreshing = false
        }

        return view
    }

    private fun showFolders() {

        mediaFilesArrayList = fetchVideos()

        adapter = VideoFilesAdapter(
            mediaFilesArrayList,
            requireContext(),
            0
        )

        adapter?.setOnVideoActionListener(
            object : OnVideoActionListener {

                override fun onRenameRequested(
                    video: MediaFiles?,
                    position: Int,
                    currentName: String?
                ) {

                    if (video == null) return

                    handleRename(
                        video,
                        position,
                        currentName ?: video.displayName ?: ""
                    )
                }

                override fun onDeleteRequested(
                    video: MediaFiles?,
                    position: Int
                ) {

                    if (video == null) return

                    handleDelete(
                        video,
                        position
                    )
                }
            }
        )

        val spanCount =
            calculateNoOfColumns(
                requireContext(),
                200f
            )

        recyclerView?.layoutManager =
            GridLayoutManager(
                requireContext(),
                spanCount
            )

        recyclerView?.adapter = adapter

        adapter?.notifyDataSetChanged()
    }

    private fun refreshList() {

        mediaFilesArrayList.clear()

        mediaFilesArrayList.addAll(
            fetchVideos()
        )

        adapter?.notifyDataSetChanged()
    }

    private fun handleRename(
        video: MediaFiles,
        position: Int,
        currentName: String
    ) {

        val alertDialog =
            AlertDialog.Builder(requireContext())

        alertDialog.setTitle("Rename Video")

        val input =
            EditText(requireContext())

        val extensionIndex =
            currentName.lastIndexOf(".")

        val nameWithoutExtension =
            if (extensionIndex > 0) {
                currentName.substring(
                    0,
                    extensionIndex
                )
            } else {
                currentName
            }

        input.setText(nameWithoutExtension)

        alertDialog.setView(input)

        alertDialog.setPositiveButton(
            "Rename"
        ) { _, _ ->

            var newName =
                input.text.toString().trim()

            if (newName.isEmpty()) {

                Toast.makeText(
                    requireContext(),
                    "Name cannot be empty",
                    Toast.LENGTH_SHORT
                ).show()

                return@setPositiveButton
            }

            newName += ".mp4"

            renameVideo = video
            renamePosition = position
            newRenameName = newName

            val videoId =
                video.id?.toLong()

            if (videoId == null) {
                Toast.makeText(
                    requireContext(),
                    "Invalid video ID",
                    Toast.LENGTH_SHORT
                ).show()

                return@setPositiveButton
            }

            if (
                Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.R
            ) {

                val videoUri =
                    ContentUris.withAppendedId(
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                        videoId
                    )

                val uriList =
                    ArrayList<Uri?>()

                uriList.add(videoUri)

                try {

                    val editIntent =
                        MediaStore.createWriteRequest(
                            requireContext()
                                .contentResolver,
                            uriList
                        )

                    val req =
                        IntentSenderRequest.Builder(
                            editIntent.intentSender
                        ).build()

                    renameLauncher?.launch(req)

                } catch (e: Exception) {

                    Toast.makeText(
                        requireContext(),
                        "Rename request failed",
                        Toast.LENGTH_SHORT
                    ).show()
                }

            } else {

                val uri =
                    ContentUris.withAppendedId(
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                        videoId
                    )

                val values =
                    ContentValues()

                values.put(
                    MediaStore.Video.Media.DISPLAY_NAME,
                    newName
                )

                val rows =
                    requireContext()
                        .contentResolver
                        .update(
                            uri,
                            values,
                            null,
                            null
                        )

                if (rows > 0) {

                    video.displayName =
                        newName

                    adapter?.notifyItemChanged(
                        position
                    )

                    Toast.makeText(
                        requireContext(),
                        "Renamed successfully",
                        Toast.LENGTH_SHORT
                    ).show()

                } else {

                    Toast.makeText(
                        requireContext(),
                        "Rename failed",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        alertDialog.setNegativeButton(
            "Cancel",
            null
        )

        alertDialog.show()
    }

    private fun handleDelete(
        video: MediaFiles,
        position: Int
    ) {

        if (requireActivity() is MainActivity) {

            (requireActivity() as MainActivity)
                .prepareForDelete(
                    video,
                    position
                )
        }

        val videoId =
            video.id?.toLong()

        if (videoId == null) {
            Toast.makeText(
                requireContext(),
                "Invalid video ID",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.R
        ) {

            val contentUri =
                ContentUris.withAppendedId(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    videoId
                )

            val uriList =
                ArrayList<Uri?>()

            uriList.add(contentUri)

            val deleteIntent =
                MediaStore.createDeleteRequest(
                    requireContext()
                        .contentResolver,
                    uriList
                )

            try {

                val req =
                    IntentSenderRequest.Builder(
                        deleteIntent.intentSender
                    ).build()

                deleteLauncher?.launch(req)

            } catch (e: Exception) {

                e.printStackTrace()

                Toast.makeText(
                    requireContext(),
                    "Could not start delete request",
                    Toast.LENGTH_SHORT
                ).show()
            }

        } else {

            val contentUri =
                ContentUris.withAppendedId(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    videoId
                )

            val rowsDeleted =
                requireContext()
                    .contentResolver
                    .delete(
                        contentUri,
                        null,
                        null
                    )

            if (rowsDeleted > 0) {

                recyclerView
                    ?.recycledViewPool
                    ?.clear()

                refreshList()

                Toast.makeText(
                    requireContext(),
                    "Video deleted successfully",
                    Toast.LENGTH_SHORT
                ).show()

            } else {

                Toast.makeText(
                    requireContext(),
                    "Video not deleted",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun calculateNoOfColumns(
        context: Context,
        columnWidthDp: Float
    ): Int {

        val displayMetrics =
            context.resources.displayMetrics

        val screenWidthDp =
            displayMetrics.widthPixels /
                    displayMetrics.density

        val noOfColumns =
            (
                    screenWidthDp /
                            columnWidthDp +
                            0.5
                    ).toInt()

        return if (noOfColumns >= 2) {
            noOfColumns
        } else {
            2
        }
    }

    private fun fetchVideos(): ArrayList<MediaFiles?> {

        val list = ArrayList<MediaFiles?>()
        val uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        val orderBy = MediaStore.Video.Media.DATE_ADDED + " DESC"
        val cursor =
            requireContext()
                .contentResolver
                .query(
                    uri,
                    null,
                    null,
                    null,
                    orderBy
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

                val mediaFile =
                    MediaFiles(
                        id,
                        title,
                        displayName,
                        size,
                        duration,
                        path,
                        dateAdded
                    )

                list.add(mediaFile)

            } while (cursor.moveToNext())

            cursor.close()
        }

        return list
    }

    fun updateRenamedItem(
        renamedVideo: MediaFiles?,
        position: Int
    ) {

        if (
            renamedVideo != null &&
            position >= 0 &&
            position < mediaFilesArrayList.size
        ) {

            mediaFilesArrayList[position] =
                renamedVideo

            adapter?.notifyItemChanged(
                position
            )
        }
    }
}