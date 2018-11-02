package com.hmomeni.canto.activities

import android.content.Context
import android.media.AudioManager
import android.os.Bundle
import android.os.Environment
import android.support.v7.app.AppCompatActivity
import android.widget.Toast
import com.hmomeni.canto.R
import com.hmomeni.canto.utils.views.VerticalSlider
import kotlinx.android.synthetic.main.activity_karaoke.*
import java.io.File

class KaraokeActivity : AppCompatActivity() {
    init {
        System.loadLibrary("Canto")
    }

    private lateinit var filePath: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_karaoke)

        filePath = "/mnt/sdcard/Music/07-Mohsen_Chavoshi_To_Dar_Masafate_Barani.mp3"

        initAudio()

        playBtn.setOnClickListener {
            OpenFile(filePath, 0, File(filePath).length().toInt())
            TogglePlayback()
        }
        seekBar.max = 20

        pitchSlider.max = 20
        pitchSlider.progress = 10
        pitchSlider.onProgressChangeListener = object : VerticalSlider.OnSliderProgressChangeListener {
            override fun onChanged(progress: Int, max: Int) {
                SetPitch(progress - 10)
            }
        }

        tempoSlider.max = 20
        tempoSlider.progress = 10
        tempoSlider.onProgressChangeListener = object : VerticalSlider.OnSliderProgressChangeListener {
            override fun onChanged(progress: Int, max: Int) {
                SetTempo((progress / 10f).toDouble())
            }
        }
        volumeSlider.max = 50
        volumeSlider.progress = 10
        volumeSlider.onProgressChangeListener = object : VerticalSlider.OnSliderProgressChangeListener {
            override fun onChanged(progress: Int, max: Int) {
                SetVolume(progress / 10f)
            }
        }

        recordBtn.setOnClickListener {
            if (!IsPlaying() && !IsRecording()) {
                Toast.makeText(this, R.string.you_must_play_the_song_first, Toast.LENGTH_SHORT).show()
            } else if (IsPlaying() && !IsRecording()) {
                Toast.makeText(this, R.string.recording_started, Toast.LENGTH_SHORT).show()
                StartRecording()
            } else {
                Toast.makeText(this, R.string.recording_stopped, Toast.LENGTH_SHORT).show()
                StopRecording()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        onForeground()
    }

    override fun onPause() {
        super.onPause()
        onBackground()
    }

    override fun onDestroy() {
        super.onDestroy()
        Cleanup()
    }

    private fun initAudio() {
        val audioManager = this.getSystemService(Context.AUDIO_SERVICE) as AudioManager

        var samplerateString = audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE)
        var buffersizeString = audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER)

        if (samplerateString == null) samplerateString = "48000"
        if (buffersizeString == null) buffersizeString = "480"

        val sampleRate = Integer.parseInt(samplerateString)
        val bufferSize = Integer.parseInt(buffersizeString)

        val tempFilePath = File(filesDir, "temp.wav").absolutePath
        val finalFilePath = File(Environment.getExternalStorageDirectory(), "out.wav").absolutePath

        StartAudio(sampleRate, bufferSize, tempFilePath, finalFilePath)

    }

    private external fun StartAudio(samplerate: Int, buffersize: Int, tempFilePath: String, finalFilePath: String)
    private external fun OpenFile(path: String, offset: Int, length: Int)
    private external fun TogglePlayback()
    private external fun onForeground()
    private external fun onBackground()
    private external fun Cleanup()
    private external fun SetPitch(pitchShift: Int)
    private external fun SetTempo(tempo: Double)
    private external fun SetVolume(vol: Float)
    private external fun StartRecording()
    private external fun StopRecording()
    private external fun IsPlaying(): Boolean
    private external fun IsRecording(): Boolean
}
