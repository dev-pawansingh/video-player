package com.pawansingh.vp.fragments

import android.app.Activity
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.pawansingh.vp.R
import com.pawansingh.vp.activities.MainActivity
import com.pawansingh.vp.adapters.VideoFilesAdapter
import com.pawansingh.vp.models.MediaFiles

class VideosFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    private var mediaFiles = ArrayList<MediaFiles>()
    private lateinit var adapter: VideoFilesAdapter

    private lateinit var deleteLauncher: ActivityResultLauncher<IntentSenderRequest>
    private lateinit var renameLauncher: ActivityResultLauncher<IntentSenderRequest>

    private var renameVideo: MediaFiles? = null
    private var renamePosition = -1
    private var newNameForRename = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

//         DELETE LAUNCHER
        deleteLauncher = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    // Let MediaStore update
                    requireContext().contentResolver.notifyChange(
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI, null
                    )

                    Handler(Looper.getMainLooper()).postDelayed({
                        recyclerView.recycledViewPool.clear()
                        refreshList()
                    }, 120)

                } else {
                    Toast.makeText(context, "Video not deleted", Toast.LENGTH_SHORT).show()
                }

                (requireActivity() as? MainActivity)?.apply {
                    selectedVideo = null
                    selectedPosition = RecyclerView.NO_POSITION
                }
            }
