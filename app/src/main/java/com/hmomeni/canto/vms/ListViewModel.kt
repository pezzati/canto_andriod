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
    var urlPath: String = ""
    var objectId: Int = 0

    @Inject
    lateinit var api: Api
    @Inject
    lateinit var navEvents: PublishProcessor<NavEvent>

    val posts: MutableList<Post> = mutableListOf()

    private var page = 0

    private var nextUrl: String? = null

    fun loadPosts(): Completable {
        return api
                .getGenrePosts(urlPath)
                .doOnSuccess {
                    nextUrl = it.next
                }
                .map { it.data }
                .doOnSuccess {
                    posts.addAll(it)
                }
                .ignoreElement()
    }

    fun loadNextPage(): Completable? {
        if (nextUrl.isNullOrEmpty()) {
            return null
        }
        return api
                .getGenrePosts(nextUrl!!)
                .doOnSuccess {
                    nextUrl = it.next
                }
                .map { it.data }
                .doOnSuccess {
                    posts.addAll(it)
                }
                .ignoreElement()
    }
}