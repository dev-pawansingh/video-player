package com.pawansingh.vp.models

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class MediaFiles(
    var id: String = "",
    var title: String = "",
    var displayName: String = "",
    var size: String = "0",
    var duration: String = "0",
    var path: String = "",
    var dateAdded: String = ""
) : Parcelable