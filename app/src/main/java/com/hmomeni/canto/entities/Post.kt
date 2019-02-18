package com.hmomeni.canto.entities

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.SerializedName

data class Post(
        @SerializedName("id")
        val id: Int,
        @SerializedName("name")
        val name: String,
        @SerializedName("is_premium")
        val isPremium: Boolean = false,
        @SerializedName("artist")
        val artist: Artist? = null,
        @SerializedName("genre")
        val genre: Genre? = null,
        @SerializedName("cover_photo")
        val coverPhoto: CoverPhoto? = null,
        @SerializedName("price")
        var price: Long = 0,
        @SerializedName("count")
        var count: Int = 0
) : Parcelable {
    constructor(source: Parcel) : this(
            source.readInt(),
            source.readString()!!,
            1 == source.readInt(),
            source.readParcelable<Artist>(Artist::class.java.classLoader),
            source.readParcelable<Genre>(Genre::class.java.classLoader),
            source.readParcelable<CoverPhoto>(CoverPhoto::class.java.classLoader),
            source.readLong(),
            source.readInt()
    )

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {
        writeInt(id)
        writeString(name)
        writeInt((if (isPremium) 1 else 0))
        writeParcelable(artist, 0)
        writeParcelable(genre, 0)
        writeParcelable(coverPhoto, 0)
        writeLong(price)
        writeInt(count)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<Post> = object : Parcelable.Creator<Post> {
            override fun createFromParcel(source: Parcel): Post = Post(source)
            override fun newArray(size: Int): Array<Post?> = arrayOfNulls(size)
        }
    }
}