package com.hmomeni.canto.utils

import androidx.room.TypeConverter
import com.hmomeni.canto.App
import com.hmomeni.canto.entities.Avatar

class TypeConvertors {
    @TypeConverter
    fun fromJson(value: String?): Avatar? = if (value == null) null else App.gson.fromJson(value, Avatar::class.java)

    @TypeConverter
    fun toJson(avatar: Avatar?): String? = if (avatar == null) null else App.gson.toJson(avatar)
}