package com.hmomeni.canto.activities

import android.os.Bundle
import com.hmomeni.canto.R
import com.hmomeni.canto.utils.views.AutoFitTextureView
import kotlinx.android.synthetic.main.activity_singing.*
import java.io.File

class SingingActivity : CameraActivity() {

    init {
        System.loadLibrary("Canto")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_singing)
    }

    override fun getTextureView(): AutoFitTextureView {
        return textureView
    }

    override fun onRecordStarted() {
    }

    override fun onRecordStopped() {
    }

    override fun onRecordError() {
    }

    override fun getVideoFilePath(): String {
        return File(filesDir, "singing.mp4").absolutePath
    }
}
