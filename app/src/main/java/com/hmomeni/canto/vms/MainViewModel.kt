package com.hmomeni.canto.vms

import androidx.lifecycle.ViewModel
import com.hmomeni.canto.App
import com.hmomeni.canto.BuildConfig
import com.hmomeni.canto.api.Api
import com.hmomeni.canto.di.DIComponent
import com.hmomeni.canto.entities.Post
import com.hmomeni.canto.entities.User
import com.hmomeni.canto.entities.UserInventory
import com.hmomeni.canto.persistence.UserDao
import com.hmomeni.canto.utils.*
import com.hmomeni.canto.utils.navigation.NavEvent
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.processors.PublishProcessor
import javax.inject.Inject

class MainViewModel : ViewModel(), DIComponent.Injectable {
    override fun inject(diComponent: DIComponent) {
        diComponent.inject(this)
    }

    @Inject
    lateinit var api: Api
    @Inject
    lateinit var navEvents: PublishProcessor<NavEvent>
    @Inject
    lateinit var userInventory: UserInventory
    @Inject
    lateinit var userSession: UserSession

    @Inject
    lateinit var userDao: UserDao

    fun sing(post: Post): Completable {
        return api.sing(post.id)
                .doOnSuccess {
                    userInventory.update(it)
                }
                .ignoreElement()
    }

    fun purchaseSong(post: Post): Completable {
        return api.purchaseSong(post.id)
                .doOnSuccess {
                    userInventory.update(it)
                }
                .ignoreElement()
    }

    fun getUser(): Flowable<User> = userDao
            .getCurrentUser()
            .mergeWith(
                    api.getUserInfo()
                            .doOnSuccess {
                                userSession.user = it
                                userDao.insert(it)
                            }
            )

    fun handshake(app: App): Single<Pair<Int, String?>> {
        val map = mutableMapOf<String, Any>()
        map["build_version"] = BuildConfig.VERSION_CODE
        map["device_type"] = "android"
        map["udid"] = getDeviceId(app)
        map["one_signal_id"] = ""
        map["bundle"] = BuildConfig.APPLICATION_ID
        return api.handshake(map.toBody()).map {
            return@map when {
                it["force_update"].asBoolean -> Pair(HANDSHAKE_FORCE_UPDATE, it["url"].asString)
                it["suggest_update"].asBoolean -> Pair(HANDSHAKE_SUGGEST_UPDATE, it["url"].asString)
                it["token"].asString.startsWith("guest") -> Pair(HANDSHAKE_GUEST, null)
                else -> Pair(HANDSHAKE_OK, null)
            }
        }
    }
}