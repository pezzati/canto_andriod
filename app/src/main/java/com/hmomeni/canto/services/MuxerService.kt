package com.hmomeni.canto.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.os.IBinder
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationManagerCompat
import android.widget.Toast
import com.hmomeni.canto.R
import com.hmomeni.canto.activities.RATIO_FULLSCREEN
import com.hmomeni.canto.entities.MuxJob
import com.hmomeni.canto.entities.PROJECT_TYPE_DUBSMASH
import com.hmomeni.canto.entities.PROJECT_TYPE_SINGING
import com.hmomeni.canto.utils.app
import com.hmomeni.canto.utils.ffmpeg.FFcommandExecuteResponseHandler
import com.hmomeni.canto.utils.ffmpeg.FFmpeg
import com.hmomeni.canto.utils.iomain
import com.hmomeni.canto.vms.EditViewModel
import io.reactivex.android.schedulers.AndroidSchedulers
import timber.log.Timber

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
    private lateinit var mNotificationManager: NotificationManagerCompat

    override fun onCreate() {
        super.onCreate()
        viewModel = EditViewModel(app())
        mNotificationManager = NotificationManagerCompat.from(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {

        val job = intent.getParcelableExtra<MuxJob>("job")
        jobList.add(job)
        processJobs()
        return START_STICKY
    }

    private fun processJobs() {
        if (inProgress) return

        if (jobList.isEmpty()) {
            stopSelf()
            return
        }
        createNotification(false)
        inProgress = true
        activeJob = jobList[0]
        jobList.removeAt(0)

        activeJob!!.let { job ->
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
                            "-i", job.inputFiles[1]
                    )
                    commands.addAll(listOf(
                            "-codec:a", "aac",
                            "-codec:v", "libx264",
                            "-crf", "30",
                            "-preset", "ultrafast",
                            "-map", "0:v:0",
                            "-map", "1:a:0",
                            "-shortest", "-y", job.outputFile
                    ))
                }
                PROJECT_TYPE_SINGING -> {

                    commands += listOf(
                            "-i", job.inputFiles[0],
                            "-i", job.inputFiles[1],
                            "-i", job.inputFiles[2]
                    )

                    commands.addAll(listOf(
                            "-filter_complex", "[1:0][2:0]  amix=inputs=2:duration=longest",
                            "-codec:a", "aac",
                            "-codec:v", "libx264",
                            "-crf", "30",
                            "-preset", "ultrafast",
                            "-map", "0:v",
                            "-map", "1:a:0",
                            "-shortest", "-y", job.outputFile
                    ))
                }
            }


            ffmpeg.execute(
                    commands.toTypedArray(),
                    object : FFcommandExecuteResponseHandler {
                        override fun onFinish() {
                            Timber.d("Mux finished")
                        }

                        override fun onSuccess(message: String?) {
                            Timber.d("Mux successful: %s", message)
                            saveProject(job)
                        }

                        override fun onFailure(message: String?) {
                            Timber.e("Mux failed: %s", message)
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

    private fun saveProject(job: MuxJob) {
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
                                createNotification(true)
                            }, {
                                Timber.e(it)
                            })
                }, {
                    Timber.e(it)
                })
    }

    private val CHANNEL_ID: String = "muxer"

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Timber"
            val importance = NotificationManager.IMPORTANCE_LOW
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val mChannel = NotificationChannel(CHANNEL_ID, name, importance)
            manager.createNotificationChannel(mChannel)
        }
    }

    private fun createNotification(finish: Boolean) {
        val nBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(getString(R.string.canto))
                .setContentText(getString(R.string.muxing_project))
                .setSmallIcon(R.drawable.cantoriom)
                .setLargeIcon(BitmapFactory.decodeResource(resources, R.drawable.ic_canto_logo))

        if (finish) {
            nBuilder.setContentText(getString(R.string.muxing_done))
        }
        if (finish) {
            mNotificationManager.notify(hashCode(), nBuilder.build())
        } else {
            startForeground(hashCode(), nBuilder.build())
        }

    }
}