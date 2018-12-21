package com.hmomeni.canto.activities

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.util.SparseIntArray
import android.view.TextureView
import android.view.View
import android.widget.Toast
import com.azoft.carousellayoutmanager.CarouselLayoutManager
import com.hmomeni.canto.*
import com.hmomeni.canto.adapters.rcl.LyricRclAdapter
import com.hmomeni.canto.entities.FullPost
import com.hmomeni.canto.entities.MidiItem
import com.hmomeni.canto.entities.PROJECT_TYPE_DUBSMASH
import com.hmomeni.canto.entities.PROJECT_TYPE_SINGING
import com.hmomeni.canto.utils.DownloadEvent
import com.hmomeni.canto.utils.app
import com.hmomeni.canto.utils.getBitmapFromVectorDrawable
import com.hmomeni.canto.utils.getDuration
import com.hmomeni.canto.utils.views.RecordButton
import com.hmomeni.canto.utils.views.TrimView
import com.hmomeni.canto.utils.views.VerticalSlider
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.processors.PublishProcessor
import kotlinx.android.synthetic.main.activity_dubsmash.*
import java.io.File
import javax.inject.Inject

const val RATIO_FULLSCREEN = 1
const val RATIO_SQUARE = 2

class DubsmashActivity : CameraActivity() {


    init {
        System.loadLibrary("Dubsmash")
    }

    override fun getTextureView(): TextureView = textureView

    override fun getVideoFilePath(): String {
        return File(baseFile, "dubsmash.mp4").absolutePath
    }

    override fun onRecordStarted() {
    }

    override fun onRecordStopped() {
    }

    override fun onRecordError() {
    }

    override fun onTextureAvailable(w: Int, h: Int) {
        cropTop.layoutParams = cropTop.layoutParams.apply {
            height = h / 2 - w / 2
        }
        cropBottom.layoutParams = cropBottom.layoutParams.apply {
            height = h / 2 - w / 2
        }
    }


    private var audioInitialized: Boolean = false
    private var isPlaying: Boolean = false
    private var isRecording: Boolean = false

    private lateinit var filePath: String
    private lateinit var fileUrl: String
    private var mRatio = RATIO_FULLSCREEN

    private var disposable: Disposable? = null

    private lateinit var timeMap: SparseIntArray

    private lateinit var midiItems: List<MidiItem>

    private lateinit var post: FullPost

    private lateinit var baseFile: File


    var type: Int = PROJECT_TYPE_DUBSMASH

    @Inject
    lateinit var downloadEvents: PublishProcessor<DownloadEvent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        baseFile = filesDir

        app().di.inject(this)

        type = intent.getIntExtra("type", type)
        post = intent.getParcelableExtra("post")

        midiItems = post.content.midi.filter { it.text != "\n" }

        timeMap = preProcessLyric(midiItems)