//          RENAME LAUNCHER
        renameLauncher = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->

                if (result.resultCode == Activity.RESULT_OK) {

                    val video = renameVideo ?: return@registerForActivityResult
                    val uri = ContentUris.withAppendedId(
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                        video.id.toLong()
                    )

                    val values = ContentValues().apply {
                        put(MediaStore.Video.Media.DISPLAY_NAME, newNameForRename)
                    }

                    val rows = requireContext().contentResolver.update(uri, values, null, null)

                    if (rows > 0) {
                        Toast.makeText(context, "Renamed successfully", Toast.LENGTH_SHORT).show()
                        video.displayName = newNameForRename
                        adapter.notifyItemChanged(renamePosition)
                    } else {
                        Toast.makeText(context, "Rename failed", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, "Rename cancelled", Toast.LENGTH_SHORT).show()
                }

                renameVideo = null
                newNameForRename = ""
                renamePosition = -1
            }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_videos, container, false)

        recyclerView = view.findViewById(R.id.recyclerView)
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh_fragment_vid)

        setupRecycler()
        loadVideos()

        swipeRefreshLayout.setOnRefreshListener {
            loadVideos()
            swipeRefreshLayout.isRefreshing = false
        }

        return view
    }

    private fun setupRecycler() {
        val spanCount = calculateNoOfColumns(requireContext(), 200f)
        recyclerView.layoutManager = GridLayoutManager(context, spanCount)
    }

    private fun loadVideos() {
        mediaFiles = fetchVideos()
        adapter = VideoFilesAdapter(mediaFiles as ArrayList<MediaFiles?>, requireContext(), 0)

        adapter.setOnVideoActionListener(object : VideoFilesAdapter.OnVideoActionListener {

            override fun onRenameRequested(video: MediaFiles?, position: Int, currentName: String?) {
                if (video != null && currentName != null)
                    handleRename(video, position, currentName)
            }

            override fun onDeleteRequested(video: MediaFiles?, position: Int) {
                if (video != null) handleDelete(video, position)
            }
        })

        recyclerView.adapter = adapter
    }

    private fun refreshList() {
        mediaFiles.clear()
        mediaFiles.addAll(fetchVideos())
        adapter.notifyDataSetChanged()
    }

    private fun handleRename(video: MediaFiles, position: Int, currentName: String) {

        val dialog = android.app.AlertDialog.Builder(context)
        dialog.setTitle("Rename Video")

        val input = android.widget.EditText(context)
        input.setText(currentName.replace(".mp4", ""))

        dialog.setView(input)

        dialog.setPositiveButton("Rename") { _, _ ->
            val enteredName = input.text.toString().trim()

            if (enteredName.isEmpty()) {
                Toast.makeText(context, "Name cannot be empty", Toast.LENGTH_SHORT).show()
                return@setPositiveButton
            }

            newNameForRename = "$enteredName.mp4"
            renameVideo = video
            renamePosition = position

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val uri = ContentUris.withAppendedId(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    video.id.toLong()
                )
                val uriList = arrayListOf(uri)

                try {
                    val editIntent = MediaStore.createWriteRequest(requireContext().contentResolver, uriList)
                    val request = IntentSenderRequest.Builder(editIntent.intentSender).build()
                    renameLauncher.launch(request)

                } catch (e: Exception) {
                    Toast.makeText(context, "Rename request failed", Toast.LENGTH_SHORT).show()
                }

            } else {
                val uri = ContentUris.withAppendedId(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    video.id.toLong()
                )

                val values = ContentValues().apply {
                    put(MediaStore.Video.Media.DISPLAY_NAME, newNameForRename)
                }

                val rows = requireContext().contentResolver.update(uri, values, null, null)

                if (rows > 0) {
                    video.displayName = newNameForRename
                    adapter.notifyItemChanged(position)
                    Toast.makeText(context, "Renamed successfully", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Rename failed", Toast.LENGTH_SHORT).show()
                }
            }
        }
        dialog.setNegativeButton("Cancel") { d, _ -> d.dismiss() }
        dialog.show()
    }

    private fun handleDelete(video: MediaFiles, position: Int) {

        (requireActivity() as? MainActivity)?.prepareForDelete(video, position)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {

            val uri = ContentUris.withAppendedId(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                video.id.toLong()
            )

            try {
                val deleteIntent = MediaStore.createDeleteRequest(
                    requireContext().contentResolver,
                    arrayListOf(uri)
                )

                val request = IntentSenderRequest.Builder(deleteIntent.intentSender).build()
                deleteLauncher.launch(request)

            } catch (e: Exception) {
                Toast.makeText(context, "Could not delete", Toast.LENGTH_SHORT).show()
            }

        } else {
            val uri = ContentUris.withAppendedId(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                video.id.toLong()
            )

            val rows = requireContext().contentResolver.delete(uri, null, null)

            if (rows > 0) {
                recyclerView.recycledViewPool.clear()
                refreshList()
                Toast.makeText(context, "Video deleted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Video not deleted", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun calculateNoOfColumns(context: Context, columnWidthDp: Float): Int {
        val metrics = context.resources.displayMetrics
        val screenWidthDp = metrics.widthPixels / metrics.density
        return maxOf(2, (screenWidthDp / columnWidthDp + 0.5).toInt())
    }

    private fun fetchVideos(): ArrayList<MediaFiles> {
        val list = ArrayList<MediaFiles>()

        val uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        val order = "${MediaStore.Video.Media.DATE_ADDED} DESC"

        val cursor: Cursor? =
            requireContext().contentResolver.query(uri, null, null, null, order)

        cursor?.use {
            val id = it.getColumnIndex(MediaStore.Video.Media._ID)
            val title = it.getColumnIndex(MediaStore.Video.Media.TITLE)
            val display = it.getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME)
            val size = it.getColumnIndex(MediaStore.Video.Media.SIZE)
            val duration = it.getColumnIndex(MediaStore.Video.Media.DURATION)
            val data = it.getColumnIndex(MediaStore.Video.Media.DATA)
            val dateAdded = it.getColumnIndex(MediaStore.Video.Media.DATE_ADDED)

            while (it.moveToNext()) {
                list.add(
                    MediaFiles(
                        it.getString(id),
                        it.getString(title),
                        it.getString(display),
                        it.getString(size),
                        it.getString(duration),
                        it.getString(data),
                        it.getString(dateAdded)
                    )
                )
            }
        }
        return list
    }
    fun updateRenamedItem(renamedVideo: MediaFiles, position: Int) {
        if (position >= 0 && position < mediaFiles.size) {
            mediaFiles[position] = renamedVideo
            adapter.notifyItemChanged(position)
        }
    }
}