package com.hmomeni.canto.services

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.ThumbnailUtils
import android.os.Build
import android.os.IBinder
import android.provider.MediaStore.Video.Thumbnails.MINI_KIND
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.crashlytics.android.Crashlytics
import com.hmomeni.canto.R
import com.hmomeni.canto.activities.MainActivity
import com.hmomeni.canto.activities.RATIO_FULLSCREEN
import com.hmomeni.canto.entities.MuxJob
import com.hmomeni.canto.entities.PROJECT_TYPE_DUBSMASH
import com.hmomeni.canto.entities.PROJECT_TYPE_SINGING
import com.hmomeni.canto.utils.*
import com.hmomeni.canto.utils.ffmpeg.FFcommandExecuteResponseHandler
import com.hmomeni.canto.utils.ffmpeg.FFmpeg
import com.hmomeni.canto.vms.EditViewModel
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import java.io.File
import java.util.*
import java.util.regex.Pattern

const val CRF_FACTOR = 24 // less is more quality
val DURATION_PATTERN: Pattern = Pattern.compile("(.*)Duration: 00:([0-9]{2}):([0-9]{2})\\.([0-9]{2}),(.*)")
val PROGRESS_PATTERN: Pattern = Pattern.compile("(.*)time=00:([0-9]{2}):([0-9]{2})\\.([0-9]{2})(.*)")

class MuxerService : Service() {
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    companion object {
        fun startJob(context: Context, job: MuxJob) {
            context.startService(Intent(context, MuxerService::class.java).putExtra("job", job))
        }
    }


    private val jobList = mutableListOf<MuxJob>()
    private var inProgress = false
    private var activeJob: MuxJob? = null
    private lateinit var viewModel: EditViewModel
    private lateinit var nManager: NotificationManager
    private lateinit var watermark: String

