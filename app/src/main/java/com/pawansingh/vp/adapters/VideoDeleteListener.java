package com.pawansingh.vp.adapters;

import com.pawansingh.vp.models.MediaFiles;

public interface VideoDeleteListener {
    void onDeleteRequested(MediaFiles video, int position);
}
