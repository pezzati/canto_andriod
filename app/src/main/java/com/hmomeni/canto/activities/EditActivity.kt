package com.hmomeni.canto.activities

import android.content.Context
import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Environment
import android.text.format.DateFormat
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.widget.Toast
import com.crashlytics.android.Crashlytics
import com.hmomeni.canto.App
import com.hmomeni.canto.R
import com.hmomeni.canto.entities.*
import com.hmomeni.canto.services.MuxerService
import com.hmomeni.canto.utils.CantoDialog
import com.hmomeni.canto.utils.addUserAction
import com.hmomeni.canto.utils.getDuration
import com.hmomeni.canto.utils.views.VerticalSlider
import kotlinx.android.synthetic.main.activity_edit.*
import timber.log.Timber
import java.io.File
import java.util.*
import kotlin.concurrent.thread

class EditActivity : BaseFullActivity(), View.OnClickListener {

    init {
        System.loadLibrary("Edit")
    }

    private val mediaPlayer = MediaPlayer()

    private lateinit var audioFile: File
    private lateinit var micFile: File
    private lateinit var videoFile: File

    private var type: Int = PROJECT_TYPE_SINGING
    private lateinit var post: FullPost

    private lateinit var baseDir: File
    private lateinit var outFile: File

    private var ratio: Int = RATIO_FULLSCREEN
    private var timeStamp: Long = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit)

        baseDir = cacheDir
        val outDir = File(Environment.getExternalStorageDirectory(), "Canto")

        outDir.mkdirs()

        outFile = File(outDir, "Canto_%s_%s.mp4".format(Locale.ENGLISH,
                if (type == PROJECT_TYPE_SINGING) "singing" else "dubsmash",
                DateFormat.format("yyyy-MM-dd-hh-mm-ss", Date())
        ))

        type = intent.getIntExtra(INTENT_EXTRA_TYPE, type)
        post = App.gson.fromJson(intent.getStringExtra(INTENT_EXTRA_POST), FullPost::class.java)
        ratio = intent.getIntExtra(INTENT_EXTRA_RATIO, RATIO_FULLSCREEN)
        timeStamp = intent.getLongExtra(INTENT_EXTRA_TIMESTAMP, 0L)


        initAudio()

        audioFile = File(baseDir, "dubsmash-$timeStamp.wav")
        micFile = File(baseDir, "dubsmash-mic-$timeStamp.wav")
        videoFile = File(baseDir, "dubsmash-$timeStamp.mp4")

        val duration = getDuration(audioFile.absolutePath)

        Timber.d("FileDuration=%d", duration)

        OpenFile(audioFile.absolutePath, audioFile.length().toInt(), micFile.absolutePath, micFile.length().toInt())

        saveBtn.setOnClickListener {
            StopAudio()
            mediaPlayer.stop()
            doMux()
            addUserAction(UserAction("Save tapped", post.id.toString(), if (type == PROJECT_TYPE_DUBSMASH) "Dubsmash" else "Singing"))
        }

        try {
            mediaPlayer.setDataSource(videoFile.absolutePath)
        } catch (e: Exception) {
            Timber.e(e, "FilePath: %s, %b", videoFile.absoluteFile, videoFile.exists())
            Crashlytics.setString("file_path", videoFile.absolutePath)
            Crashlytics.setBool("file_exists", videoFile.exists())
            Crashlytics.logException(e)
            Toast.makeText(this, R.string.failed_storing_video, Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        mediaPlayer.setOnPreparedListener {
            mediaPlayer.seekTo(0)
            mediaPlayer.start()
            StartAudio()
            applyTransformation()
        }
        mediaPlayer.setOnCompletionListener {
            it.seekTo(0)
            SeekMS(0.0)
            it.start()
            StartAudio()
        }
        textureView.setOnClickListener {
            mediaPlayer.seekTo(0)
            SeekMS(0.0)
        }
        mediaPlayer.prepareAsync()

        if (textureView.isAvailable) {
            mediaPlayer.setSurface(Surface(textureView.surfaceTexture))
            applyTransformation()
        } else {
            textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture?, width: Int, height: Int) {

                }

                override fun onSurfaceTextureUpdated(surface: SurfaceTexture?) {
                }

                override fun onSurfaceTextureDestroyed(surface: SurfaceTexture?): Boolean {
                    return true
                }

                override fun onSurfaceTextureAvailable(surface: SurfaceTexture?, width: Int, height: Int) {
                    mediaPlayer.setSurface(Surface(surface))
                    applyTransformation()
                }
            }
        }

        noneBtn.setOnClickListener(this)
        reverbBtn.setOnClickListener(this)
        flangerBtn.setOnClickListener(this)
        pitchBtn.setOnClickListener(this)
        echoBtn.setOnClickListener(this)
        openSettingBtn.setOnClickListener(this)
        closeSettingBtn.setOnClickListener(this)

        musicVolume.max = 40
        musicVolume.progress = 10
        musicVolume.onProgressChangeListener = object : VerticalSlider.OnSliderProgressChangeListener {
            override fun onChanged(progress: Int, max: Int) {
                SetMusicVol(progress / 10f)
            }
        }

        micVolume.max = 40
        micVolume.progress = 10
        micVolume.onProgressChangeListener = object : VerticalSlider.OnSliderProgressChangeListener {
            override fun onChanged(progress: Int, max: Int) {
                SetMicVol(progress / 10f)
            }
        }

        if (type == PROJECT_TYPE_DUBSMASH) {
            openSettingBtn.visibility = View.GONE
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
        StopAudio()
        Cleanup()
        super.onDestroy()
    }

    override fun onClick(v: View) {
        resetAllBtns()
        when (v.id) {
            R.id.noneBtn -> {
                noneBtn.setImageResource(R.drawable.ic_ef_none)
                ApplyEffect(0)
            }
            R.id.reverbBtn -> {
                reverbBtn.setImageResource(R.drawable.ic_ef_reverb)
                ApplyEffect(1)
            }
            R.id.flangerBtn -> {
                flangerBtn.setImageResource(R.drawable.ic_ef_flanger)
                ApplyEffect(2)
            }
            R.id.pitchBtn -> {
                pitchBtn.setImageResource(R.drawable.ic_ef_pitch)
                ApplyEffect(3)
            }
            R.id.echoBtn -> {
                echoBtn.setImageResource(R.drawable.ic_ef_echo)
                ApplyEffect(4)
            }
            R.id.openSettingBtn -> settingsWrapper.visibility = View.VISIBLE
            R.id.closeSettingBtn -> settingsWrapper.visibility = View.GONE

        }
    }

    private var df = 1f
    private fun applyTransformation() {
        if (0 in arrayOf(textureView.width, textureView.height, mediaPlayer.videoHeight, mediaPlayer.videoWidth)) {
            return
        }
        if (textureView.height.toFloat() / textureView.width != mediaPlayer.videoHeight.toFloat() / mediaPlayer.videoWidth) {
            val matrix = Matrix()
            df = textureView.height / mediaPlayer.videoHeight.toFloat()

            val sx = mediaPlayer.videoWidth * df / textureView.width
            val sy = mediaPlayer.videoHeight * df / textureView.height

            val cx = textureView.width / 2f
            val cy = textureView.height / 2f

            matrix.setScale(sx, sy, cx, cy)
            textureView.setTransform(matrix)
        }
    }

    private fun resetAllBtns() {
        noneBtn.setImageResource(R.drawable.ic_ef_none_disabled)
        echoBtn.setImageResource(R.drawable.ic_ef_echo_disabled)
        reverbBtn.setImageResource(R.drawable.ic_ef_reverb_disabled)
        flangerBtn.setImageResource(R.drawable.ic_ef_flanger_disabled)
        pitchBtn.setImageResource(R.drawable.ic_ef_pitch_disabled)
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
                sampleRate,
                type == PROJECT_TYPE_SINGING
        )
        audioInitialized = true
    }

    private fun applyEffects() {
        if (Effect() != 0) {
            SaveEffect(micFile.absolutePath, File(baseDir, "mic-effect-$timeStamp.wav").absolutePath)
        }
    }

    private fun doMux() {
        thread {
            applyEffects()
            val inputFiles = arrayListOf(
                    videoFile.absolutePath,
                    audioFile.absolutePath
            )

            if (type == PROJECT_TYPE_SINGING) {
                inputFiles.add(
                        if (Effect() == 0) micFile.absolutePath else File(baseDir, "mic-effect-$timeStamp.wav").absolutePath
                )
            }

            MuxerService.startJob(this, MuxJob(type, post.id, inputFiles, outFile.absolutePath))
            runOnUiThread {
                CantoDialog(this, getString(R.string.congrats), getString(R.string.your_project_is_now_being_processed), autoDismiss = true)
                        .apply {
                            setOnDismissListener { finish() }
                        }.show()
            }
        }

    }


    external fun InitAudio(bufferSize: Int, sampleRate: Int, isSinging: Boolean = false)
    external fun OpenFile(filePath: String, length: Int, micFilePath: String = "", micLength: Int = 0): Double
    external fun StartAudio()
    external fun StopAudio()
    external fun GetProgressMS(): Double
    external fun SeekMS(percent: Double)
    external fun IsPlaying(): Boolean
    external fun SaveEffect(sourcePath: String, destPath: String)
    external fun Effect(): Int
    external fun ApplyEffect(effect: Int)
    external fun onBackground()
    external fun onForeground()
    external fun Cleanup()
    external fun SetMusicVol(musicVol: Float)
    external fun SetMicVol(micVol: Float)
}
