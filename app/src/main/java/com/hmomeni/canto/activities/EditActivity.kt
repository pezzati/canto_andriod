package com.hmomeni.canto.activities

import android.app.ProgressDialog
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.view.WindowManager
import android.widget.SeekBar
import android.widget.Toast
import com.hmomeni.canto.App
import com.hmomeni.canto.R
import com.hmomeni.canto.entities.FullPost
import com.hmomeni.canto.entities.PROJECT_TYPE_DUBSMASH
import com.hmomeni.canto.entities.PROJECT_TYPE_SINGING
import com.hmomeni.canto.utils.ViewModelFactory
import com.hmomeni.canto.utils.app
import com.hmomeni.canto.utils.ffmpeg.FFcommandExecuteResponseHandler
import com.hmomeni.canto.utils.ffmpeg.FFmpeg
import com.hmomeni.canto.utils.getDuration
import com.hmomeni.canto.utils.views.VerticalSlider
import com.hmomeni.canto.vms.EditViewModel
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_edit.*
import timber.log.Timber
import java.io.File
import kotlin.concurrent.thread

class EditActivity : BaseFullActivity(), View.OnClickListener {

    init {
        System.loadLibrary("Edit")
    }

    private lateinit var viewModel: EditViewModel

    private val mediaPlayer = MediaPlayer()

    private lateinit var audioFile: File
    private lateinit var micFile: File
    private lateinit var videoFile: File

    private var type: Int = PROJECT_TYPE_SINGING
    private lateinit var post: FullPost

    private lateinit var baseDir: File
    private lateinit var outFile: File

    private var ratio: Int = RATIO_FULLSCREEN

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val uiOptions = window.decorView.systemUiVisibility
        val newUiOptions = uiOptions or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN
        window.decorView.systemUiVisibility = newUiOptions
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContentView(R.layout.activity_edit)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        viewModel = ViewModelProviders.of(this, ViewModelFactory(app()))[EditViewModel::class.java]

        baseDir = cacheDir
        val outDir = File(Environment.getExternalStorageDirectory(), "Canto")

        outDir.mkdirs()

        outFile = File(outDir, "Canto_%s_%d.mp4".format(
                if (type == PROJECT_TYPE_SINGING) "singing" else "dubsmash",
                System.currentTimeMillis()
        ))

        type = intent.getIntExtra(INTENT_EXTRA_TYPE, type)
        post = App.gson.fromJson(intent.getStringExtra(INTENT_EXTRA_POST), FullPost::class.java)
        ratio = intent.getIntExtra(INTENT_EXTRA_RATIO, RATIO_FULLSCREEN)


        initAudio()

        audioFile = File(baseDir, "dubsmash.wav")
        micFile = File(baseDir, "dubsmash-mic.wav")
        videoFile = File(baseDir, "dubsmash.mp4")

        val duration = getDuration(audioFile.absolutePath)

        Timber.d("FileDuration=%d", duration)

        OpenFile(audioFile.absolutePath, audioFile.length().toInt(), micFile.absolutePath, micFile.length().toInt())

        playBtn.setOnClickListener {
            if (IsPlaying()) {
                StopAudio()
                mediaPlayer.pause()
                playBtn.setImageResource(R.drawable.ic_play_circle)
            } else {
                StartAudio()
                mediaPlayer.start()
                timer()
                playBtn.setImageResource(R.drawable.ic_pause_circle)
            }
        }

