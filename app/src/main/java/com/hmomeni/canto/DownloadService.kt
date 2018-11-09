package com.hmomeni.canto

import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.IBinder
import com.hmomeni.canto.utils.DownloadEvent
import com.hmomeni.canto.utils.app
import io.reactivex.processors.PublishProcessor
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import kotlin.concurrent.thread

const val ACTION_DOWNLOAD_START = 1
const val ACTION_DOWNLOAD_CANCEL = 2
const val ACTION_DOWNLOAD_FINISH = 3
const val ACTION_DOWNLOAD_PROGRESS = 4
const val ACTION_DOWNLOAD_FAILED = 5

class DownloadService : Service() {

    companion object {
        fun startDownload(context: Context, fileUrl: String): String {
            val uri = Uri.parse(fileUrl)
            context.startService(Intent(context, DownloadService::class.java)
                    .putExtra("action", ACTION_DOWNLOAD_START)
                    .putExtra("file_url", fileUrl))
            return File(context.filesDir, uri.lastPathSegment).absolutePath
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }


    var downloadUrl: String? = ""
    private var isCanceled = false
    @Inject
    lateinit var downloadEvents: PublishProcessor<DownloadEvent>

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {

        val action = intent.getIntExtra("action", ACTION_DOWNLOAD_START)

        when (action) {
            ACTION_DOWNLOAD_START -> {
                downloadUrl = intent.getStringExtra("file_url")
                thread {
                    downloadFile()
                }
            }
            ACTION_DOWNLOAD_CANCEL -> {
                isCanceled = true
            }
        }

        return START_NOT_STICKY
    }

    override fun onCreate() {
        super.onCreate()
        app().di.inject(this)
    }


    private fun downloadFile() {
        val fileName = Uri.parse(downloadUrl).lastPathSegment
        val finalFile = File(filesDir, fileName)
        if (finalFile.exists()) {
            onDownloadFinished()
            return
        }
        try {
            val url = URL(downloadUrl)
            val c = url.openConnection() as HttpURLConnection
            c.requestMethod = "GET"
            c.connect()

            if (c.responseCode != 200) throw Exception("Error in connection")

            val downloadFile = File(filesDir, "tempFile")

            val fileOutput = FileOutputStream(downloadFile)
            val inputStream = c.inputStream
            val buffer = ByteArray(1024)


            val fileLength = c.contentLength
            var downloded: Long = 0

            onDownloadStarted()

            var read = 0
            while (read != -1) {
                if (isCanceled) {
                    fileOutput.close()
                    inputStream.close()
                    downloadFile.delete()
                    c.disconnect()
                    isCanceled = false
                    onDownloadCanceled()
                    return
                }
                val percent = downloded / fileLength.toFloat() * 100
                onProgressUpdate(percent.toInt())
                downloded += read.toLong()
                fileOutput.write(buffer, 0, read)
                read = inputStream.read(buffer)
            }

            downloadFile.renameTo(finalFile)

            onDownloadFinished()

            fileOutput.close()
            inputStream.close()
            c.disconnect()
        } catch (e: IOException) {
            Timber.e(e)
            onDownloadFailed()
        }
    }

    private fun onDownloadStarted() {
        downloadEvents.onNext(DownloadEvent(ACTION_DOWNLOAD_START, 0))
    }

    private fun onDownloadCanceled() {
        downloadEvents.onNext(DownloadEvent(ACTION_DOWNLOAD_CANCEL, 0))
    }

    private fun onDownloadFailed() {
        downloadEvents.onNext(DownloadEvent(ACTION_DOWNLOAD_FAILED, 0))
    }

    private fun onDownloadFinished() {
        downloadEvents.onNext(DownloadEvent(ACTION_DOWNLOAD_FINISH, 0))
    }

    private fun onProgressUpdate(progress: Int) {
        downloadEvents.onNext(DownloadEvent(ACTION_DOWNLOAD_PROGRESS, progress))
    }

}