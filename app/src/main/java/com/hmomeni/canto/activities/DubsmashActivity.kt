package com.hmomeni.canto.activities

import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.view.View
import com.hmomeni.canto.*
import com.hmomeni.canto.utils.DownloadEvent
import com.hmomeni.canto.utils.ViewModelFactory
import com.hmomeni.canto.utils.app
import com.hmomeni.canto.utils.getDuration
import com.hmomeni.canto.utils.views.AutoFitTextureView
import com.hmomeni.canto.utils.views.RecordButton
import com.hmomeni.canto.utils.views.TrimView
import com.hmomeni.canto.utils.views.VerticalSlider
import com.hmomeni.canto.vms.DubsmashViewModel
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.activity_dubsmash.*
import timber.log.Timber
import java.io.File

class DubsmashActivity : CameraActivity() {

    init {
        System.loadLibrary("Canto")
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

    val RATIO_FULLSCREEN = 1
    val RATIO_SQUARE = 2

    private var audioInitialized: Boolean = false
    private var isPlaying: Boolean = false

    private lateinit var filePath: String
    private lateinit var fileUrl: String
    private var mRatio = RATIO_FULLSCREEN

    private lateinit var viewModel: DubsmashViewModel

    private var seekFraction = 0f

    private var disposable: Disposable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProviders.of(this, ViewModelFactory(app()))[DubsmashViewModel::class.java]

        disposable = viewModel.dEvents()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    handleDownloadEvents(it)
                }

        setContentView(R.layout.activity_dubsmash)

        fileUrl = "https://storage.backtory.com/cantotest/posts/Canto/karaokes/2018-8/K_125_Dreamon.mp3"
        filePath = DownloadService.startDownload(this, fileUrl)

        recordBtn.setOnClickListener {
            if (recordBtn.mode in arrayOf(RecordButton.Mode.Loading, RecordButton.Mode.Idle)) {
                return@setOnClickListener
            }
            if (isRecordingVideo) {
                stopDubsmash()
                recordBtn.mode = RecordButton.Mode.Ready
            } else {
                startDubsmash()
                recordBtn.mode = RecordButton.Mode.Recording
                timer()
            }
        }

        switchCam.setOnClickListener {
            switchCamera()
        }
        switchRatio.setOnClickListener {
            closeCamera()
            if (mRatio == RATIO_FULLSCREEN) {
                ratio = 4 / 3f
                mRatio = RATIO_SQUARE
            } else {
                ratio = 16 / 9f
                mRatio = RATIO_FULLSCREEN
            }
        }

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
    }

    private var handler = Handler()
    private fun timer() {
        val trimPos = GetProgressMS() * trimView.max / GetDurationMS()
        Timber.d("pos=%f, trimPos=%f", GetProgressMS(), trimPos)
        trimView.progress = (trimPos - trimView.trimStart).toInt()
        handler.postDelayed({
            if (isPlaying) {
                timer()
            }
        }, 1000)
    }

    override fun onStop() {
        disposable?.dispose()
        super.onStop()
    }

    private fun handleDownloadEvents(event: DownloadEvent) {
        when (event.action) {
            ACTION_DOWNLOAD_START -> {
                recordBtn.mode = RecordButton.Mode.Loading
            }
            ACTION_DOWNLOAD_PROGRESS -> {
                recordBtn.mode = RecordButton.Mode.Loading
                recordBtn.progress = event.progress
            }
            ACTION_DOWNLOAD_FINISH -> {
                prepare()
            }
            ACTION_DOWNLOAD_FAILED -> {

            }
            ACTION_DOWNLOAD_CANCEL -> {

            }
        }
    }

    private fun prepare() {
        val duration = getDuration(filePath)

        trimView.max = 100
        trimView.trim = (60000 * trimView.max / duration).toInt()
        trimView.maxTrim = trimView.trim
        trimView.minTrim = trimView.maxTrim

        trimView.visibility = View.VISIBLE
        settingsBtn.visibility = View.VISIBLE

        trimView.onTrimChangeListener = object : TrimView.TrimChangeListener() {
            override fun onRangeChanged(trimStart: Int, trim: Int) {
                val pos = trimStart * duration / trimView.max
                SeekMS(pos.toDouble())
            }
        }


        recordBtn.mode = RecordButton.Mode.Ready
        initAudio()
        OpenFile(filePath, File(filePath).length().toInt())
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
                File(Environment.getExternalStorageDirectory(), "dubsmash").absolutePath,
                File(filesDir, "temp").absolutePath
        )
        audioInitialized = true
    }

    private fun startDubsmash() {
        isPlaying = true
        StartAudio()
        startRecordingVideo()
    }

    private fun stopDubsmash() {
        isPlaying = false
        StopAudio()
        stopRecordingVideo()

        startActivity(Intent(this, EditActivity::class.java))
    }

    external fun InitAudio(bufferSize: Int, sampleRate: Int, outputPath: String, tempPath: String)
    external fun OpenFile(filePath: String, length: Int): Double
    external fun TogglePlayback()
    external fun StartAudio()
    external fun StopAudio()
    external fun GetProgressMS(): Double
    external fun GetDurationMS(): Double
    external fun Seek(percent: Double)
    external fun SeekMS(percent: Double)
    private external fun SetPitch(pitchShift: Int)
    private external fun SetTempo(tempo: Double)

}
