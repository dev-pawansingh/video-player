package com.pawansingh.vp.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.pawansingh.vp.fragments.FoldersFragment
import com.pawansingh.vp.fragments.VideosFragment

class ViewPagerAdapter(fragmentManager: FragmentManager, lifecycle: Lifecycle) :
    FragmentStateAdapter(fragmentManager, lifecycle) {

    val videosFragment = VideosFragment()
    val foldersFragment = FoldersFragment()

    override fun createFragment(position: Int): Fragment {
        return if (position == 0) videosFragment else foldersFragment
    }
    override fun getItemCount() = 2
}

