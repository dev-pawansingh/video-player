package com.pawansingh.vp.newthing;

import com.pawansingh.vp.models.MediaFiles;

public interface MediaActionHandler {
    void prepareForRename(MediaFiles video, int position, String newName);
    void prepareForDelete(MediaFiles video, int position);
}
