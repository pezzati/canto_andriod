package com.hmomeni.canto.persistence.typeconvertors

import android.arch.persistence.room.TypeConverter
import com.hmomeni.canto.App
import com.hmomeni.canto.entities.Artist
import com.hmomeni.canto.entities.Content
import com.hmomeni.canto.entities.CoverPhoto
import com.hmomeni.canto.entities.Genre

class PostTypeConvertor {
    @TypeConverter
    fun toArtist(value: String): Artist = App.gson.fromJson(value, Artist::class.java)

    @TypeConverter
    fun fromArtist(value: Artist): String = App.gson.toJson(value)

    @TypeConverter
    fun toContent(value: String): Content = App.gson.fromJson(value, Content::class.java)

    @TypeConverter
    fun fromContent(value: Content): String = App.gson.toJson(value)

    @TypeConverter
    fun toGenre(value: String): Genre = App.gson.fromJson(value, Genre::class.java)

    @TypeConverter
    fun fromGenre(value: Genre): String = App.gson.toJson(value)

    @TypeConverter
    fun toCoverPhoto(value: String): CoverPhoto = App.gson.fromJson(value, CoverPhoto::class.java)

    @TypeConverter
    fun fromCoverPhoto(value: CoverPhoto): String = App.gson.toJson(value)
}