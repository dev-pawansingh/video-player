package com.pawansingh.vp.models

import android.os.Parcel
import android.os.Parcelable

class MediaFiles : Parcelable {
    var id: String?
    var title: String?
    var displayName: String?
    var size: String?
    var duration: String?
    var path: String?
    var dateAdded: String?

    constructor(
        id: String?,
        title: String?,
        displayName: String?,
        size: String?,
        duration: String?,
        path: String?,
        dateAdded: String?
    ) {
        this.id = id
        this.title = title
        this.displayName = displayName
        this.size = size
        this.duration = duration
        this.path = path
        this.dateAdded = dateAdded
    }

    protected constructor(`in`: Parcel) {
        id = `in`.readString()
        title = `in`.readString()
        displayName = `in`.readString()
        size = `in`.readString()
        duration = `in`.readString()
        path = `in`.readString()
        dateAdded = `in`.readString()
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(parcel: Parcel, i: Int) {
        parcel.writeString(id)
        parcel.writeString(title)
        parcel.writeString(displayName)
        parcel.writeString(size)
        parcel.writeString(duration)
        parcel.writeString(path)
        parcel.writeString(dateAdded)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<MediaFiles?> = object : Parcelable.Creator<MediaFiles?> {
            override fun createFromParcel(`in`: Parcel): MediaFiles {
                return MediaFiles(`in`)
            }
            override fun newArray(size: Int): Array<MediaFiles?> {
                return arrayOfNulls<MediaFiles>(size)
            }
        }
    }
}
