package com.hmomeni.canto.entities

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.SerializedName

data class Content(
        @SerializedName("original_file_url")
        val originalFileUrl: String,
        @SerializedName("link")
        val link: String,
        @SerializedName("length")
        val length: String,
        @SerializedName("karaoke_file_url")
        val karaokeFileUrl: String,
        @SerializedName("artist")
        val artist: Artist,
        @SerializedName("midi")
        val midi: List<MidiItem>?

) : Parcelable {
    constructor(source: Parcel) : this(
            source.readString(),
            source.readString(),
            source.readString(),
            source.readString(),
            source.readParcelable<Artist>(Artist::class.java.classLoader),
            source.createTypedArrayList(MidiItem.CREATOR)
    )

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {
        writeString(originalFileUrl)
        writeString(link)
        writeString(length)
        writeString(karaokeFileUrl)
        writeParcelable(artist, 0)
        writeTypedList(midi)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<Content> = object : Parcelable.Creator<Content> {
            override fun createFromParcel(source: Parcel): Content = Content(source)
            override fun newArray(size: Int): Array<Content?> = arrayOfNulls(size)
        }
    }
}