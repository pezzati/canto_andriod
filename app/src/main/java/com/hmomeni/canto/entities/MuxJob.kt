package com.hmomeni.canto.entities

import android.os.Parcel
import android.os.Parcelable

class MuxJob(
        val type: Int,
        val postId: Long,
        val inputFiles: ArrayList<String>,
        val outputFile: String
) : Parcelable {
    constructor(source: Parcel) : this(
            source.readInt(),
            source.readLong(),
            source.createStringArrayList(),
            source.readString()
    )

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {
        writeInt(type)
        writeLong(postId)
        writeStringList(inputFiles)
        writeString(outputFile)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<MuxJob> = object : Parcelable.Creator<MuxJob> {
            override fun createFromParcel(source: Parcel): MuxJob = MuxJob(source)
            override fun newArray(size: Int): Array<MuxJob?> = arrayOfNulls(size)
        }
    }
}