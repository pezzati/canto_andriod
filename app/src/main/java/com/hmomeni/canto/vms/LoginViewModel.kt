package com.hmomeni.canto.vms

import android.arch.lifecycle.ViewModel
import com.hmomeni.canto.App
import com.hmomeni.canto.BuildConfig
import com.hmomeni.canto.api.Api
import com.hmomeni.canto.di.DIComponent
import com.hmomeni.canto.entities.User
import com.hmomeni.canto.persistence.UserDao
import com.hmomeni.canto.utils.*
import com.hmomeni.canto.utils.ffmpeg.CpuArch
import com.hmomeni.canto.utils.ffmpeg.CpuArchHelper
import io.reactivex.BackpressureStrategy
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Single
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject

class LoginViewModel : ViewModel(), DIComponent.Injectable {
    override fun inject(diComponent: DIComponent) {
        diComponent.inject(this)
    }

    @Inject
    lateinit var app: App
    @Inject
    lateinit var api: Api
    @Inject
    lateinit var userDao: UserDao
    @Inject
    lateinit var userSession: UserSession

    lateinit var login: String

    lateinit var signupMode: SignupMode

    fun handshake(): Single<Pair<Int, String?>> {
        val map = mutableMapOf<String, Any>()
        map["build_version"] = BuildConfig.VERSION_CODE
        map["device_type"] = "android"
        map["udid"] = getDeviceId(app)
        map["one_signal_id"] = ""
        map["bundle"] = BuildConfig.APPLICATION_ID
        return api.handshake(map.toBody()).map {
            return@map when {
                it["force_update"].asBoolean -> Pair(1, it["url"].asString)
                it["suggest_update"].asBoolean -> Pair(2, it["url"].asString)
                it["token"].asString.startsWith("guest") -> Pair(3, null)
                else -> Pair(0, null)
            }
        }
    }

    fun signUp(login: String): Completable {
        this.login = login
        val map = mutableMapOf<String, Any>()
        if (signupMode == SignupMode.EMAIL) {
            map["email"] = login
        } else {
            map["mobile"] = login
        }
        return api.signUp(map.toBody())
    }

    fun verify(code: String): Completable {
        val map = makeMap().apply {
            if (signupMode == SignupMode.EMAIL) {
                add("email", login)
            } else {
                add("mobile", login)
            }
            add("code", code)
        }

        return api.verify(map.body())
                .doOnSuccess {
                    val token = it["token"].asString
                    val user = User(
                            0, login, "", "", token, true
                    )
                    userDao.insert(user)
                    userSession.user = user
                }
                .ignoreElement()
    }

    fun isFFMpegAvailable(): Boolean {
        val ffmpeg = File(app.filesDir, "ffmpeg")
        return ffmpeg.exists()
    }

    fun downloadFFMpeg(): Flowable<Int> {
        return Flowable.create({ e ->
            val finalFile = File(app.filesDir, "ffmpeg")
            val cpuArch = if (CpuArchHelper.getCpuArch() == CpuArch.x86) "x86" else "arm"
            val downloadUrl = FFMPEG_URL.replace("{arch}", cpuArch)
            try {
                val url = URL(downloadUrl)
                val c = url.openConnection() as HttpURLConnection
                c.requestMethod = "GET"
                c.connect()

                if (c.responseCode != 200) throw Exception("Error in connection")

                val downloadFile = File(app.filesDir, "fftemp")

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

    enum class SignupMode {
        EMAIL, PHONE
    }
}