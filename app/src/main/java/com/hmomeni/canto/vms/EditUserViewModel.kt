package com.hmomeni.canto.vms

import androidx.lifecycle.ViewModel
import com.hmomeni.canto.api.Api
import com.hmomeni.canto.di.DIComponent
import com.hmomeni.canto.entities.Avatar
import javax.inject.Inject

class EditUserViewModel : ViewModel(), DIComponent.Injectable {
    override fun inject(diComponent: DIComponent) {
        diComponent.inject(this)
    }

    @Inject
    lateinit var api: Api

    val avatars = mutableListOf<Avatar>()
    private var avatarPage = 1
    fun getAvatars() = api.getAvatarList(avatarPage++)
            .map {
                it.data
            }.doOnSuccess {
                avatars.addAll(it)
            }
            .ignoreElement()

}