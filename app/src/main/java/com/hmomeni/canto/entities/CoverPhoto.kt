package com.hmomeni.canto.entities

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.SerializedName

data class CoverPhoto(
        @field:SerializedName("link")
        val link: String,
        @field:SerializedName("id")
        val id: Int
) : Parcelable {
        constructor(source: Parcel) : this(
                source.readString(),
                source.readInt()
        )

        override fun describeContents() = 0

        override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {
                writeString(link)
                writeInt(id)
        }

        companion object {
                @JvmField
                val CREATOR: Parcelable.Creator<CoverPhoto> = object : Parcelable.Creator<CoverPhoto> {
                        override fun createFromParcel(source: Parcel): CoverPhoto = CoverPhoto(source)
                        override fun newArray(size: Int): Array<CoverPhoto?> = arrayOfNulls(size)
                }
        }
}