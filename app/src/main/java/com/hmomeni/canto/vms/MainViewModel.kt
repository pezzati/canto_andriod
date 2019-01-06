package com.hmomeni.canto.vms

import android.arch.lifecycle.ViewModel
import com.hmomeni.canto.api.Api
import com.hmomeni.canto.di.DIComponent
import com.hmomeni.canto.entities.Post
import com.hmomeni.canto.entities.UserInventory
import com.hmomeni.canto.utils.navigation.NavEvent
import io.reactivex.Completable
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
}