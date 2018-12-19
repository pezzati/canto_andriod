package com.hmomeni.canto.utils

import android.Manifest

const val BASE_URL = "http://stg.canto-app.ir/"

const val REQUEST_VIDEO_PERMISSIONS = 1
val VIDEO_PERMISSIONS = listOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE)

const val FA_LANG = "fa"
const val EN_LANG = "en"

const val FFMPEG_URL = "http://185.208.175.123/ffmpeg/{arch}/ffmpeg"