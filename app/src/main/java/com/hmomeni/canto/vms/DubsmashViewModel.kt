package com.hmomeni.canto.vms

import android.arch.lifecycle.ViewModel
import com.hmomeni.canto.di.DIComponent
import com.hmomeni.canto.entities.FullPost
import com.hmomeni.canto.utils.DownloadEvent
import io.reactivex.processors.PublishProcessor
import javax.inject.Inject

class DubsmashViewModel : ViewModel(), DIComponent.Injectable {
    override fun inject(diComponent: DIComponent) {
        diComponent.inject(this)
    }

    lateinit var post: FullPost

    @Inject
    lateinit var downloadEvents: PublishProcessor<DownloadEvent>

    fun dEvents() = downloadEvents.onBackpressureDrop()
            .doOnNext {

            }
}