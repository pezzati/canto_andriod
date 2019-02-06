package com.hmomeni.canto.activities

import android.animation.Animator
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.util.SparseIntArray
import android.view.TextureView
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import com.azoft.carousellayoutmanager.CarouselLayoutManager
import com.azoft.carousellayoutmanager.CenterScrollListener
import com.hmomeni.canto.App
import com.hmomeni.canto.R
import com.hmomeni.canto.adapters.rcl.LyricRclAdapter
import com.hmomeni.canto.entities.FullPost
import com.hmomeni.canto.entities.MidiItem
import com.hmomeni.canto.entities.PROJECT_TYPE_DUBSMASH
import com.hmomeni.canto.entities.PROJECT_TYPE_SINGING
import com.hmomeni.canto.services.*
import com.hmomeni.canto.utils.*
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

const val INTENT_EXTRA_POST = "record_post"
const val INTENT_EXTRA_TYPE = "record_type"
const val INTENT_EXTRA_RATIO = "record_ratio"
const val INTENT_EXTRA_TIMESTAMP = "time_stamp"

class DubsmashActivity : CameraActivity() {
    val timeStamp = System.currentTimeMillis()

    init {
        System.loadLibrary("Dubsmash")
    }

    override fun getTextureView(): TextureView = textureView

    override fun getVideoFilePath(): String {
        return File(baseFile, "dubsmash-$timeStamp.mp4").absolutePath
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
        baseFile = cacheDir

        app().di.inject(this)

        val details = DubsmashActivityArgs.fromBundle(intent.extras!!)
        type = details.type
        post = App.gson.fromJson(details.post, FullPost::class.java)


        midiItems = post.content!!.midi!!.filter { it.text != "\n" }

        timeMap = preProcessLyric(midiItems)

        disposable = downloadEvents
                .onBackpressureDrop()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    handleDownloadEvents(it)
                }

        setContentView(R.layout.activity_dubsmash)

        if (type == PROJECT_TYPE_DUBSMASH) {
            pageTitle.setText(R.string.dubsmash)
        }

        with(post.content!!) {
            fileUrl = if (type == PROJECT_TYPE_DUBSMASH) {
                originalFileUrl
            } else {
                karaokeFileUrl
            }
        }

        if (fileUrl.isEmpty()) {
            Toast.makeText(this, R.string.audio_track_not_found, Toast.LENGTH_SHORT).show()
            finish()
            return
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
//                startDubsmash()
                startRecordCountdown()
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
        if (type == PROJECT_TYPE_DUBSMASH) {
            lyricRecyclerVIew.visibility = View.GONE
            toggleLyricsBtn.visibility = View.GONE
        } else {
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
                    lyricsBackground.visible()
                    lyricRecyclerVIew.visible()
                    toggleLyricsBtn.setImageResource(R.drawable.ic_hide_lyric)
                } else {
                    lyricsBackground.gone()
                    lyricRecyclerVIew.gone()
                    toggleLyricsBtn.setImageResource(R.drawable.ic_show_lyric)
                }
            }
        }

        closeBtn.setOnClickListener {
            finish()
        }

    }

    private var lastPos = -2
    private var handler = Handler()
    private fun timer() {
        val progressMs = GetProgressMS()

        val sec = (progressMs / 1000).toInt()

        val pos = timeMap.get(sec, -1)

        if (pos >= 0 && lastPos != pos && type == PROJECT_TYPE_SINGING) {
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
            if (diff >= 60L) {
                stopDubsmash()
            }
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
                guideTextView1.setText(R.string.downloading_song)
            }
            ACTION_DOWNLOAD_PROGRESS -> {
                if (recordBtn.mode != RecordButton.Mode.Loading) {
                    recordBtn.mode = RecordButton.Mode.Loading
                }
                recordBtn.progress = event.progress
            }
            ACTION_DOWNLOAD_FINISH -> {
                prepare()
                guideTextView1.setText(R.string.select_60_sec_of_song)
                guideTextView2.setText(R.string.tap_record_to_start)
            }
            ACTION_DOWNLOAD_FAILED -> {
                Toast.makeText(this, R.string.download_failed_try_again, Toast.LENGTH_SHORT).show()
                guideTextView1.setText(R.string.download_failed_try_again)
                guideTextView2.gone()

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

        slidersWrapper.visible()
        trimView.visible()
        settingsBtn.visible()

        settingsBtn.setOnClickListener {
            toggleSettings()
        }

        trimView.onTrimChangeListener = object : TrimView.TrimChangeListener() {
            override fun onRangeChanged(trimStart: Int, trim: Int) {
                guideTextView1.setText(R.string.tap_record_to_start)
                guideTextView2.gone()
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
                File(baseFile, "dubsmash-$timeStamp").absolutePath,
                File(baseFile, "temp-$timeStamp").absolutePath,
                File(baseFile, "dubsmash-mic-$timeStamp").absolutePath,
                File(baseFile, "tempmic-$timeStamp").absolutePath
        )
    }

    private fun startPlayBack() {
        isPlaying = true
        StartAudio()
        timer()
    }

    private var countDown = 3
    private fun startRecordCountdown() {
        if (countDown < 0) {
            if (!isDestroyed) {
                startDubsmash()
            }
            return
        }
        val textView = TextView(this).apply {
            layoutParams = ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.WRAP_CONTENT, ConstraintLayout.LayoutParams.WRAP_CONTENT)
            textSize = dpToPx(30).toFloat()
            setTextColor(Color.WHITE)
            text = if (countDown > 0) {
                countDown.toString()
            } else {
                "Go!"
            }
            id = View.generateViewId()
        }
        wrapper.addView(textView)
        val constraintSet = ConstraintSet()
        constraintSet.clone(wrapper)

        constraintSet.connect(textView.id, ConstraintSet.LEFT, wrapper.id, ConstraintSet.LEFT)
        constraintSet.connect(textView.id, ConstraintSet.RIGHT, wrapper.id, ConstraintSet.RIGHT)
        constraintSet.connect(textView.id, ConstraintSet.TOP, wrapper.id, ConstraintSet.TOP)
        constraintSet.connect(textView.id, ConstraintSet.BOTTOM, wrapper.id, ConstraintSet.BOTTOM)

        constraintSet.applyTo(wrapper)
        textView.animate().setDuration(1000).scaleX(3f).scaleY(3f).alpha(0f).setListener(object : MyAnimatorListener() {
            override fun onAnimationEnd(animation: Animator?) {
                countDown--
                wrapper.removeView(textView)
                startRecordCountdown()
            }
        })
    }

    private var recordStartTime = 0L
    private fun startDubsmash() {
        guideTextView1.gone()
        guideTextView2.gone()
        if (type == PROJECT_TYPE_DUBSMASH) {
            lyricsBackground.gone()
        } else {
            lyricRecyclerVIew.visible()
        }
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
        startActivity(
                Intent(this, EditActivity::class.java)
                        .putExtra(INTENT_EXTRA_TYPE, type)
                        .putExtra(INTENT_EXTRA_POST, App.gson.toJson(post))
                        .putExtra(INTENT_EXTRA_RATIO, mRatio)
                        .putExtra(INTENT_EXTRA_TIMESTAMP, timeStamp)
        )
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
    private external fun StartAudio()
    private external fun StartRecording()
    private external fun StopAudio()
    private external fun GetProgressMS(): Double
    private external fun GetDurationMS(): Double
    private external fun SeekMS(percent: Double)
    private external fun SetPitch(pitchShift: Int)
    private external fun SetTempo(tempo: Double)
    private external fun Cleanup()

}