    override fun onCreate() {
        val dm = resources.displayMetrics
        val conf = resources.configuration
        val locale = Locale(FA_LANG.toLowerCase())
        Locale.setDefault(locale)
        conf.setLocale(locale)
        resources.updateConfiguration(conf, dm)
        super.onCreate()
        watermark = installWatermark(this)
        viewModel = EditViewModel(app())


        nManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getSystemService(NotificationManager::class.java)
        } else {
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        }
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        val job = intent?.getParcelableExtra<MuxJob>("job")
        if (job == null) {
            stopSelf()
            return START_NOT_STICKY
        }
        jobList.add(job)
        processJobs()
        return START_NOT_STICKY
    }

    private fun processJobs() {
        if (inProgress) return

        if (jobList.isEmpty()) {
            stopSelf()
            return
        }

        inProgress = true
        activeJob = jobList[0]
        jobList.removeAt(0)

        activeJob!!.let { job ->
            createNotification(job.inputFiles[0])
                    .subscribeOn(Schedulers.computation())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({ nBuilder ->
                        val ffmpeg = FFmpeg.getInstance(this)
                        if (!ffmpeg.isSupported) {
                            Toast.makeText(this, "FFMPEG not supported!", Toast.LENGTH_SHORT).show()
                            stopSelf()
                        }

                        val commands: MutableList<String> = mutableListOf()
                        when (job.type) {
                            PROJECT_TYPE_DUBSMASH -> {
                                commands += listOf(
                                        "-i", job.inputFiles[0],
                                        "-i", watermark,
                                        "-i", job.inputFiles[1]
                                )
                                commands.addAll(listOf(
                                        "-filter_complex", "[0:v]crop=in_h*0.5625:in_h [bg];[1:v]scale=-2:50 [ovrl];[bg][ovrl]overlay=x=(main_w-overlay_w-40):y=(main_h-overlay_h-40)",
                                        "-codec:a", "aac",
                                        "-codec:v", "libx264",
                                        "-crf", "$CRF_FACTOR",
                                        "-preset", "ultrafast",
                                        "-map", "0:v:0",
                                        "-map", "2:a:0",
                                        "-shortest", "-y", job.outputFile
                                ))
                            }
                            PROJECT_TYPE_SINGING -> {

                                commands += listOf(
                                        "-i", job.inputFiles[0],
                                        "-i", watermark,
                                        "-i", job.inputFiles[1],
                                        "-i", job.inputFiles[2]
                                )

                                commands.addAll(listOf(
                                        "-filter_complex", "[0:v]crop=in_h*0.5625:in_h [bg];[1:v]scale=-2:50 [ovrl];[bg][ovrl]overlay=x=(main_w-overlay_w-40):y=(main_h-overlay_h-40);[3:0]volume=2.0[mic];[2:0][mic]amix=inputs=2:duration=longest",
                                        "-codec:a", "aac",
                                        "-codec:v", "libx264",
                                        "-crf", "$CRF_FACTOR",
                                        "-preset", "ultrafast",
                                        "-map", "0:v",
                                        "-map", "2:a",
                                        "-shortest", "-y",
                                        job.outputFile
                                ))
                            }
                        }

                        var duration: Long? = null
                        ffmpeg.execute(
                                commands.toTypedArray(),
                                object : FFcommandExecuteResponseHandler {
                                    override fun onFinish() {
                                        Timber.d("Mux finished")
                                        inProgress = false
                                        job.inputFiles.forEach {
                                            Timber.d("Deleting %s, %b", it, File(it).delete())
                                        }
                                    }

                                    override fun onSuccess(message: String?) {
                                        Timber.d("Mux successful: %s", message)
                                        if (job.shouldUpload) {
                                            uploadProject(job, nBuilder)
                                        } else {
                                            saveProject(job, nBuilder)
                                        }
                                    }

                                    override fun onFailure(message: String?) {
                                        Timber.e("Mux failed: %s", message)
                                        Crashlytics.logException(Exception(message))
                                        failNotify(nBuilder)
                                    }

                                    override fun onProgress(message: String?) {
                                        Timber.d("Mux progress: %s", message)
                                        if (duration == null) {
                                            val durationMatcher = DURATION_PATTERN.matcher(message)
                                            if (durationMatcher.matches()) {
                                                duration = durationMatcher.group(2).toLong() * 60000 + durationMatcher.group(3).toLong() * 1000 + durationMatcher.group(4).toLong()
                                                nBuilder.setProgress(100, 0, false)
                                                nManager.notify(hashCode(), nBuilder.build())
                                            }
                                        }

                                        val progressMatcher = PROGRESS_PATTERN.matcher(message)
                                        if (progressMatcher.matches()) {
                                            val progress = progressMatcher.group(2).toLong() * 60000 + progressMatcher.group(3).toLong() * 1000 + progressMatcher.group(4).toLong()

                                            val percent = (progress.toFloat() / duration!!.toFloat() * 100f).toInt()

                                            nBuilder.setContentText(getString(R.string.progres_x, percent))
                                            nBuilder.setProgress(100, percent, false)
                                            nManager.notify(hashCode(), nBuilder.build())
                                        }
                                    }

                                    override fun onStart() {
                                        Timber.d("Mux started")
                                        startForeground(hashCode(), nBuilder.build())
                                    }
                                }
                        )
                    }, {
                        Timber.e(it)
                        Crashlytics.logException(it)
                    })

        }
    }

    @SuppressLint("CheckResult")
    private fun uploadProject(job: MuxJob, nBuilder: NotificationCompat.Builder) {
        viewModel.uploadSong(job.outputFile, job.postId)
                .iomain()
                .doOnSubscribe {
                    uploadNotify(nBuilder)
                }.subscribe({
                    saveProject(job, nBuilder)
                }, {
                    Timber.e(it)
                    failNotify(nBuilder)
                })
    }

    @SuppressLint("CheckResult")
    private fun saveProject(job: MuxJob, nBuilder: NotificationCompat.Builder) {
        viewModel.getPost(job.postId)
                .iomain()
                .subscribe({
                    if (job.type == PROJECT_TYPE_SINGING) {
                        viewModel.saveSinging(job.outputFile, it, RATIO_FULLSCREEN)
                    } else {
                        viewModel.saveDubsmash(job.outputFile, it, RATIO_FULLSCREEN)
                    }
                            .iomain()
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe({
                                successNotify(nBuilder)
                            }, {
                                failNotify(nBuilder)
                                Timber.e(it)
                                Crashlytics.logException(it)
                            })
                }, {
                    failNotify(nBuilder)
                    Crashlytics.logException(it)
                    Timber.e(it)
                })
    }

    private val CHANNEL_ID: String = "muxer"

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_ID, NotificationManager.IMPORTANCE_NONE)
            channel.enableVibration(false)
            channel.enableLights(false)
            channel.setSound(null, null)
            nManager.createNotificationChannel(channel)
        }
    }

    @SuppressLint("CheckResult")
    private fun createNotification(videoPath: String): Flowable<NotificationCompat.Builder> {
        return Flowable.create<Bitmap>({
            var thumbnail = ThumbnailUtils.createVideoThumbnail(videoPath, MINI_KIND)
            if (thumbnail == null) {
                thumbnail = getBitmapFromVectorDrawable(this, R.drawable.ic_error)
            }
            it.onNext(thumbnail)
            it.onComplete()
        }, BackpressureStrategy.BUFFER).map {
            val pendingIntent = PendingIntent.getActivity(this, hashCode(), Intent(this, MainActivity::class.java).apply {
                putExtra("target", "profile")
            }, PendingIntent.FLAG_UPDATE_CURRENT)

            val nBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle(getString(R.string.muxing_project))
                    .setSmallIcon(R.drawable.cantoriom)
                    .setLargeIcon(it)
            nBuilder.setContentIntent(pendingIntent)
            return@map nBuilder

        }
    }

    private fun uploadNotify(builder: NotificationCompat.Builder) {
        stopForeground(true)
        builder.setProgress(0, 0, true)
        builder.setContentText(getString(R.string.uploading))
        startForeground(hashCode(), builder.build())
    }

    private fun failNotify(builder: NotificationCompat.Builder) {
        stopForeground(true)
        builder.setProgress(0, 0, false)
        builder.setContentText(getString(R.string.muxing_failed))
        nManager.notify(hashCode(), builder.build())
    }

    private fun successNotify(builder: NotificationCompat.Builder) {
        stopForeground(true)
        builder.setProgress(0, 0, false)
        builder.setContentText(getString(R.string.muxing_done))
        nManager.notify(hashCode(), builder.build())
    }
}