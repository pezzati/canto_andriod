package com.hmomeni.canto.activities

import android.os.Bundle
import com.hmomeni.canto.R
import com.hmomeni.canto.utils.views.AutoFitTextureView
import kotlinx.android.synthetic.main.activity_dubsmash.*

class DubsmashActivity : CameraActivity() {
    override fun getTextureView(): AutoFitTextureView = textureView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dubsmash)

        recordBtn.setOnClickListener {
            if (isRecordingVideo) {
                stopRecordingVideo()
            } else {
                startRecordingVideo()
            }
        }
    }


}
