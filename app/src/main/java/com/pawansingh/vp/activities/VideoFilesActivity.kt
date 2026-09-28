package com.pawansingh.vp.activities

import android.annotation.SuppressLint
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentSender.SendIntentException
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.SearchView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts.StartIntentSenderForResult
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout.OnRefreshListener
import com.pawansingh.vp.R
import com.pawansingh.vp.adapters.VideoDeleteListener
import com.pawansingh.vp.adapters.VideoFilesAdapter
import com.pawansingh.vp.models.MediaFiles
import com.pawansingh.vp.fragments.FoldersFragment
import com.pawansingh.vp.adapters.MediaActionHandler
import java.util.Locale
import java.util.Objects

class VideoFilesActivity : AppCompatActivity(), SearchView.OnQueryTextListener, MediaActionHandler {
    var selectedVideo: MediaFiles? = null
    var selectedPosition: Int = RecyclerView.NO_POSITION
    private var deleteVideoLauncher: ActivityResultLauncher<IntentSenderRequest?>? = null

    private var videoToRename: MediaFiles? = null
    private var renamePosition = 0
    private var newVideoName = ""

    var recyclerView: RecyclerView? = null
    var folder_name: String? = null
    var swipeRefreshLayout: SwipeRefreshLayout? = null
    var sortOrder: String? = null
    private var videoFilesArrayList = ArrayList<MediaFiles?>()
    var toolbar: Toolbar? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_files)

        toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar!!.setNavigationIcon(R.drawable.ic_back)
        toolbar!!.setNavigationOnClickListener(View.OnClickListener { view: View? -> finish() })

        deleteVideoLauncher = registerForActivityResult<IntentSenderRequest?, ActivityResult?>(
            StartIntentSenderForResult() as ActivityResultContract<IntentSenderRequest?, ActivityResult?>,
            ActivityResultCallback { result: ActivityResult? ->
                if (result!!.resultCode == RESULT_OK) {
                    if (selectedVideo != null && selectedPosition != RecyclerView.NO_POSITION) {
                        videoFilesArrayList.removeAt(selectedPosition)
                        videoFilesAdapter!!.notifyItemRemoved(selectedPosition)
                        Toast.makeText(this, "Video deleted successfully", Toast.LENGTH_SHORT)
                            .show()
                    }
                } else {
                    Toast.makeText(this, "Video not deleted", Toast.LENGTH_SHORT).show()
                }
                selectedVideo = null
                selectedPosition = RecyclerView.NO_POSITION
            }
        )

        folder_name = getIntent().getStringExtra("folderName")
        Objects.requireNonNull<ActionBar?>(getSupportActionBar()).setTitle(folder_name) //L-7
        recyclerView = findViewById<RecyclerView>(R.id.videos_rv)
        swipeRefreshLayout = findViewById<SwipeRefreshLayout>(R.id.swipe_refresh_videos)

        val editor = getSharedPreferences(MY_PREF, MODE_PRIVATE).edit()
        editor.putString("playlistFolderName", folder_name)
        editor.apply()

        showVideoFiles()
        swipeRefreshLayout!!.setOnRefreshListener(OnRefreshListener {
            showVideoFiles()
            swipeRefreshLayout!!.setRefreshing(false)
        })
        getSupportActionBar()!!.setDisplayHomeAsUpEnabled(true)
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun showVideoFiles() {
        videoFilesArrayList = fetchMedia(folder_name)
        videoFilesAdapter = VideoFilesAdapter(videoFilesArrayList, this, 0)

        val noOfColumns = calculateNoOfColumns(this, 180f)
        val gridLayoutManager = GridLayoutManager(this, noOfColumns) // 2 columns
        recyclerView!!.setLayoutManager(gridLayoutManager)

        recyclerView!!.setAdapter(videoFilesAdapter)
        videoFilesAdapter!!.notifyDataSetChanged()
        videoFilesAdapter!!.setDeleteListener(object : VideoDeleteListener {
            override fun onDeleteRequested(video: MediaFiles?, position: Int) {
                val contentUri = ContentUris.withAppendedId(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    video!!.id?.toLong() ?: return
                )

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val uriList = ArrayList<Uri?>()
                    uriList.add(contentUri)
                    val deleteIntent = MediaStore.createDeleteRequest(getContentResolver(), uriList)

                    try {
                        startIntentSenderForResult(
                            deleteIntent.getIntentSender(),
                            108, null, 0, 0, 0
                        )
                        selectedVideo = video
                        selectedPosition = position
                    } catch (e: SendIntentException) {
                        e.printStackTrace()
                    }
                } else {
                    val rowsDeleted = getContentResolver().delete(contentUri, null, null)
                    if (rowsDeleted > 0) {
                        videoFilesArrayList.removeAt(position)
                        videoFilesAdapter!!.notifyItemRemoved(position)
                        Toast.makeText(
                            this@VideoFilesActivity,
                            "Video deleted successfully",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Toast.makeText(
                            this@VideoFilesActivity,
                            "Video not deleted",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        })
    }

    private fun calculateNoOfColumns(context: Context, columnWidthDp: Float): Int {
        val displayMetrics = context.getResources().getDisplayMetrics()
        val screenWidthDp = displayMetrics.widthPixels / displayMetrics.density
        val noOfColumns = (screenWidthDp / columnWidthDp + 0.5).toInt() // +0.5 for rounding
        return if (noOfColumns >= 2) noOfColumns else 2 // minimum 2 columns
    }

    private fun fetchMedia(folderName: String?): ArrayList<MediaFiles?> {
        val preferences = getSharedPreferences(MY_PREF, MODE_PRIVATE)
        val sort_value: String = preferences.getString("sort", "abcd")!!

        val videoFiles = ArrayList<MediaFiles?>()
        val uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        if (sort_value == "sortName") {
            sortOrder = MediaStore.MediaColumns.DISPLAY_NAME + " ASC"
        } else if (sort_value == "sortSize") {
            sortOrder = MediaStore.MediaColumns.SIZE + " DESC"
        } else if (sort_value == "sortDate") {
            sortOrder = MediaStore.MediaColumns.DATE_ADDED + " DESC"
        } else {
            sortOrder = MediaStore.Video.Media.DURATION + " DESC"
        }

        val selection = MediaStore.Video.Media.DATA + " like?"
        val selectionArg: Array<String?> = arrayOf<String>("%" + folderName + "%") as Array<String?>
        val cursor = getContentResolver().query(
            uri, null,
            selection, selectionArg, sortOrder
        )
        if (cursor != null && cursor.moveToNext()) {
            do {
                val id = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media._ID))
                val title = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.TITLE))
                val displayName = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME))
                val size = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.SIZE))
                val duration = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.DURATION))
                val path = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.DATA))
                val dateAdded = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.DATE_ADDED))
                val mediaFiles = MediaFiles(id, title, displayName, size, duration, path, dateAdded)
                videoFiles.add(mediaFiles)
            } while (cursor.moveToNext())
            cursor.close()
        }
        return videoFiles
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        getMenuInflater().inflate(R.menu.video_menu, menu)
        val menuItem = menu.findItem(R.id.search_video)
        val searchView = menuItem.getActionView() as SearchView?
        searchView!!.setOnQueryTextListener(this)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val preferences = getSharedPreferences(MY_PREF, MODE_PRIVATE)
        val editor = preferences.edit()

        val id = item.getItemId()
        if (id == R.id.refresh_files) {
            finish()
            startActivity(getIntent())
        } else if (id == R.id.sort_by) {
            val alertDialog = AlertDialog.Builder(this)
            alertDialog.setTitle("Sort by")

            alertDialog.setPositiveButton("OK", object : DialogInterface.OnClickListener {
                override fun onClick(dialogInterface: DialogInterface, i: Int) {
                    editor.apply()
                    finish()
                    startActivity(getIntent())
                    dialogInterface.dismiss()
                }
            })
            val items = arrayOf<String?>(
                "Name (A to Z)",
                "Size (Large to Small)",
                "Date (New to Old)",
                "Length (Long to Short)"
            )
            alertDialog.setSingleChoiceItems(items, 2, object : DialogInterface.OnClickListener {
                override fun onClick(dialogInterface: DialogInterface?, i: Int) {
                    when (i) {
                        0 -> editor.putString("sort", "sortName")
                        1 -> editor.putString("sort", "sortSize")
                        2 -> editor.putString("sort", "sortDate")
                        3 -> editor.putString("sort", "sortLength")
                    }
                }
            })
            alertDialog.create().show()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onQueryTextSubmit(s: String?): Boolean {
        return false
    }

    override fun onQueryTextChange(s: String): Boolean {
        val inputs = s.lowercase(Locale.getDefault())
        val mediaFiles = ArrayList<MediaFiles?>()
        for (media in videoFilesArrayList) {
            if (media!!.title?.lowercase(Locale.getDefault())?.contains(inputs) ?: false ) {
                mediaFiles.add(media)
            }
        }
        videoFilesAdapter!!.updateVideoFiles(mediaFiles)
        return true
    }

    override fun prepareForDelete(video: MediaFiles?, position: Int) {
        selectedVideo = video
        selectedPosition = position
    }


    override fun prepareForRename(video: MediaFiles?, position: Int, newName: String?) {
        this.videoToRename = MediaFiles(
            video?.id, video?.title,newName,
            video?.size, video?.duration,
            video?.path, video?.dateAdded
        )
        this.renamePosition = position
    }

    fun updateRenamedItem(renamedVideo: MediaFiles?, position: Int) {
        val fragment = getSupportFragmentManager()
            .findFragmentByTag("your_fragment_tag") as FoldersFragment? // or by id if using container
        if (fragment != null) {
            fragment.updateRenamedItem(renamedVideo, position)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 109) { // Rename request code
            if (resultCode == RESULT_OK && videoToRename != null) {
                // Now actually perform the rename
                val contentUri = ContentUris.withAppendedId(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    videoToRename!!.id?.toLong() ?: return
                )

                val values = ContentValues()
                values.put(MediaStore.Video.Media.DISPLAY_NAME, videoToRename!!.displayName)

                try {
                    val rowsUpdated = getContentResolver().update(contentUri, values, null, null)
                    if (rowsUpdated > 0) {
                        // Update in our list
                        videoFilesArrayList.set(renamePosition, videoToRename!!)
                        videoFilesAdapter!!.notifyItemChanged(renamePosition)
                        Toast.makeText(this, "Video renamed successfully", Toast.LENGTH_SHORT)
                            .show()
                    } else {
                        Toast.makeText(this, "Rename failed", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this, "Error: " + e.message, Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Rename cancelled", Toast.LENGTH_SHORT).show()
            }
            videoToRename = null
        }
    }

    companion object {
        const val MY_PREF: String = "my pref"
        var videoFilesAdapter: VideoFilesAdapter? = null
    }
}