package com.hmomeni.canto.activities

import android.content.Context
import android.media.AudioManager
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import com.hmomeni.canto.App
import com.hmomeni.canto.R
import com.hmomeni.canto.entities.FullPost
import com.hmomeni.canto.services.*
import com.hmomeni.canto.utils.DownloadEvent
import com.hmomeni.canto.utils.GlideApp
import com.hmomeni.canto.utils.app
import com.hmomeni.canto.utils.views.RecordButton
import com.hmomeni.canto.utils.views.VerticalSlider
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.processors.PublishProcessor
import kotlinx.android.synthetic.main.activity_karaoke.*
import java.io.File
import javax.inject.Inject

class KaraokeActivity : BaseActivity() {
    init {
        System.loadLibrary("Karaoke")
    }

    private lateinit var filePath: String
    private lateinit var post: FullPost
    private var disposable: Disposable? = null

    @Inject
    lateinit var downloadEvents: PublishProcessor<DownloadEvent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        app().di.inject(this)

        setContentView(R.layout.activity_karaoke)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val extras = intent.extras!!
        post = App.gson.fromJson(extras.getString(INTENT_EXTRA_POST), FullPost::class.java)

        disposable = downloadEvents
                .onBackpressureDrop()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    handleDownloadEvents(it)
                }

        with(post.content!!) {
            val fileUrl = karaokeFileUrl
            filePath = DownloadService.startDownload(this@KaraokeActivity, fileUrl)
        }

        initAudio()

        playBtn.setOnClickListener {
            TogglePlayback()
            if (IsPlaying()) {
                playBtn.setImageResource(R.drawable.ic_pause_circle)
            } else {
                playBtn.setImageResource(R.drawable.ic_play_circle)
            }
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

        reverbSlider.max = 50
        reverbSlider.progress = 0
        reverbSlider.onProgressChangeListener = object : VerticalSlider.OnSliderProgressChangeListener {
            override fun onChanged(progress: Int, max: Int) {
                SetReverb(progress / 50f)
            }
        }
        GlideApp.with(background)
                .load(R.drawable.lubia)
                .dontTransform()
                .into(background)
        background.startRotation()
    }

    override fun onResume() {
        super.onResume()
        onForeground()
    }

    override fun onPause() {
        onBackground()
        super.onPause()
    }

    override fun onDestroy() {
        Cleanup()
        disposable?.dispose()
        super.onDestroy()
    }

    private fun initAudio() {
        val audioManager = this.getSystemService(Context.AUDIO_SERVICE) as AudioManager

        var samplerateString = audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE)
        var buffersizeString = audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER)

        if (samplerateString == null) samplerateString = "48000"
        if (buffersizeString == null) buffersizeString = "480"

        val sampleRate = Integer.parseInt(samplerateString)
        val bufferSize = Integer.parseInt(buffersizeString)

        StartAudio(sampleRate, bufferSize)
        OpenFile(filePath, 0, File(filePath).length().toInt())
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
                recordBtn.visibility = View.GONE
                playBtn.visibility = View.VISIBLE
            }
            ACTION_DOWNLOAD_FAILED -> {
                Toast.makeText(this, R.string.download_failed_try_again, Toast.LENGTH_SHORT).show()
            }
            ACTION_DOWNLOAD_CANCEL -> {

            }
        }
    }

    private external fun StartAudio(samplerate: Int, buffersize: Int)
    private external fun OpenFile(path: String, offset: Int, length: Int)
    private external fun TogglePlayback()
    private external fun onForeground()
    private external fun onBackground()
    private external fun Cleanup()
    private external fun SetPitch(pitchShift: Int)
    private external fun SetTempo(tempo: Double)
    private external fun SetVolume(vol: Float)
    private external fun SetReverb(amount: Float)
    private external fun IsPlaying(): Boolean

}
