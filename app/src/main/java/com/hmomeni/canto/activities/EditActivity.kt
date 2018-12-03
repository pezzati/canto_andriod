package com.hmomeni.canto.activities

import android.app.ProgressDialog
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
import com.hmomeni.canto.entities.PROJECT_TYPE_DUBSMASH
import com.hmomeni.canto.entities.PROJECT_TYPE_SINGING
import com.hmomeni.canto.utils.getDuration
import kotlinx.android.synthetic.main.activity_edit.*
import nl.bravobit.ffmpeg.FFcommandExecuteResponseHandler
import nl.bravobit.ffmpeg.FFmpeg
import timber.log.Timber
import java.io.File

class EditActivity : AppCompatActivity(), View.OnClickListener {

    init {
        System.loadLibrary("Edit")
    }

    private val mediaPlayer = MediaPlayer()

    private lateinit var audioFile: File
    private lateinit var micFile: File
    private lateinit var videoFile: File

    var type: Int = PROJECT_TYPE_DUBSMASH

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit)

        type = intent.getIntExtra("type", type)

        initAudio()

        audioFile = File(filesDir, "dubsmash.wav")
        micFile = File(filesDir, "dubsmash-mic.wav")
        videoFile = File(filesDir, "dubsmash.mp4")

        val duration = getDuration(audioFile.absolutePath)

        Timber.d("FileDuration=%d", duration)

        OpenFile(audioFile.absolutePath, audioFile.length().toInt(), micFile.absolutePath, micFile.length().toInt())

        playBtn.setOnClickListener {
            StartAudio()
            mediaPlayer.start()
            timer()
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
            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {

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

    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.noneBtn -> ApplyEffect(0)
            R.id.reverbBtn -> ApplyEffect(1)
            R.id.flangerBtn -> ApplyEffect(2)
            R.id.pitchBtn -> ApplyEffect(3)
        }
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

    private fun doMux() {
        val dialog = ProgressDialog.show(this, "Finalizing project", "Applying effects", true)

        when (Effect()) {
            0 -> {
            }
            else -> {
                SaveEffect(micFile.absolutePath, File(filesDir, "mic-effect.wav").absolutePath)
            }
        }

        dialog.setMessage("Producing output")

        val ffmpeg = FFmpeg.getInstance(this)
        if (!ffmpeg.isSupported) {
            Toast.makeText(this@EditActivity, "FFMPEG not supported!", Toast.LENGTH_SHORT).show()
            return
        }

        val commands = when (type) {
            PROJECT_TYPE_DUBSMASH -> arrayOf(
                    "-i", videoFile.absolutePath,
                    "-i", audioFile.absolutePath,
                    "-codec:a", "aac",
                    "-codec:v", "libx264",
                    "-crf", "30",
                    "-preset", "ultrafast",
                    "-map", "0:v:0",
                    "-map", "1:a:0",
                    "-shortest", "-y", File(Environment.getExternalStorageDirectory(), "out.mp4").absolutePath
            )
            PROJECT_TYPE_SINGING -> if (Effect() == 0) {
                arrayOf(
                        "-i", videoFile.absolutePath,
                        "-i", audioFile.absolutePath,
                        "-i", micFile.absolutePath,
                        "-filter_complex", "[1:0][2:0]  amix=inputs=2:duration=longest",
                        "-codec:a", "aac",
                        "-codec:v", "libx264",
                        "-crf", "30",
                        "-preset", "ultrafast",
                        "-map", "0:v",
                        "-map", "1:a:0",
                        "-map", "2:a:0",
                        "-shortest", "-y", File(Environment.getExternalStorageDirectory(), "out.mp4").absolutePath
                )
            } else {
                arrayOf(
                        "-i", videoFile.absolutePath,
                        "-i", audioFile.absolutePath,
                        "-i", File(filesDir, "mic-effect.wav").absolutePath,
                        "-filter_complex", "[1:0][2:0]  amix=inputs=2:duration=longest",
                        "-codec:a", "aac",
                        "-codec:v", "libx264",
                        "-crf", "30",
                        "-preset", "ultrafast",
                        "-map", "0:v",
                        "-map", "1:a:0",
                        "-map", "2:a:0",
                        "-shortest", "-y", File(Environment.getExternalStorageDirectory(), "out.mp4").absolutePath
                )
            }
            else -> null
        }


        ffmpeg.execute(
                commands,
                object : FFcommandExecuteResponseHandler {
                    override fun onFinish() {
                        Timber.d("Mux finished")
                        dialog.dismiss()
                    }

                    override fun onSuccess(message: String?) {
                        Timber.d("Mux successful: %s", message)
                        Toast.makeText(this@EditActivity, "Muxing done", Toast.LENGTH_SHORT).show()
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
}
