package com.hmomeni.canto.entities

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.SerializedName

data class Post(
        @field:SerializedName("id")
        val id: Int,
        @field:SerializedName("name")
        val name: String? = null,
        @field:SerializedName("is_favorite")
        val isFavorite: Boolean = false,
        @field:SerializedName("like")
        val like: Int = 0,
        @field:SerializedName("popularity_rate")
        val popularityRate: Int = 0,
        @field:SerializedName("link")
        val link: String? = null,
        @field:SerializedName("description")
        val description: String? = null,
        @field:SerializedName("type")
        val type: String? = null,
        @field:SerializedName("content")
        val content: String? = null,
        /*@field:SerializedName("tags")
        val tags: List<String>? = null,*/
        @field:SerializedName("is_premium")
        val isPremium: Boolean = false,
        @field:SerializedName("created_date")
        val createdDate: String = "",
        @field:SerializedName("liked_it")
        val likedIt: Boolean = false,
        @field:SerializedName("artist")
        val artist: Artist? = null,
        @field:SerializedName("genre")
        val genre: Genre? = null,
        @field:SerializedName("cover_photo")
        val coverPhoto: CoverPhoto? = null
) : Parcelable {
    constructor(source: Parcel) : this(
            source.readInt(),
            source.readString(),
            1 == source.readInt(),
            source.readInt(),
            source.readInt(),
            source.readString(),
            source.readString(),
            source.readString(),
            source.readString(),
//            source.createStringArrayList(),
            1 == source.readInt(),
            source.readString(),
            1 == source.readInt(),
            source.readParcelable<Artist>(Artist::class.java.classLoader),
            source.readParcelable<Genre>(Genre::class.java.classLoader),
            source.readParcelable<CoverPhoto>(CoverPhoto::class.java.classLoader)
    )

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {
        writeInt(id)
        writeString(name)
        writeInt((if (isFavorite) 1 else 0))
        writeInt(like)
        writeInt(popularityRate)
        writeString(link)
        writeString(description)
        writeString(type)
        writeString(content)
//        writeStringList(tags)
        writeInt((if (isPremium) 1 else 0))
        writeString(createdDate)
        writeInt((if (likedIt) 1 else 0))
        writeParcelable(artist, 0)
        writeParcelable(genre, 0)
        writeParcelable(coverPhoto, 0)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<Post> = object : Parcelable.Creator<Post> {
            override fun createFromParcel(source: Parcel): Post = Post(source)
            override fun newArray(size: Int): Array<Post?> = arrayOfNulls(size)
        }
    }
}