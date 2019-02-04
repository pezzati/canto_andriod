package com.hmomeni.canto.activities

import android.content.Context
import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.util.SparseIntArray
import android.view.View
import android.widget.SeekBar
import android.widget.Toast
import com.azoft.carousellayoutmanager.CarouselLayoutManager
import com.azoft.carousellayoutmanager.CenterScrollListener
import com.hmomeni.canto.App
import com.hmomeni.canto.R
import com.hmomeni.canto.adapters.rcl.LyricRclAdapter
import com.hmomeni.canto.entities.FullPost
import com.hmomeni.canto.entities.MidiItem
import com.hmomeni.canto.services.*
import com.hmomeni.canto.utils.*
import com.hmomeni.canto.utils.views.RecordButton
import com.hmomeni.canto.utils.views.VerticalSlider
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.processors.PublishProcessor
import kotlinx.android.synthetic.main.activity_karaoke.*
import java.io.File
import javax.inject.Inject

class KaraokeActivity : BaseFullActivity() {
    init {
        System.loadLibrary("Karaoke")
    }

    private lateinit var filePath: String
    private lateinit var post: FullPost
    private var disposable: Disposable? = null
    private lateinit var timeMap: SparseIntArray

    private lateinit var midiItems: List<MidiItem>

    @Inject
    lateinit var downloadEvents: PublishProcessor<DownloadEvent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        app().di.inject(this)
        setContentView(R.layout.activity_karaoke)

        val details = KaraokeActivityArgs.fromBundle(intent.extras!!)
        post = App.gson.fromJson(details.post, FullPost::class.java)

        midiItems = post.content!!.midi!!.filter { it.text != "\n" }

        timeMap = preProcessLyric(midiItems)

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
                seekBar.max = GetDurationMS().toInt()
                playBtn.setImageResource(R.drawable.ic_pause_circle)
                timer()
                guideTextView1.gone()
                lyricRecyclerVIew.visible()
            } else {
                playBtn.setImageResource(R.drawable.ic_play_circle)
            }
        }

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    SeekMS(progress.toDouble())
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }
        })

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
        volumeSlider.max = 20
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

        lyricRecyclerVIew.layoutManager = object : CarouselLayoutManager(CarouselLayoutManager.VERTICAL) {
            override fun canScrollVertically(): Boolean {
                return false
            }
        }.apply {
            maxVisibleItems = 1
        }
        lyricRecyclerVIew.adapter = LyricRclAdapter(midiItems)
        lyricRecyclerVIew.setHasFixedSize(true)
        lyricRecyclerVIew.addOnScrollListener(CenterScrollListener())


        toggleLyricsBtn.setOnClickListener {
            if (lyricRecyclerVIew.visibility == View.GONE) {
                lyricRecyclerVIew.visible()
                lyricsBackground.visible()
                toggleLyricsBtn.setImageResource(R.drawable.ic_hide_lyric)
            } else {
                lyricRecyclerVIew.gone()
                lyricsBackground.gone()
                toggleLyricsBtn.setImageResource(R.drawable.ic_show_lyric)
            }
        }
        closeBtn.setOnClickListener {
            finish()
        }
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

    private var lastPos = -2
    private var handler = Handler()
    private fun timer() {
        val progressMs = GetProgressMS()
        seekBar.progress = progressMs.toInt()
        val sec = (progressMs / 1000).toInt()

        val pos = timeMap.get(sec, -1)
        if (pos >= 0 && lastPos != pos) {
//            Timber.d("sec=%d, pos=%d, lastPos=%d", sec, pos, lastPos)
            if (lastPos >= 0) {
                midiItems[lastPos].active = false
                lyricRecyclerVIew.adapter!!.notifyItemChanged(lastPos)
            }
            midiItems[pos].active = true
            lyricRecyclerVIew.adapter!!.notifyItemChanged(pos)

            lyricRecyclerVIew.scrollToPosition(pos)
            lastPos = pos
        }
        handler.postDelayed({
            if (IsPlaying()) {
                timer()
            }
        }, 300)
    }

    private fun handleDownloadEvents(event: DownloadEvent) {
        when (event.action) {
            ACTION_DOWNLOAD_START -> {
                recordBtn.mode = RecordButton.Mode.Loading
                guideTextView1.setText(R.string.downloading_song)

            }
            ACTION_DOWNLOAD_PROGRESS -> {
                if (recordBtn.mode != RecordButton.Mode.Loading) {
                    recordBtn.mode = RecordButton.Mode.Loading
                }
                recordBtn.progress = event.progress
            }
            ACTION_DOWNLOAD_FINISH -> {
                recordBtn.gone()
                playBtn.visible()
                guideTextView1.setText(R.string.tap_play_button_start_song)
            }
            ACTION_DOWNLOAD_FAILED -> {
                Toast.makeText(this, R.string.download_failed_try_again, Toast.LENGTH_SHORT).show()
            }
            ACTION_DOWNLOAD_CANCEL -> {

            }
        }
    }

    private fun preProcessLyric(midiItems: List<MidiItem>): SparseIntArray {
        val timeMap = SparseIntArray(midiItems.size)
        midiItems.forEachIndexed { i, v ->
            timeMap.append(v.time.toInt(), i)
        }
        return timeMap
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
    private external fun SeekMS(percent: Double)
    private external fun GetDurationMS(): Double
    private external fun GetProgressMS(): Double
}
