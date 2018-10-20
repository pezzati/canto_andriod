package com.hmomeni.canto.vms

import android.arch.lifecycle.ViewModel
import com.hmomeni.canto.api.Api
import com.hmomeni.canto.di.DIComponent
import com.hmomeni.canto.utils.makeMap
import com.hmomeni.canto.utils.toBody
import com.pixplicity.easyprefs.library.Prefs
import io.reactivex.Completable
import javax.inject.Inject

class LoginViewModel : ViewModel(), DIComponent.Injectable {
    override fun inject(diComponent: DIComponent) {
        diComponent.inject(this)
    }

    @Inject
    lateinit var api: Api

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
                    val newUser = it["new_user"].asBoolean
                    Prefs.putString("token", token)
                }
                .ignoreElement()
    }
}