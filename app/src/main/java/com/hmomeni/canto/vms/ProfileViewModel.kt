package com.hmomeni.canto.vms

import android.arch.lifecycle.ViewModel
import com.hmomeni.canto.di.DIComponent

class ProfileViewModel : ViewModel(), DIComponent.Injectable {
    override fun inject(diComponent: DIComponent) {
        diComponent.inject(this)
    }
}