package com.hmomeni.canto.vms

import android.arch.lifecycle.ViewModel
import com.hmomeni.canto.api.Api
import com.hmomeni.canto.di.DIComponent
import com.hmomeni.canto.utils.navigation.NavEvent
import io.reactivex.processors.PublishProcessor
import javax.inject.Inject

class SearchViewModel : ViewModel(), DIComponent.Injectable {
    override fun inject(diComponent: DIComponent) {
        diComponent.inject(this)
    }

    @Inject
    lateinit var api: Api
    @Inject
    lateinit var navEvents: PublishProcessor<NavEvent>

}