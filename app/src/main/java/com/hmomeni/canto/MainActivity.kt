package com.hmomeni.canto

import android.Manifest
import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.support.v7.app.AppCompatActivity
import android.view.View
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File

class MainActivity : AppCompatActivity() {

    init {
        System.loadLibrary("native-lib")
    }

    private var isPlaying: Boolean = false
    private var isInit = false
    private var isRecording = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Dexter.withActivity(this)
                .withPermissions(listOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE))
                .withListener(object : MultiplePermissionsListener {
                    override fun onPermissionsChecked(report: MultiplePermissionsReport) {
                        if (report.grantedPermissionResponses.size == 2) {
                            playBtn.visibility = View.VISIBLE
                            recordBtn.visibility = View.VISIBLE
                            initializeAudio()
                        }
                    }

                    override fun onPermissionRationaleShouldBeShown(permissions: MutableList<PermissionRequest>, token: PermissionToken) {
                        token.continuePermissionRequest()
                    }

                })
                .check()

        playBtn.setOnClickListener {
            if (!isInit) {
                openFile()
            }
            TogglePlayback()
            isPlaying = !isPlaying
        }

        recordBtn.setOnClickListener {
            if (!isRecording) {
                val destPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).toString() + "/karaoke"
                StartRecording(destPath)
                recImg.visibility = View.VISIBLE
                isRecording = true
            } else {
                StopRecording()
            }
        }
    }


    public override fun onPause() {
        super.onPause()
        if (isPlaying)
            onBackground()
    }

    public override fun onResume() {
        super.onResume()
        if (isPlaying)
            onForeground()
    }

    override fun onDestroy() {
        super.onDestroy()
        Cleanup()
    }


    private fun initializeAudio() {
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

        val tempPath = cacheDir.absolutePath + "/temp.wav"  // temporary file path

        startAudio(sampleRate, bufferSize, tempPath)
    }

    private fun openFile() {
        isInit = true
        val file = File(Environment.getExternalStorageDirectory(), "Music/sample.mp3")
        OpenFile(file.absolutePath, 0, file.length().toInt())
    }

    private external fun startAudio(sampleRate: Int, bufferSize: Int, tempPath: String)
    private external fun OpenFile(path: String, offset: Int, length: Int)
    private external fun TogglePlayback()
    private external fun onForeground()
    private external fun onBackground()
    private external fun Cleanup()
    private external fun StopRecording()
    private external fun StartRecording(destPath: String)

}
