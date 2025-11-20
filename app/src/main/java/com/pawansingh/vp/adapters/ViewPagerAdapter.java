package com.pawansingh.vp.adapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.pawansingh.vp.newthing.FoldersFragment;
import com.pawansingh.vp.fragments.VideosFragment;

public class ViewPagerAdapter extends FragmentStateAdapter {
    public ViewPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        if (position == 0) {
            return new VideosFragment();
        }
        return new FoldersFragment();
    }

    @Override
    public int getItemCount() {
        return 2;
    }
}
