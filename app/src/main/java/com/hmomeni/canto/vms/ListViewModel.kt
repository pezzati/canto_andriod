package com.hmomeni.canto.vms

import android.arch.lifecycle.ViewModel
import com.hmomeni.canto.api.Api
import com.hmomeni.canto.di.DIComponent
import javax.inject.Inject

class ListViewModel : ViewModel(), DIComponent.Injectable {
    override fun inject(diComponent: DIComponent) {
        diComponent.inject(this)
    }

    lateinit var type: String
    var objectId: Int = 0

    @Inject
    lateinit var api: Api
}