package com.hmomeni.canto.vms

import androidx.lifecycle.ViewModel
import com.hmomeni.canto.api.Api
import com.hmomeni.canto.di.DIComponent
import com.hmomeni.canto.entities.Avatar
import com.hmomeni.canto.persistence.UserDao
import com.hmomeni.canto.utils.UserSession
import com.hmomeni.canto.utils.makeMap
import io.reactivex.Completable
import io.reactivex.Single
import javax.inject.Inject

class EditUserViewModel : ViewModel(), DIComponent.Injectable {
    override fun inject(diComponent: DIComponent) {
        diComponent.inject(this)
    }

    @Inject
    lateinit var api: Api
    @Inject
    lateinit var userDao: UserDao
    @Inject
    lateinit var userSession: UserSession

    val avatars = mutableListOf<Avatar>()
    private var avatarPage = 1
    fun getAvatars(): Single<List<Avatar>> = api.getAvatarList(avatarPage++)
            .map {
                it.data
            }.doOnSuccess {
                avatars.addAll(it)
            }

    fun updateUser(avatarId: Int, username: String): Completable {
        val map = makeMap().add("username", username)
                .add("avatar", avatarId)
        return api.updateUserInfo(map.body())
                .doOnSuccess {
                    userDao.updateUser(it)
                }.ignoreElement()
    }

}