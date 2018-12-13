package com.hmomeni.canto.vms

import android.arch.lifecycle.ViewModel
import com.hmomeni.canto.api.Api
import com.hmomeni.canto.di.DIComponent
import com.hmomeni.canto.entities.User
import com.hmomeni.canto.persistence.UserDao
import com.hmomeni.canto.utils.UserSession
import com.hmomeni.canto.utils.makeMap
import com.hmomeni.canto.utils.toBody
import io.reactivex.Completable
import javax.inject.Inject

class LoginViewModel : ViewModel(), DIComponent.Injectable {
    override fun inject(diComponent: DIComponent) {
        diComponent.inject(this)
    }

    @Inject
    lateinit var api: Api
    @Inject
    lateinit var userDao: UserDao
    @Inject
    lateinit var userSession: UserSession

    lateinit var phone: String

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