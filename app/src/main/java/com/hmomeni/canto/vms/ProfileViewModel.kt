package com.hmomeni.canto.vms

import android.arch.lifecycle.ViewModel
import com.hmomeni.canto.di.DIComponent
import com.hmomeni.canto.persistence.ProjectDao
import javax.inject.Inject

class ProfileViewModel : ViewModel(), DIComponent.Injectable {
    override fun inject(diComponent: DIComponent) {
        diComponent.inject(this)
    }

    @Inject
    lateinit var projectDao: ProjectDao



}