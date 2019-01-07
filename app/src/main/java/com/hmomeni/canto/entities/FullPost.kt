package com.hmomeni.canto.entities

import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey
import android.arch.persistence.room.TypeConverters
import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import com.hmomeni.canto.persistence.typeconvertors.PostTypeConvertor

@Entity
data class FullPost(
        @PrimaryKey(autoGenerate = false)
        @SerializedName("id")
        var id: Long,
        @SerializedName("name")
        var name: String,
        @SerializedName("is_premium")
        var isPremium: Boolean = false,
        @SerializedName("link")
        var link: String? = null,
        @SerializedName("liked_it")
        var likedIt: Boolean = false,
        @TypeConverters(PostTypeConvertor::class)
        @SerializedName("artist")
        var artist: Artist? = null,
        @TypeConverters(PostTypeConvertor::class)
        @SerializedName("content")
        var content: Content? = null,
        @TypeConverters(PostTypeConvertor::class)
        @SerializedName("cover_photo")
        var coverPhoto: CoverPhoto? = null,
        @SerializedName("price")
        var price: Long,
        @SerializedName("count")
        var count: Int
) : Parcelable {
    constructor(source: Parcel) : this(
            source.readLong(),
            source.readString()!!,
            1 == source.readInt(),
            source.readString(),
            1 == source.readInt(),
            source.readParcelable<Artist>(Artist::class.java.classLoader),
            source.readParcelable<Content>(Content::class.java.classLoader),
            source.readParcelable<CoverPhoto>(CoverPhoto::class.java.classLoader),
            source.readLong(),
            source.readInt()
    )

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {
        writeLong(id)
        writeString(name)
        writeInt((if (isPremium) 1 else 0))
        writeString(link)
        writeInt((if (likedIt) 1 else 0))
        writeParcelable(artist, 0)
        writeParcelable(content, 0)
        writeParcelable(coverPhoto, 0)
        writeLong(price)
        writeInt(count)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<FullPost> = object : Parcelable.Creator<FullPost> {
            override fun createFromParcel(source: Parcel): FullPost = FullPost(source)
            override fun newArray(size: Int): Array<FullPost?> = arrayOfNulls(size)
        }
    }
}