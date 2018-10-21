package com.hmomeni.canto.entities

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.SerializedName

data class Artist(
        @field:SerializedName("image")
        val image: String? = null,
        @field:SerializedName("name")
        val name: String? = null,
        @field:SerializedName("link")
        val link: String? = null,
        @field:SerializedName("poems_count")
        val poemsCount: Int,
        @field:SerializedName("id")
        val id: Int
) : Parcelable {
        constructor(source: Parcel) : this(
                source.readString(),
                source.readString(),
                source.readString(),
                source.readInt(),
                source.readInt()
        )

        override fun describeContents() = 0

        override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {
                writeString(image)
                writeString(name)
                writeString(link)
                writeInt(poemsCount)
                writeInt(id)
        }

        companion object {
                @JvmField
                val CREATOR: Parcelable.Creator<Artist> = object : Parcelable.Creator<Artist> {
                        override fun createFromParcel(source: Parcel): Artist = Artist(source)
                        override fun newArray(size: Int): Array<Artist?> = arrayOfNulls(size)
                }
        }
}