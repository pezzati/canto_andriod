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
        val id: Long,
        @SerializedName("name")
        val name: String,
        @SerializedName("description")
        val description: String,
        @SerializedName("type")
        val type: String,
        @SerializedName("is_premium")
        val isPremium: Boolean = false,
        @SerializedName("is_favorite")
        val isFavorite: Boolean = false,
        @SerializedName("like")
        val like: Int = 0,
        @SerializedName("popularity_rate")
        val popularityRate: Int = 0,
        @SerializedName("link")
        val link: String,
        @SerializedName("created_date")
        val createdDate: String? = null,
        @SerializedName("liked_it")
        val likedIt: Boolean = false,
        @TypeConverters(PostTypeConvertor::class)
        @SerializedName("artist")
        val artist: Artist,
        @TypeConverters(PostTypeConvertor::class)
        @SerializedName("content")
        val content: Content,
        @TypeConverters(PostTypeConvertor::class)
        @SerializedName("genre")
        val genre: Genre? = null,
        @TypeConverters(PostTypeConvertor::class)
        @SerializedName("cover_photo")
        val coverPhoto: CoverPhoto? = null
) : Parcelable {
    constructor(source: Parcel) : this(
            source.readLong(),
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
        writeLong(id)
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