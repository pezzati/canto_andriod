package com.hmomeni.canto.persistence.typeconvertors

import androidx.room.TypeConverter
import com.hmomeni.canto.App
import com.hmomeni.canto.entities.Artist
import com.hmomeni.canto.entities.CantoFile
import com.hmomeni.canto.entities.Content
import com.hmomeni.canto.entities.Genre

class PostTypeConvertor {
    @TypeConverter
    fun toArtist(value: String?): Artist? = if (value == null) null else App.gson.fromJson(value, Artist::class.java)

    @TypeConverter
    fun fromArtist(value: Artist?): String? = if (value == null) null else App.gson.toJson(value)

    @TypeConverter
    fun toContent(value: String?): Content? = if (value == null) null else App.gson.fromJson(value, Content::class.java)

    @TypeConverter
    fun fromContent(value: Content?): String? = if (value == null) null else App.gson.toJson(value)

    @TypeConverter
    fun toGenre(value: String): Genre = App.gson.fromJson(value, Genre::class.java)

    @TypeConverter
    fun fromGenre(value: Genre): String = App.gson.toJson(value)

    @TypeConverter
    fun toCoverPhoto(value: String?): CantoFile? = if (value == null) null else App.gson.fromJson(value, CantoFile::class.java)

    @TypeConverter
    fun fromCoverPhoto(value: CantoFile?): String? = if (value == null) null else App.gson.toJson(value)
}