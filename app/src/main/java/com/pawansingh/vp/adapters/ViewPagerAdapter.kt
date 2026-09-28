package com.pawansingh.vp.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.pawansingh.vp.fragments.VideosFragment
import com.pawansingh.vp.fragments.FoldersFragment

class ViewPagerAdapter(fragmentActivity: FragmentActivity) :
    FragmentStateAdapter(fragmentActivity) {
    override fun createFragment(position: Int): Fragment {
        if (position == 0) {
            return VideosFragment()
        }
        return FoldersFragment()
    }

    override fun getItemCount(): Int {
        return 2
    }
}
