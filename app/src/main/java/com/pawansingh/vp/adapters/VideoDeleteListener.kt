package com.pawansingh.vp.adapters

import com.pawansingh.vp.models.MediaFiles

interface VideoDeleteListener {
    fun onDeleteRequested(video: MediaFiles?, position: Int)
}
