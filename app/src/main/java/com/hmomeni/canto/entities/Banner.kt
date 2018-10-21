package com.hmomeni.canto.entities

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.SerializedName

data class Banner(
        @field:SerializedName("file")
        val file: String,
        @field:SerializedName("content_type")
        val contentType: String,
        @field:SerializedName("link")
        val link: String,
        @field:SerializedName("description")
        val description: String,
        @field:SerializedName("title")
        val title: String
) : Parcelable {
        constructor(source: Parcel) : this(
                source.readString(),
                source.readString(),
                source.readString(),
                source.readString(),
                source.readString()
        )

        override fun describeContents() = 0

        override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {
                writeString(file)
                writeString(contentType)
                writeString(link)
                writeString(description)
                writeString(title)
        }

        companion object {
                @JvmField
                val CREATOR: Parcelable.Creator<Banner> = object : Parcelable.Creator<Banner> {
                        override fun createFromParcel(source: Parcel): Banner = Banner(source)
                        override fun newArray(size: Int): Array<Banner?> = arrayOfNulls(size)
                }
        }
}