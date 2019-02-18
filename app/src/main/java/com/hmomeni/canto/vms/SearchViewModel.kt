package com.hmomeni.canto.vms

import androidx.lifecycle.ViewModel
import com.hmomeni.canto.api.Api
import com.hmomeni.canto.entities.Post
import com.hmomeni.canto.utils.ListAction
import com.hmomeni.canto.utils.navigation.NavEvent
import io.reactivex.Completable
import io.reactivex.processors.PublishProcessor
import io.reactivex.subjects.PublishSubject
import javax.inject.Inject

class SearchViewModel @Inject constructor() : ViewModel() {

    @Inject
    lateinit var api: Api
    @Inject
    lateinit var navEvents: PublishProcessor<NavEvent>

    val result: MutableList<Post> = mutableListOf()

    val postsPublisher: PublishSubject<Pair<ListAction, Int>> = PublishSubject.create()

    fun search(query: String): Completable {
        return api.searchInGenres(query)
                .doOnSuccess {
                    result.clear()
                    result.addAll(it.data)
                    postsPublisher.onNext(Pair(ListAction.RESET, -1))
                }.ignoreElement()
    }

}