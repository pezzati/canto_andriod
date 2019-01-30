package com.hmomeni.canto.vms

import androidx.lifecycle.ViewModel
import com.crashlytics.android.Crashlytics
import com.hmomeni.canto.api.Api
import com.hmomeni.canto.di.DIComponent
import com.hmomeni.canto.entities.User
import com.hmomeni.canto.persistence.ProjectDao
import com.hmomeni.canto.persistence.UserDao
import com.hmomeni.canto.utils.UserSession
import com.hmomeni.canto.utils.navigation.NavEvent
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.processors.PublishProcessor
import timber.log.Timber
import javax.inject.Inject

class ProfileViewModel : ViewModel(), DIComponent.Injectable {
    override fun inject(diComponent: DIComponent) {
        diComponent.inject(this)
    }

    @Inject
    lateinit var userSession: UserSession

    @Inject
    lateinit var userDao: UserDao

    @Inject
    lateinit var projectDao: ProjectDao

    @Inject
    lateinit var navEvents: PublishProcessor<NavEvent>

    @Inject
    lateinit var api: Api

    fun getUser(): Flowable<User> = Flowable.create({ e ->
        userDao.getCurrentUser().subscribe({
            userSession.user = it
            e.onNext(it)
        }, {
            Timber.e(it)
            Crashlytics.logException(it)
        })
        api.getUserInfo().subscribe({
            userSession.user = it
            userDao.insert(it)
            e.onNext(it)
        }, {
            Crashlytics.logException(it)
            e.onError(it)
        })
    }, BackpressureStrategy.BUFFER)

}