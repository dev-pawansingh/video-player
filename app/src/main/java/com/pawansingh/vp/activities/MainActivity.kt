package com.pawansingh.vp.activities

import android.Manifest
import android.content.ContentUris
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.OnApplyWindowInsetsListener
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayout.OnTabSelectedListener
import com.google.android.material.tabs.TabLayoutMediator
import com.google.android.material.tabs.TabLayoutMediator.TabConfigurationStrategy
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.InstallState
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.pawansingh.vp.R
import com.pawansingh.vp.adapters.ViewPagerAdapter
import com.pawansingh.vp.fragments.VideosFragment
import com.pawansingh.vp.models.MediaFiles
import com.pawansingh.vp.fragments.FoldersFragment
import com.pawansingh.vp.adapters.MediaActionHandler

class MainActivity : AppCompatActivity(), MediaActionHandler {
    private var appUpdateManager: AppUpdateManager? = null

    private var viewPager: ViewPager2? = null
    private var tabLayout: TabLayout? = null
    private var toolbar: MaterialToolbar? = null
    private var adapter: ViewPagerAdapter? = null

    private var videoToRename: MediaFiles? = null
    private var renamePosition = -1
    private var newVideoName = ""

    @JvmField
    var selectedVideo: MediaFiles? = null
    @JvmField
    var selectedPosition: Int = RecyclerView.NO_POSITION

