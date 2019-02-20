package com.hmomeni.canto.vms

import androidx.lifecycle.ViewModel
import com.hmomeni.canto.api.Api
import com.hmomeni.canto.entities.ApiResponse
import com.hmomeni.canto.entities.Post
import com.hmomeni.canto.utils.navigation.NavEvent
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.processors.PublishProcessor
import javax.inject.Inject

class ListViewModel @Inject constructor() : ViewModel() {

    lateinit var type: String
    var urlPath: String = ""

    @Inject
    lateinit var api: Api
    @Inject
    lateinit var navEvents: PublishProcessor<NavEvent>

    val posts: MutableList<Post> = mutableListOf()

    var nextUrl: String? = null

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
                .flatMap {
                    if (it.next == nextUrl) {
                        return@flatMap Single.never<ApiResponse<List<Post>>>()
                    } else {
                        return@flatMap Single.just(it)
                    }
                }
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