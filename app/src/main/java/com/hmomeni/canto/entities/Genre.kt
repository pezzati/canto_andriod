package com.hmomeni.canto.entities

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.SerializedName

data class Genre(
        @field:SerializedName("files_link")
        val filesLink: String,
        @field:SerializedName("cover_photo")
        val coverPhoto: String? = null,
        @field:SerializedName("link")
        val link: String,
        @field:SerializedName("name")
        val name: String,
        @field:SerializedName("liked_it")
        val likedIt: Boolean = false,
        var posts: List<Post>? = null
) : Parcelable {
    constructor(source: Parcel) : this(
            source.readString(),
            source.readString(),
            source.readString(),
            source.readString(),
            1 == source.readInt()
    )

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {
        writeString(filesLink)
        writeString(coverPhoto)
        writeString(link)
        writeString(name)
        writeInt((if (likedIt) 1 else 0))
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<Genre> = object : Parcelable.Creator<Genre> {
            override fun createFromParcel(source: Parcel): Genre = Genre(source)
            override fun newArray(size: Int): Array<Genre?> = arrayOfNulls(size)
        }
    }
}