package com.hmomeni.canto.activities

import android.app.ProgressDialog
import android.content.Context
import android.media.AudioManager
import android.os.Bundle
import android.os.Environment
import android.support.v7.app.AppCompatActivity
import com.hmomeni.canto.R
import com.hmomeni.canto.utils.getDuration
import com.hmomeni.trimview.TrimView
import kotlinx.android.synthetic.main.activity_edit.*
import java.io.File
import kotlin.concurrent.thread

class EditActivity : AppCompatActivity() {
    init {
        System.loadLibrary("Canto")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit)
        initAudio()

        val audioFile = File(Environment.getExternalStorageDirectory(), "dubsmash.wav")
        val videoFile = File(filesDir, "dubsmash.mp4").absolutePath

        val duration = getDuration(audioFile.absolutePath)
        OpenFile(audioFile.absolutePath, audioFile.length().toInt())

        playBtn.setOnClickListener {
            StartAudio()
        }

        trimView.max = 100
        trimView.trim = 100
        trimView.minTrim = 20

        trimView.onTrimChangeListener = object : TrimView.TrimChangeListener() {
            override fun onRangeChanged(trimStart: Int, trim: Int) {
                val pos = trimStart * duration / trimView.max
                SeekMS(pos.toDouble())
            }
        }

        saveBtn.setOnClickListener {
            StopAudio()
            val dialog = ProgressDialog(this)
            dialog.show()
            thread {
                val from = trimView.trimStart * duration / trimView.max
                val to = from + trimView.trim * duration / trimView.max
                CropSave(audioFile.absolutePath, File(Environment.getExternalStorageDirectory(), "final.wav").absolutePath, from, to, duration)
                runOnUiThread {
                    dialog.dismiss()
                }
            }
        }
    }

    private var audioInitialized: Boolean = false

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
                sampleRate
        )
        audioInitialized = true
    }

    external fun InitAudio(bufferSize: Int, sampleRate: Int)
    external fun OpenFile(filePath: String, length: Int): Double
    external fun TogglePlayback()
    external fun StartAudio()
    external fun StopAudio()
    external fun GetProgressMS(): Double
    external fun GetDurationMS(): Double
    external fun Seek(percent: Double)
    external fun SeekMS(percent: Double)
    external fun CropSave(sourcePath: String, destPath: String, from: Long, to: Long, total: Long)
}
