package com.hmomeni.canto.vms

import android.arch.lifecycle.ViewModel
import com.hmomeni.canto.api.Api
import com.hmomeni.canto.di.DIComponent
import com.hmomeni.canto.entities.Post
import com.hmomeni.canto.utils.navigation.NavEvent
import io.reactivex.Completable
import io.reactivex.processors.PublishProcessor
import javax.inject.Inject

class ListViewModel : ViewModel(), DIComponent.Injectable {
    override fun inject(diComponent: DIComponent) {
        diComponent.inject(this)
    }

    lateinit var type: String
    var objectId: Int = 0

    @Inject
    lateinit var api: Api
    @Inject
    lateinit var navEvents: PublishProcessor<NavEvent>

    val posts: MutableList<Post> = mutableListOf()

    private var page = 0

    fun loadPosts(): Completable {
        return api
                .getGenrePosts(objectId)
                .map { it.data }
                .doOnSuccess {
                    posts.addAll(it)
                }
                .ignoreElement()
    }
}