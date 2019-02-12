package com.hmomeni.canto.utils

import android.Manifest

const val BASE_URL = "https://stg.canto-app.ir/"

const val REQUEST_VIDEO_PERMISSIONS = 1
val VIDEO_PERMISSIONS = listOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE)

const val FA_LANG = "fa"
const val EN_LANG = "en"

const val FFMPEG_URL = "${BASE_URL}static/ffmpeg/{arch}/ffmpeg"

const val HTTP_ERROR_PAYMENT_REQUIRED = 402
const val HTTP_ERROR_NOT_PURCHASED = 403

const val HANDSHAKE_FORCE_UPDATE = 1
const val HANDSHAKE_SUGGEST_UPDATE = 2
const val HANDSHAKE_GUEST = 3
const val HANDSHAKE_OK = 0