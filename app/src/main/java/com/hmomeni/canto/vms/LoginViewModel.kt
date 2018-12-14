package com.hmomeni.canto.vms

import android.arch.lifecycle.ViewModel
import com.hmomeni.canto.App
import com.hmomeni.canto.BuildConfig
import com.hmomeni.canto.api.Api
import com.hmomeni.canto.di.DIComponent
import com.hmomeni.canto.entities.User
import com.hmomeni.canto.persistence.UserDao
import com.hmomeni.canto.utils.UserSession
import com.hmomeni.canto.utils.getDeviceId
import com.hmomeni.canto.utils.makeMap
import com.hmomeni.canto.utils.toBody
import io.reactivex.Completable
import io.reactivex.Single
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

    lateinit var phone: String

    fun handshake(): Single<Int> {
        val map = mutableMapOf<String, Any>()
        map["build_version"] = BuildConfig.VERSION_CODE
        map["device_type"] = "android"
        map["udid"] = getDeviceId(app)
        map["one_signal_id"] = ""
        map["bundle"] = BuildConfig.APPLICATION_ID
        return api.handshake(map.toBody()).map {
            return@map when {
                it["force_update"].asBoolean -> 1
                it["token"].asString.startsWith("guest") -> 2
                else -> 0
            }
        }
    }

    fun signUpPhone(phone: String): Completable {
        this.phone = phone
        val map = mutableMapOf<String, Any>()
        map["mobile"] = phone
        return api.signUp(map.toBody())
    }

    fun verifyPhone(code: String): Completable {
        val map = makeMap()
                .add("mobile", phone)
                .add("code", code)
        return api.verify(map.body())
                .doOnSuccess {
                    val token = it["token"].asString
                    val user = User(
                            0, phone, "", "", token, true
                    )
                    userDao.insert(user)
                    userSession.user = user
                }
                .ignoreElement()
    }
}