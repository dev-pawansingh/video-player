package com.pawansingh.vp.fragments

import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.pawansingh.vp.R
import com.pawansingh.vp.adapters.VideoFoldersAdapter
import com.pawansingh.vp.models.MediaFiles

class FoldersFragment : Fragment() {

    private var recyclerView: RecyclerView? = null
    private var mediaFilesArrayList = ArrayList<MediaFiles>()
    private val allFolderList = ArrayList<String?>()
    private var adapter: VideoFoldersAdapter? = null
    private var swipeRefreshLayout: SwipeRefreshLayout? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val view = inflater.inflate(
            R.layout.fragment_folders,
            container,
            false
        )

        recyclerView = view.findViewById(R.id.recyclerView)
        swipeRefreshLayout = view.findViewById(
                R.id.swipe_refresh_fragment_fldr
            )

        mediaFilesArrayList = fetchMedia()

        adapter = VideoFoldersAdapter(
            mediaFilesArrayList,
            allFolderList,
            requireContext()
        )

        recyclerView?.adapter = adapter

        recyclerView?.layoutManager =
            LinearLayoutManager(requireContext())

        swipeRefreshLayout?.setOnRefreshListener {

            mediaFilesArrayList = fetchMedia()

            adapter = VideoFoldersAdapter(
                mediaFilesArrayList,
                allFolderList,
                requireContext()
            )
            recyclerView?.adapter = adapter
            recyclerView?.layoutManager = LinearLayoutManager(requireContext())
            swipeRefreshLayout?.isRefreshing = false
        }
        return view
    }

    private fun fetchMedia(): ArrayList<MediaFiles> {

        val list = ArrayList<MediaFiles>()
        val uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        val cursor = requireContext()
                .contentResolver
                .query(
                    uri,
                    null,
                    null,
                    null,
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

                val index =
                    path.lastIndexOf("/")

                if (index < 0) {
                    continue
                }

                val folderPath =
                    path.substring(
                        0,
                        index
                    )

                // Skip /storage/emulated/0 folder
                if (
                    folderPath ==
                    "/storage/emulated/0"
                ) {
                    continue
                }

                // Add folder if it doesn't already exist
                if (
                    !allFolderList.contains(
                        folderPath
                    )
                ) {
                    allFolderList.add(
                        folderPath
                    )
                }

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
            position >= 0 &&
            position < mediaFilesArrayList.size
        ) {

            if (renamedVideo != null) {

                mediaFilesArrayList[position] =
                    renamedVideo

                adapter?.notifyItemChanged(
                    position
                )
            }
        }
    }
}