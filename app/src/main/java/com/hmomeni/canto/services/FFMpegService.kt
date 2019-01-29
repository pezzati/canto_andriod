package com.hmomeni.canto.services

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.crashlytics.android.Crashlytics
import com.hmomeni.canto.R
import com.hmomeni.canto.utils.FFMPEG_URL
import com.hmomeni.canto.utils.ffmpeg.CpuArch
import com.hmomeni.canto.utils.ffmpeg.CpuArchHelper
import com.hmomeni.canto.utils.isFFMpegAvailable
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.*
import java.util.concurrent.TimeUnit

class FFMpegService : Service() {
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private lateinit var nManager: NotificationManager
    override fun onCreate() {
        super.onCreate()
        nManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getSystemService(NotificationManager::class.java)
        } else {
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        }
    }

    @SuppressLint("CheckResult")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        if (!inProgress) {
            if (isFFMpegAvailable(this)) {
                Timber.d("FFMPEG is present, exiting...")
                stopSelf()
                return START_NOT_STICKY
            }

            Timber.d("FFMPEG is not present, initiating download...")
            val builder = showNotification()
            downloadFFMpeg()
                    .subscribeOn(Schedulers.io())
                    .throttleLatest(1000, TimeUnit.MILLISECONDS)
                    .doOnSubscribe {
                        inProgress = true
                        startForeground(hashCode(), builder.build())
                    }
                    .doAfterTerminate {
                        inProgress = false
                        stopForeground(true)
                        stopSelf()
                    }
                    .subscribe({
                        Timber.d("Downloading FFMPEG, progress=%d", it)
                        builder.setProgress(100, it, false)
                        builder.setContentText("%d%%".format(Locale.ENGLISH, it))
                        nManager.notify(hashCode(), builder.build())
                    }, {
                        Crashlytics.logException(it)
                        Timber.e(it)
                    })
        }
        return START_STICKY
    }

    private fun showNotification(): NotificationCompat.Builder {

        val nManager: NotificationManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getSystemService(NotificationManager::class.java)
        } else {
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        }
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel("ffmpeg", "ffmpeg", NotificationManager.IMPORTANCE_DEFAULT)
            nManager.createNotificationChannel(channel)
            NotificationCompat.Builder(this, channel.id)
        } else {
            NotificationCompat.Builder(this)
        }

        builder.setContentTitle(getString(R.string.preparing_assets))
        builder.setContentText(getString(R.string.downloading))
        builder.setSmallIcon(R.drawable.cantoriom)

        builder.setVibrate(longArrayOf(0, 0))
        builder.setSound(null)

        return builder
    }

    private var inProgress = false

    private fun downloadFFMpeg(): Flowable<Int> {
        return Flowable.create({ e ->
            val finalFile = File(filesDir, "ffmpeg")
            val cpuArch = if (CpuArchHelper.getCpuArch() == CpuArch.x86) "x86" else "arm"
            val downloadUrl = FFMPEG_URL.replace("{arch}", cpuArch)
            try {
                val url = URL(downloadUrl)
                val c = url.openConnection() as HttpURLConnection
                c.requestMethod = "GET"
                c.connect()

                if (c.responseCode != 200) throw Exception("Error in connection")

                val downloadFile = File(filesDir, "fftemp")

                val fileOutput = FileOutputStream(downloadFile)
                val inputStream = c.inputStream
                val buffer = ByteArray(1024)


                val fileLength = c.contentLength
                var downloded: Long = 0

                var read = 0
                while (read != -1) {
                    if (e.isCancelled) {
                        fileOutput.close()
                        inputStream.close()
                        downloadFile.delete()
                        c.disconnect()
                        return@create
                    }
                    val percent = downloded / fileLength.toFloat() * 100
                    e.onNext(percent.toInt())
                    downloded += read.toLong()
                    fileOutput.write(buffer, 0, read)
                    read = inputStream.read(buffer)
                }

                downloadFile.renameTo(finalFile)

                finalFile.setExecutable(true, false)
                finalFile.setReadable(true, false)
                finalFile.setWritable(true, false)

                fileOutput.close()
                inputStream.close()
                c.disconnect()
                e.onComplete()
            } catch (ex: IOException) {
                Timber.e(ex)
                e.onError(ex)
            }
        }, BackpressureStrategy.BUFFER)
    }
}