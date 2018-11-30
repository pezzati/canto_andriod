package com.hmomeni.canto.entities

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.SerializedName

data class MidiItem(
        @field:SerializedName("time")
        val time: Double,
        @field:SerializedName("text")
        val text: String,
        @Transient
        var active: Boolean = false
) : Parcelable {
    constructor(source: Parcel) : this(
            source.readDouble(),
            source.readString()
    )

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {
        writeDouble(time)
        writeString(text)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<MidiItem> = object : Parcelable.Creator<MidiItem> {
            override fun createFromParcel(source: Parcel): MidiItem = MidiItem(source)
            override fun newArray(size: Int): Array<MidiItem?> = arrayOfNulls(size)
        }
    }
}