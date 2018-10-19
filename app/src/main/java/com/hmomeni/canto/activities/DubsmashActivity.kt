package com.hmomeni.canto.activities

import android.content.Context
import android.media.AudioManager
import android.os.Bundle
import android.os.Environment
import com.hmomeni.canto.R
import com.hmomeni.canto.utils.views.AutoFitTextureView
import kotlinx.android.synthetic.main.activity_dubsmash.*
import java.io.File

class DubsmashActivity : CameraActivity() {

    init {
        System.loadLibrary("Dubsmash")
    }

    override fun getTextureView(): AutoFitTextureView = textureView

    override fun getVideoFilePath(): String {
        return File(Environment.getExternalStorageDirectory(), "dubsmash.mp4").absolutePath
    }

    override fun onRecordStarted() {
    }

    override fun onRecordStopped() {
    }

    override fun onRecordError() {
    }

    private lateinit var filePath: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dubsmash)

        filePath = "/mnt/sdcard/Music/07-Mohsen_Chavoshi_To_Dar_Masafate_Barani.mp3"

        initAudio()

        recordBtn.setOnClickListener {
            if (isRecordingVideo) {
                stopDubsmash()
            } else {
                startDubsmash()
            }
        }
    }

    private fun initAudio() {
        val audioManager = this.getSystemService(Context.AUDIO_SERVICE) as AudioManager

        var samplerateString = audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE)
        var buffersizeString = audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER)

        if (samplerateString == null) samplerateString = "48000"
        if (buffersizeString == null) buffersizeString = "480"

        val sampleRate = Integer.parseInt(samplerateString)
        val bufferSize = Integer.parseInt(buffersizeString)

        InitAudio(
                bufferSize,
                sampleRate,
                File(Environment.getExternalStorageDirectory(), "dubsmash.wav").absolutePath,
                File(Environment.getExternalStorageDirectory(), "temp.wav").absolutePath
        )
        OpenFile(filePath)
    }

    private fun startDubsmash() {
        StartAudio()
        startRecordingVideo()
    }

    private fun stopDubsmash() {
        StopAudio()
        stopRecordingVideo()
    }

    external fun InitAudio(bufferSize: Int, sampleRate: Int, outputPath: String, tempPath: String)
    external fun OpenFile(filePath: String): Double
    external fun TogglePlayback()
    external fun StartAudio()
    external fun StopAudio()
    external fun GetProgressMS(): Double
    external fun GetDurationMS(): Double
    external fun Seek(positionMS: Double)

}
