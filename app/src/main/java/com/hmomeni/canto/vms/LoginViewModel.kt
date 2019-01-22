package com.hmomeni.canto.vms

import androidx.lifecycle.ViewModel
import com.hmomeni.canto.App
import com.hmomeni.canto.BuildConfig
import com.hmomeni.canto.api.Api
import com.hmomeni.canto.di.DIComponent
import com.hmomeni.canto.persistence.UserDao
import com.hmomeni.canto.utils.UserSession
import com.hmomeni.canto.utils.getDeviceId
import com.hmomeni.canto.utils.makeMap
import com.hmomeni.canto.utils.toBody
import com.pixplicity.easyprefs.library.Prefs
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

    fun verify(code: String): Single<Boolean> {
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
                    userSession.token = token
                    Prefs.putString("token", token)
                }
                .flatMap {
                    Single.just(it["new_user"].asBoolean)
                }
    }

    fun googleSignIn(idToken: String): Completable {
        val map = makeMap().apply {
            add("token", idToken)
        }

        return api.googleSignIn(map.body())
                .doOnSuccess {
                    val token = it["token"].asString
                    userSession.token = token
                    Prefs.putString("token", token)
                }
                .ignoreElement()
    }

    enum class SignupMode {
        EMAIL, PHONE
    }
}