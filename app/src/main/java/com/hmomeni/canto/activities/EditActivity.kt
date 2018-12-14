package com.hmomeni.canto.activities

import android.app.ProgressDialog
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.support.v7.app.AppCompatActivity
import android.view.SurfaceHolder
import android.view.View
import android.widget.SeekBar
import android.widget.Toast
import com.hmomeni.canto.R
import com.hmomeni.canto.entities.FullPost
import com.hmomeni.canto.entities.PROJECT_TYPE_DUBSMASH
import com.hmomeni.canto.entities.PROJECT_TYPE_SINGING
import com.hmomeni.canto.utils.ViewModelFactory
import com.hmomeni.canto.utils.app
import com.hmomeni.canto.utils.getBitmapFromVectorDrawable
import com.hmomeni.canto.utils.getDuration
import com.hmomeni.canto.utils.views.VerticalSlider
import com.hmomeni.canto.vms.EditViewModel
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_edit.*
import nl.bravobit.ffmpeg.FFcommandExecuteResponseHandler
import nl.bravobit.ffmpeg.FFmpeg
import timber.log.Timber
import java.io.File
import kotlin.concurrent.thread

class EditActivity : AppCompatActivity(), View.OnClickListener {

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

    private var ratio: Int = RATIO_FULLSCREEN

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit)

        viewModel = ViewModelProviders.of(this, ViewModelFactory(app()))[EditViewModel::class.java]

        baseDir = Environment.getExternalStorageDirectory()

        type = intent.getIntExtra("type", type)
        post = intent.getParcelableExtra("post")
        ratio = intent.getIntExtra("ratio", RATIO_FULLSCREEN)


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
        mediaPlayer.prepare()
        surfaceView.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceChanged(holder: SurfaceHolder, format: Int, w: Int, h: Int) {
                if (ratio == RATIO_SQUARE) {
                    cropTop.visibility = View.VISIBLE
                    cropBottom.visibility = View.VISIBLE
                    cropTop.layoutParams = cropTop.layoutParams.apply {
                        height = h / 2 - w / 2
                    }
                    cropBottom.layoutParams = cropBottom.layoutParams.apply {
                        height = h / 2 - w / 2
                    }
                } else {
                    cropTop.visibility = View.GONE
                    cropBottom.visibility = View.GONE
                }
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
            }

            override fun surfaceCreated(holder: SurfaceHolder) {
                Timber.d("Surface Ready!")
                mediaPlayer.setSurface(holder.surface)
            }
        })

        noneBtn.setOnClickListener(this)
        reverbBtn.setOnClickListener(this)
        flangerBtn.setOnClickListener(this)
        pitchBtn.setOnClickListener(this)
        echoBtn.setOnClickListener(this)
        openSettingBtn.setOnClickListener(this)
        closeSettingBtn.setOnClickListener(this)

        micVolume.lowIcon = getBitmapFromVectorDrawable(this, R.drawable.ic_mic_low)
        micVolume.midIcon = getBitmapFromVectorDrawable(this, R.drawable.ic_mic_mid)
        micVolume.hiIcon = getBitmapFromVectorDrawable(this, R.drawable.ic_mic_mid)

        musicVolume.lowIcon = getBitmapFromVectorDrawable(this, R.drawable.ic_music_volume_low)
        musicVolume.midIcon = getBitmapFromVectorDrawable(this, R.drawable.ic_music_volume_mid)
        musicVolume.hiIcon = getBitmapFromVectorDrawable(this, R.drawable.ic_music_volume_hi)

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
                SaveEffect(audioFile.absolutePath, File(baseDir, "dubsmash-effect.wav").absolutePath)
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
                val commands: MutableList<String> = mutableListOf()
                when (type) {
                    PROJECT_TYPE_DUBSMASH -> {
                        commands += listOf(
                                "-i", videoFile.absolutePath,
                                "-i", audioFile.absolutePath
                        )
                        if (ratio == RATIO_SQUARE) {
                            commands.addAll(listOf(
                                    "-filter:v", "crop=in_w:in_h-${surfaceView.height - surfaceView.width}"
                            ))
                        }
                        commands.addAll(listOf(
                                "-codec:a", "aac",
                                "-codec:v", "libx264",
                                "-crf", "30",
                                "-preset", "ultrafast",
                                "-map", "0:v:0",
                                "-map", "1:a:0",
                                "-shortest", "-y", File(baseDir, "out.mp4").absolutePath
                        ))
                    }
                    PROJECT_TYPE_SINGING -> if (Effect() == 0) {
                        commands += listOf(
                                "-i", videoFile.absolutePath,
                                "-i", audioFile.absolutePath,
                                "-i", micFile.absolutePath
                        )
                        if (ratio == RATIO_SQUARE) {
                            commands.addAll(listOf(
                                    "-filter:v", "crop=in_w:in_h-${surfaceView.height - surfaceView.width}"
                            ))
                        }
                        commands.addAll(listOf(
                                "-filter_complex", "[1:0][2:0]  amix=inputs=2:duration=longest",
                                "-codec:a", "aac",
                                "-codec:v", "libx264",
                                "-crf", "30",
                                "-preset", "ultrafast",
                                "-map", "0:v",
                                "-map", "1:a:0",
                                "-map", "2:a:0",
                                "-shortest", "-y", File(baseDir, "out.mp4").absolutePath
                        ))
                    } else {

                        commands += listOf(
                                "-i", videoFile.absolutePath,
                                "-i", File(baseDir, "dubsmash-effect.wav").absolutePath,
                                "-i", File(baseDir, "mic-effect.wav").absolutePath
                        )
                        if (ratio == RATIO_SQUARE) {
                            commands.addAll(listOf(
                                    "-filter:v", "crop=in_w:in_h-${surfaceView.height - surfaceView.width}"
                            ))
                        }
                        commands.addAll(listOf(
                                "-filter_complex", "[1:0][2:0]  amix=inputs=2:duration=longest",
                                "-codec:a", "aac",
                                "-codec:v", "libx264",
                                "-crf", "30",
                                "-preset", "ultrafast",
                                "-map", "0:v",
                                "-map", "1:a:0",
                                "-map", "2:a:0",
                                "-shortest", "-y", File(baseDir, "out.mp4").absolutePath
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
            viewModel.saveSinging(File(baseDir, "out.mp4"), post, ratio)
        } else {
            viewModel.saveDubsmash(File(baseDir, "out.mp4"), post, ratio)
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