        disposable = downloadEvents
                .onBackpressureDrop()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    handleDownloadEvents(it)
                }

        setContentView(R.layout.activity_dubsmash)

        with(post.content) {
            fileUrl = if (!originalFileUrl.isEmpty()) {
                originalFileUrl
            } else {
                karaokeFileUrl
            }
        }

        filePath = DownloadService.startDownload(this, fileUrl)

        recordBtn.setOnClickListener {
            if (recordBtn.mode in arrayOf(RecordButton.Mode.Loading, RecordButton.Mode.Idle)) {
                return@setOnClickListener
            }
            if (isRecording) {
                stopDubsmash()
                recordBtn.mode = RecordButton.Mode.Ready
            } else {
                startDubsmash()
                recordBtn.mode = RecordButton.Mode.Recording
            }
        }

        switchCam.setOnClickListener {
            if (!isRecording) {
                switchCamera()
            }
        }
        switchRatio.setOnClickListener {
            if (mRatio == RATIO_FULLSCREEN) {
                cropTop.visibility = View.GONE
                cropBottom.visibility = View.GONE
                mRatio = RATIO_SQUARE
            } else {
                cropTop.visibility = View.VISIBLE
                cropBottom.visibility = View.VISIBLE
                mRatio = RATIO_FULLSCREEN
            }
        }

        pitchSlider.lowIcon = getBitmapFromVectorDrawable(this, R.drawable.ic_pitch_low)
        pitchSlider.midIcon = getBitmapFromVectorDrawable(this, R.drawable.ic_pitch_mid)
        pitchSlider.hiIcon = getBitmapFromVectorDrawable(this, R.drawable.ic_pitch_hi)

        pitchSlider.max = 20
        pitchSlider.progress = 10
        pitchSlider.onProgressChangeListener = object : VerticalSlider.OnSliderProgressChangeListener {
            override fun onChanged(progress: Int, max: Int) {
                SetPitch(progress - 10)
            }
        }

        tempoSlider.lowIcon = getBitmapFromVectorDrawable(this, R.drawable.ic_speed_low)
        tempoSlider.midIcon = getBitmapFromVectorDrawable(this, R.drawable.ic_speed_mid)
        tempoSlider.hiIcon = getBitmapFromVectorDrawable(this, R.drawable.ic_speed_hi)

        tempoSlider.max = 20
        tempoSlider.progress = 10
        tempoSlider.onProgressChangeListener = object : VerticalSlider.OnSliderProgressChangeListener {
            override fun onChanged(progress: Int, max: Int) {
                SetTempo((progress / 10f).toDouble())
            }
        }

        lyricRecyclerVIew.layoutManager = object : CarouselLayoutManager(CarouselLayoutManager.VERTICAL) {
            override fun canScrollVertically(): Boolean {
                return false
            }
        }
        lyricRecyclerVIew.adapter = LyricRclAdapter(midiItems)


        toggleLyricsBtn.setOnClickListener {
            if (lyricRecyclerVIew.visibility == View.GONE) {
                lyricRecyclerVIew.visibility = View.VISIBLE
            } else {
                lyricRecyclerVIew.visibility = View.GONE
            }
        }

    }

    private var lastPos = -2
    private var handler = Handler()
    private fun timer() {
        val progressMs = GetProgressMS()

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

        if (!isRecording) {
            val trimPos = progressMs * trimView.max / GetDurationMS()
            trimView.progress = (trimPos - trimView.trimStart).toInt()
        } else {
            val diff = (System.currentTimeMillis() - recordStartTime) / 1000
            timerText.text = "%d:%02d".format(diff / 60, diff % 60)
        }

        handler.postDelayed({
            if (isPlaying) {
                timer()
            }
        }, 300)
    }

    override fun onStop() {
        disposable?.dispose()
        super.onStop()
    }

    override fun onDestroy() {
        Cleanup()
        super.onDestroy()
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
                Toast.makeText(this, R.string.download_failed_try_again, Toast.LENGTH_SHORT).show()
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

        settingsBtn.setOnClickListener {
            toggleSettings()
        }

        trimView.onTrimChangeListener = object : TrimView.TrimChangeListener() {
            override fun onRangeChanged(trimStart: Int, trim: Int) {
                val pos = trimStart * duration / trimView.max
                SeekMS(pos.toDouble())
            }
        }


        recordBtn.mode = RecordButton.Mode.Ready
        initAudio()
        OpenFile(filePath, File(filePath).length().toInt())
        startPlayBack()
    }

    private var sampleRate: Int = 0

    private var bufferSize: Int = 0

    private fun initAudio() {
        val audioManager = this.getSystemService(Context.AUDIO_SERVICE) as AudioManager

        var samplerateString = audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE)
        var buffersizeString = audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER)

        if (samplerateString == null) samplerateString = "48000"
        if (buffersizeString == null) buffersizeString = "480"

        sampleRate = Integer.parseInt(samplerateString)
        bufferSize = Integer.parseInt(buffersizeString)

        InitAudio(
                bufferSize,
                sampleRate,
                type == PROJECT_TYPE_SINGING,
                File(baseFile, "dubsmash").absolutePath,
                File(baseFile, "temp").absolutePath,
                File(baseFile, "dubsmash-mic").absolutePath,
                File(baseFile, "tempmic").absolutePath
        )
        audioInitialized = true
    }

    private fun startPlayBack() {
        isPlaying = true
        StartAudio()
        timer()
    }

    private var recordStartTime = 0L
    private fun startDubsmash() {
        val pos = trimView.trimStart * GetDurationMS() / trimView.max
        SeekMS(pos)
        recordStartTime = System.currentTimeMillis()
        isRecording = true
        startRecordingVideo()
        StartRecording()
        trimView.visibility = View.GONE
        timerText.visibility = View.VISIBLE
        switchCam.alpha = 0.3f
    }

    private fun stopDubsmash() {
        isPlaying = false
        isRecording = false
        stopRecordingVideo()
        StopAudio()
        startActivity(Intent(this, EditActivity::class.java).putExtra("type", type).putExtra("post", post).putExtra("ratio", mRatio))
        finish()
    }

    private fun preProcessLyric(midiItems: List<MidiItem>): SparseIntArray {
        val timeMap = SparseIntArray(midiItems.size)
        midiItems.forEachIndexed { i, v ->
            timeMap.append(v.time.toInt(), i)
        }
        return timeMap
    }

    private fun toggleSettings() {
        slidersWrapper.visibility = if (slidersWrapper.visibility == View.GONE) View.VISIBLE else View.GONE
    }

    private external fun InitAudio(bufferSize: Int, sampleRate: Int, isSinging: Boolean, outputPath: String, tempPath: String, outputPathMic: String, tempPathMic: String)
    private external fun OpenFile(filePath: String, length: Int): Double
    private external fun TogglePlayback()
    private external fun StartAudio()
    private external fun StartRecording()
    private external fun StopAudio()
    private external fun GetProgressMS(): Double
    private external fun GetDurationMS(): Double
    private external fun Seek(percent: Double)
    private external fun SeekMS(percent: Double)
    private external fun SetPitch(pitchShift: Int)
    private external fun SetTempo(tempo: Double)
    private external fun IsPlaying(): Boolean
    private external fun Cleanup()

}
