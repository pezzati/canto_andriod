package com.hmomeni.canto.activities

import android.Manifest
import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.support.v7.app.AppCompatActivity
import android.view.View
import com.github.angads25.filepicker.model.DialogConfigs
import com.github.angads25.filepicker.model.DialogProperties
import com.github.angads25.filepicker.view.FilePickerDialog
import com.hmomeni.canto.R
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import kotlinx.android.synthetic.main.activity_player.*
import java.io.File


class PlayerActivity : AppCompatActivity() {
    init {
        System.loadLibrary("Player")
    }

    private var isPlaying = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)

        Dexter.withActivity(this)
                .withPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .withListener(object : PermissionListener {
                    override fun onPermissionRationaleShouldBeShown(permission: PermissionRequest, token: PermissionToken) {
                        token.continuePermissionRequest()
                    }

                    override fun onPermissionDenied(response: PermissionDeniedResponse) {

                    }

                    override fun onPermissionGranted(response: PermissionGrantedResponse) {
                        browseBtn.text = "Browse for File"
                        browseBtn.isClickable = true
                        initAudio()
                    }
                }).check()

        browseBtn.setOnClickListener {
            val properties = DialogProperties()
            properties.selection_mode = DialogConfigs.SINGLE_MODE
            properties.selection_type = DialogConfigs.FILE_SELECT
            properties.root = File(DialogConfigs.DEFAULT_DIR)
            properties.error_dir = File(DialogConfigs.DEFAULT_DIR)
            properties.offset = File(DialogConfigs.DEFAULT_DIR)
            properties.extensions = null
            val dialog = FilePickerDialog(this, properties)
            dialog.setTitle("Select a File")

            dialog.setDialogSelectionListener {
                val file = File(it[0])
                val duration = OpenFile(file.absolutePath, 0, file.length().toInt())
                seekBar.max = duration.toInt()
                displayPlayer()
            }
            dialog.show()

        }

        toggleBtn.setOnClickListener {
            TogglePlayback()
            seekBar.max = GetDurationMS().toInt()
            isPlaying = !isPlaying
            if (isPlaying) {
                timer()
            }
        }

    }


    private val handler = Handler()
    private fun timer() {
        seekBar.progress = GetProgressMS().toInt()
        if (isPlaying) {
            handler.postDelayed({ timer() }, 1000)
        }
    }

    private fun displayPlayer() {
        textView.visibility = View.VISIBLE
        fileName.visibility = View.VISIBLE
        toggleBtn.visibility = View.VISIBLE
        seekBar.visibility = View.VISIBLE
    }

    private fun initAudio() {
        var samplerateString: String? = null
        var buffersizeString: String? = null
        if (Build.VERSION.SDK_INT >= 17) {
            val audioManager = this.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            samplerateString = audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE)
            buffersizeString = audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER)
        }
        if (samplerateString == null) samplerateString = "48000"
        if (buffersizeString == null) buffersizeString = "480"

        val sampleRate = Integer.parseInt(samplerateString)
        val bufferSize = Integer.parseInt(buffersizeString)
        InitAudio(bufferSize, sampleRate)
    }

    external fun InitAudio(bufferSize: Int, sampleRate: Int)
    external fun OpenFile(filePath: String, offset: Int, length: Int): Double
    external fun TogglePlayback()
    external fun GetProgressMS(): Double
    external fun GetDurationMS(): Double
    external fun Seek(positionMS: Double)
}