        seekBar.max = duration.toInt()

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    mediaPlayer.seekTo(progress)
                    SeekMS(progress.toDouble())
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }
        })

        saveBtn.setOnClickListener {
            StopAudio()
            mediaPlayer.stop()
            doMux()
        }

        mediaPlayer.setDataSource(videoFile.absolutePath)

        mediaPlayer.setOnPreparedListener {
            applyTransformation()
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

    private var handler = Handler()
    private fun timer() {
        seekBar.progress = GetProgressMS().toInt()
        if (IsPlaying()) {
            handler.postDelayed({
                timer()
            }, 300)
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
                sampleRate,
                type == PROJECT_TYPE_SINGING
        )
        audioInitialized = true
    }

    private fun applyEffects() {
        when (Effect()) {
            0 -> {
            }
            else -> {
                SaveEffect(micFile.absolutePath, File(baseDir, "mic-effect.wav").absolutePath)
//                SaveEffect(audioFile.absolutePath, File(baseDir, "dubsmash-effect.wav").absolutePath)
            }
        }
    }

    private fun doMux() {
        val dialog = ProgressDialog.show(this, "Finalizing project", "Applying effects", true)
        thread {
            applyEffects()

            runOnUiThread {
                dialog.setMessage("Producing output")

                val ffmpeg = FFmpeg.getInstance(this)
                if (!ffmpeg.isSupported) {
                    Toast.makeText(this@EditActivity, "FFMPEG not supported!", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                    return@runOnUiThread
                }

                val cropCommands = if (mediaPlayer.videoHeight / mediaPlayer.videoWidth.toFloat() != 16 / 9f) {
                    val w = mediaPlayer.videoHeight / (16 / 9f)
                    listOf("-filter:v", "crop=$w:${mediaPlayer.videoHeight}")
                } else {
                    listOf()
                }
                val commands: MutableList<String> = mutableListOf()
                when (type) {
                    PROJECT_TYPE_DUBSMASH -> {
                        commands += listOf(
                                "-i", videoFile.absolutePath,
                                "-i", audioFile.absolutePath
                        )
                        if (ratio == RATIO_SQUARE) {
                            commands.addAll(listOf(
                                    "-filter:v", "crop=in_w:in_h-${textureView.height - textureView.width}"
                            ))
                        }
                        if (cropCommands.isNotEmpty()) {
                            commands.addAll(cropCommands)
                        }
                        commands.addAll(listOf(
                                "-codec:a", "aac",
                                "-codec:v", "libx264",
                                "-crf", "30",
                                "-preset", "ultrafast",
                                "-map", "0:v:0",
                                "-map", "1:a:0",
                                "-shortest", "-y", outFile.absolutePath
                        ))
                    }
                    PROJECT_TYPE_SINGING -> {
                        val micFile = if (Effect() == 0) micFile else File(baseDir, "mic-effect.wav")

                        commands += listOf(
                                "-i", videoFile.absolutePath,
                                "-i", audioFile.absolutePath,
                                "-i", micFile.absolutePath
                        )
                        if (ratio == RATIO_SQUARE) {
                            commands.addAll(listOf(
                                    "-filter:v", "crop=in_w:in_h-${textureView.height - textureView.width}"
                            ))
                        }
                        if (cropCommands.isNotEmpty()) {
                            commands.addAll(cropCommands)
                        }
                        commands.addAll(listOf(
                                "-filter_complex", "[1:0][2:0]  amix=inputs=2:duration=longest",
                                "-codec:a", "aac",
                                "-codec:v", "libx264",
                                "-crf", "30",
                                "-preset", "ultrafast",
                                "-map", "0:v",
                                "-map", "1:a:0",
                                "-shortest", "-y", outFile.absolutePath
                        ))
                    }
                }


                ffmpeg.execute(
                        commands.toTypedArray(),
                        object : FFcommandExecuteResponseHandler {
                            override fun onFinish() {
                                Timber.d("Mux finished")
                                dialog.dismiss()
                            }

                            override fun onSuccess(message: String?) {
                                Timber.d("Mux successful: %s", message)
                                Toast.makeText(this@EditActivity, "Muxing done", Toast.LENGTH_SHORT).show()
                                saveProject()
                            }

                            override fun onFailure(message: String?) {
                                Timber.e("Mux failed: %s", message)
                                Toast.makeText(this@EditActivity, "Muxing failed", Toast.LENGTH_SHORT).show()
                            }

                            override fun onProgress(message: String?) {
                                Timber.d("Mux progress: %s", message)
                            }

                            override fun onStart() {
                                Timber.d("Mux started")
                            }
                        }
                )
            }
        }

    }

    private var disposable: Disposable? = null

    private fun saveProject() {
        disposable = if (type == PROJECT_TYPE_SINGING) {
            viewModel.saveSinging(outFile, post, ratio)
        } else {
            viewModel.saveDubsmash(outFile, post, ratio)
        }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    Toast.makeText(this@EditActivity, R.string.saving_project_done, Toast.LENGTH_SHORT).show()
                    finish()
                }, {
                    Timber.e(it)
                    Toast.makeText(this@EditActivity, R.string.saving_project_failed, Toast.LENGTH_SHORT).show()
                })
    }

    external fun InitAudio(bufferSize: Int, sampleRate: Int, isSinging: Boolean = false)
    external fun OpenFile(filePath: String, length: Int, micFilePath: String = "", micLength: Int = 0): Double
    external fun TogglePlayback()
    external fun StartAudio()
    external fun StopAudio()
    external fun GetProgressMS(): Double
    external fun GetDurationMS(): Double
    external fun Seek(percent: Double)
    external fun SeekMS(percent: Double)
    external fun IsPlaying(): Boolean
    external fun CropSave(sourcePath: String, destPath: String, from: Long, to: Long, total: Long)
    external fun SaveEffect(sourcePath: String, destPath: String)
    external fun Effect(): Int
    external fun ApplyEffect(effect: Int)
    external fun onBackground()
    external fun onForeground()
    external fun Cleanup()
    external fun SetMusicVol(musicVol: Float)
    external fun SetMicVol(micVol: Float)
}