    var vf: VideosFragment? = VideosFragment()
    var ff: FoldersFragment? = FoldersFragment()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(
            findViewById<View?>(R.id.main),
            OnApplyWindowInsetsListener { v: View?, insets: WindowInsetsCompat? ->
                val systemBars = insets!!.getInsets(WindowInsetsCompat.Type.systemBars())
                v!!.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
                insets
            })

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf<String>(Manifest.permission.READ_MEDIA_VIDEO),
                    1
                )
            } else {
                initViews()
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf<String>(Manifest.permission.READ_EXTERNAL_STORAGE),
                    1
                )
            } else {
                initViews()
            }
        }
        appUpdateManager = AppUpdateManagerFactory.create(this)
        checkForAppUpdate()
    }

    private fun checkForAppUpdate() {
        val appUpdateInfoTask = appUpdateManager!!.getAppUpdateInfo()

        appUpdateInfoTask.addOnSuccessListener(OnSuccessListener { appUpdateInfo: AppUpdateInfo? ->
            if (appUpdateInfo!!.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)
            ) {
                try {
                    appUpdateManager!!.startUpdateFlowForResult(
                        appUpdateInfo,
                        AppUpdateType.FLEXIBLE,
                        this,
                        FLEXIBLE_UPDATE_REQUEST_CODE
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        })

        // Listen to update state
        appUpdateManager!!.registerListener(installStateUpdatedListener)
    }

    // Listener to track download and notify user to complete update
    var installStateUpdatedListener: InstallStateUpdatedListener =
        object : InstallStateUpdatedListener {
            override fun onStateUpdate(state: InstallState) {
                if (state.installStatus() == InstallStatus.DOWNLOADED) {
                    // Show a toast or snackbar for user to complete update
                    Snackbar.make(
                        findViewById<View?>(android.R.id.content),
                        "An update has been downloaded.",
                        Snackbar.LENGTH_INDEFINITE
                    )
                        .setAction(
                            "Restart",
                            View.OnClickListener { view: View? -> appUpdateManager!!.completeUpdate() })
                        .show()
                }
            }
        }

    override fun onResume() {
        super.onResume()
        appUpdateManager!!
            .getAppUpdateInfo()
            .addOnSuccessListener(OnSuccessListener { appUpdateInfo: AppUpdateInfo? ->
                if (appUpdateInfo!!.installStatus() == InstallStatus.DOWNLOADED) {
                    appUpdateManager!!.completeUpdate() // Prompt to restart
                }
            })
    }

    override fun onDestroy() {
        super.onDestroy()
        if (appUpdateManager != null) {
            appUpdateManager!!.unregisterListener(installStateUpdatedListener)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1 && grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            initViews()
        }
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        tabLayout = findViewById<TabLayout>(R.id.tabLayout)
        viewPager = findViewById<ViewPager2>(R.id.viewPager)

        adapter = ViewPagerAdapter(this)
        viewPager!!.setAdapter(adapter)

        TabLayoutMediator(
            tabLayout!!,
            viewPager!!,
            TabConfigurationStrategy { tab: TabLayout.Tab?, position: Int ->
                tab!!.setText(if (position == 0) "  Videos 📽️  " else "  Folders 📂  ")
            }).attach()
        tabLayout!!.addOnTabSelectedListener(object : OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                val tabView = tab.getCustomView()
                if (tabView is TextView) {
                    tabView.setTypeface(null, Typeface.BOLD)
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {
                val tabView = tab.getCustomView()
                if (tabView is TextView) {
                    tabView.setTypeface(null, Typeface.NORMAL)
                }
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {
            }
        })
        reduceDragSensitivity(viewPager!!)
    }

    private fun reduceDragSensitivity(viewPager2: ViewPager2) {
        try {
            val recyclerView = viewPager2.getChildAt(0) as RecyclerView?
            val touchSlopField = RecyclerView::class.java.getDeclaredField("mTouchSlop")
            touchSlopField.setAccessible(true)
            val touchSlop = touchSlopField.getInt(recyclerView)
            touchSlopField.setInt(recyclerView, touchSlop * 2) // 3x less sensitive
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        getMenuInflater().inflate(R.menu.folder_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.getItemId()
        if (id == R.id.rateUs) {
            val uri = Uri.parse(
                "https://play.google.com/store/apps/details?id="
                        + getApplicationContext().getPackageName()
            )
            val intent = Intent(Intent.ACTION_VIEW, uri)
            startActivity(intent)
        } else if (id == R.id.refresh_folders) {
            finish()
            startActivity(getIntent())
        } else if (id == R.id.share_app) {
            val share_intent = Intent()
            share_intent.setAction(Intent.ACTION_SEND)
            share_intent.putExtra(
                Intent.EXTRA_TEXT,
                ("Check this app via\n" + "https://play.google.com/store/apps/details?id="
                        + getApplicationContext().getPackageName())
            )
            share_intent.setType("text/plain")
            startActivity(Intent.createChooser(share_intent, "Share app via"))
        }
        return true
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == FLEXIBLE_UPDATE_REQUEST_CODE) { // App update
            if (resultCode != RESULT_OK) {
                Toast.makeText(this, "Update canceled by user", Toast.LENGTH_SHORT).show()
            }
        }

        if (requestCode == 108) { // Delete
            if (resultCode == RESULT_OK && selectedVideo != null && selectedPosition != RecyclerView.NO_POSITION) {
                // Remove from UI
                Toast.makeText(this, "Video deleted successfully", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Video not deleted", Toast.LENGTH_SHORT).show()
            }
            selectedVideo = null
            selectedPosition = RecyclerView.NO_POSITION
        }

        if (requestCode == 109 && resultCode == RESULT_OK && videoToRename != null) {
            val values = ContentValues()
            val finalName =
                if (newVideoName.endsWith(".mp4")) newVideoName else newVideoName + ".mp4"
            values.put(MediaStore.Video.Media.DISPLAY_NAME, finalName)

            val uri = ContentUris.withAppendedId(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                videoToRename!!.id?.toLong() ?: return
            )

            val rows = getContentResolver().update(uri, values, null, null)

            if (rows > 0) {
                Toast.makeText(this, "Video renamed successfully", Toast.LENGTH_SHORT).show()
                videoToRename!!.displayName = finalName

                if (vf != null) {
                    vf!!.updateRenamedItem(videoToRename, renamePosition)
                } else if (ff != null) {
                    ff!!.updateRenamedItem(videoToRename, renamePosition)
                }
            } else {
                Toast.makeText(this, "Rename failed", Toast.LENGTH_SHORT).show()
            }

            videoToRename = null
            newVideoName = ""
            renamePosition = -1
        }
    }

    override fun prepareForRename(video: MediaFiles?, position: Int, newName: String?) {
        this.videoToRename = video
        this.renamePosition = position
        this.newVideoName = newName ?: ""
    }

    override fun prepareForDelete(video: MediaFiles?, position: Int) {
        selectedVideo = video
        selectedPosition = position
    }

    companion object {
        private const val FLEXIBLE_UPDATE_REQUEST_CODE = 123
    }
}