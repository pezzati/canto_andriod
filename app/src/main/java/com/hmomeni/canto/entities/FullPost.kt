package com.hmomeni.canto.entities

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.SerializedName

data class FullPost(
        @field:SerializedName("id")
        val id: Int,
        @field:SerializedName("name")
        val name: String = "",
        @field:SerializedName("description")
        val description: String = "",
        @field:SerializedName("type")
        val type: String,
        @field:SerializedName("is_premium")
        val isPremium: Boolean = false,
        @field:SerializedName("is_favorite")
        val isFavorite: Boolean = false,
        @field:SerializedName("like")
        val like: Int = 0,
        @field:SerializedName("popularity_rate")
        val popularityRate: Int = 0,
        @field:SerializedName("link")
        val link: String? = null,
        @field:SerializedName("created_date")
        val createdDate: String? = null,
        @field:SerializedName("liked_it")
        val likedIt: Boolean = false,
        @field:SerializedName("artist")
        val artist: Artist,
        @field:SerializedName("content")
        val content: Content,
        @field:SerializedName("genre")
        val genre: Genre? = null,
        @field:SerializedName("cover_photo")
        val coverPhoto: CoverPhoto? = null
) : Parcelable {
    constructor(source: Parcel) : this(
            source.readInt(),
            source.readString(),
            source.readString(),
            source.readString(),
            1 == source.readInt(),
            1 == source.readInt(),
            source.readInt(),
            source.readInt(),
            source.readString(),
            source.readString(),
            1 == source.readInt(),
            source.readParcelable<Artist>(Artist::class.java.classLoader),
            source.readParcelable<Content>(Content::class.java.classLoader),
            source.readParcelable<Genre>(Genre::class.java.classLoader),
            source.readParcelable<CoverPhoto>(CoverPhoto::class.java.classLoader)
    )

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {
        writeInt(id)
        writeString(name)
        writeString(description)
        writeString(type)
        writeInt((if (isPremium) 1 else 0))
        writeInt((if (isFavorite) 1 else 0))
        writeInt(like)
        writeInt(popularityRate)
        writeString(link)
        writeString(createdDate)
        writeInt((if (likedIt) 1 else 0))
        writeParcelable(artist, 0)
        writeParcelable(content, 0)
        writeParcelable(genre, 0)
        writeParcelable(coverPhoto, 0)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<FullPost> = object : Parcelable.Creator<FullPost> {
            override fun createFromParcel(source: Parcel): FullPost = FullPost(source)
            override fun newArray(size: Int): Array<FullPost?> = arrayOfNulls(size)
        }
    }
}