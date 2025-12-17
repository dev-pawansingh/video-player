package com.pawansingh.vp.update

import android.Manifest
import android.content.ContentUris
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.pawansingh.vp.R
import com.pawansingh.vp.adapters.ViewPagerAdapter
import com.pawansingh.vp.databinding.FragmentHomeBinding
import com.pawansingh.vp.fragments.FoldersFragment
import com.pawansingh.vp.fragments.VideosFragment
import com.pawansingh.vp.models.MediaFiles


class HomeFragment : Fragment() {

    private lateinit var binding: FragmentHomeBinding

    private var permissionHandled = false

    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout
    private lateinit var toolbar: MaterialToolbar
    private lateinit var adapter: ViewPagerAdapter

    private var videoToRename: MediaFiles? = null
    private var renamePosition = -1
    private var newVideoName = ""

    var selectedVideo: MediaFiles? = null
    var selectedPosition: Int = RecyclerView.NO_POSITION

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true) // enables toolbar menu
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHomeBinding.inflate(layoutInflater)
        checkPermissions(binding.root)
        return binding.root
    }

    private fun checkPermissions(view: View) {
        val permission =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                Manifest.permission.READ_MEDIA_VIDEO
            else
                Manifest.permission.READ_EXTERNAL_STORAGE

        if (ContextCompat.checkSelfPermission(requireContext(), permission)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(permission), 1)
        } else {
            initViews(view)
        }
    }

    private fun initViews(view: View) {
        toolbar = view.findViewById(R.id.toolbar)
        tabLayout = view.findViewById(R.id.tabLayout)
        viewPager = view.findViewById(R.id.viewPager)

        // Toolbar works only if activity has setSupportActionBar
        (requireActivity() as? androidx.appcompat.app.AppCompatActivity)?.apply {
            setSupportActionBar(toolbar)
        }

        adapter = ViewPagerAdapter(childFragmentManager, viewLifecycleOwner.lifecycle)
        viewPager.adapter = adapter

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = if (position == 0) "Videos 🎥" else "Folders 📂"
        }.attach()

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                (tab.customView as? TextView)?.setTypeface(null, android.graphics.Typeface.BOLD)
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {
                (tab.customView as? TextView)?.setTypeface(null, android.graphics.Typeface.NORMAL)
            }

            override fun onTabReselected(tab: TabLayout.Tab) {}
        })

        reduceDragSensitivity(viewPager)
        permissionHandled = true
    }

    private fun reduceDragSensitivity(viewPager2: ViewPager2) {
        try {
            val recyclerView = viewPager2.getChildAt(0) as RecyclerView
            val field = RecyclerView::class.java.getDeclaredField("mTouchSlop")
            field.isAccessible = true
            val touchSlop = field.getInt(recyclerView)
            field.setInt(recyclerView, touchSlop * 2)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // ------------------- MENU -------------------------

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.folder_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {

            R.id.rateUs -> {
                val uri = Uri.parse("https://play.google.com/store/apps/details?id=" + requireContext().packageName)
                startActivity(Intent(Intent.ACTION_VIEW, uri))
            }

            R.id.share_app -> {
                val shareIntent = Intent(Intent.ACTION_SEND)
                shareIntent.putExtra(
                    Intent.EXTRA_TEXT,
                    "Check this app:\nhttps://play.google.com/store/apps/details?id=" + requireContext().packageName
                )
                shareIntent.type = "text/plain"
                startActivity(Intent.createChooser(shareIntent, "Share via"))
            }
        }
        return true
    }

    // ------------------- DELETE / RENAME CALLBACKS -------------------------

    fun prepareForDelete(video: MediaFiles, position: Int) {
        selectedVideo = video
        selectedPosition = position
    }

    fun prepareForRename(video: MediaFiles, position: Int, newName: String) {
        videoToRename = video
        renamePosition = position
        newVideoName = newName
    }

    fun handleRenameResult() {
        val video = videoToRename ?: return

        val finalName =
            if (newVideoName.endsWith(".mp4")) newVideoName else "$newVideoName.mp4"

        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, finalName)
        }

        val uri = ContentUris.withAppendedId(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            video.id.toLong()
        )

        val rows = requireContext().contentResolver.update(uri, values, null, null)

        if (rows > 0) {
            Toast.makeText(requireContext(), "Renamed successfully", Toast.LENGTH_SHORT).show()
            video.displayName = finalName

            adapter.videosFragment.updateRenamedItem(video, renamePosition)
            adapter.foldersFragment.updateRenamedItem(video, renamePosition)

        }

        videoToRename = null
        renamePosition = -1
        newVideoName = ""
    }
    override fun onResume() {
        super.onResume()

        if (!permissionHandled) {
            val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                Manifest.permission.READ_MEDIA_VIDEO
            else
                Manifest.permission.READ_EXTERNAL_STORAGE

            if (ContextCompat.checkSelfPermission(requireContext(), permission)
                == PackageManager.PERMISSION_GRANTED
            ) {
                permissionHandled = true
                initViews(binding.root)
            }
        }
    }

}