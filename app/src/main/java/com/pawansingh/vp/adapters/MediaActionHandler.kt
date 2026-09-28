package com.pawansingh.vp.adapters

import com.pawansingh.vp.models.MediaFiles

interface MediaActionHandler {
    fun prepareForRename(video: MediaFiles?, position: Int, newName: String?)
    fun prepareForDelete(video: MediaFiles?, position: Int)
}