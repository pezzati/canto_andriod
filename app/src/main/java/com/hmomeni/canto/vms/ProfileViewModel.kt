package com.hmomeni.canto.vms

import androidx.lifecycle.ViewModel
import androidx.room.EmptyResultSetException
import com.crashlytics.android.Crashlytics
import com.hmomeni.canto.api.Api
import com.hmomeni.canto.di.DIComponent
import com.hmomeni.canto.entities.User
import com.hmomeni.canto.persistence.ProjectDao
import com.hmomeni.canto.persistence.UserDao
import com.hmomeni.canto.utils.UserSession
import com.hmomeni.canto.utils.navigation.NavEvent
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.processors.PublishProcessor
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

    fun getUser(): Flowable<User> = userDao
            .getCurrentUser()
            .doOnError {
                if (it !is EmptyResultSetException) {
                    Crashlytics.logException(it)
                }
            }
            .onErrorResumeNext {
                Single.just(User(-1, "", "", "", 0, 0, null))
            }
            .mergeWith(
                    api.getUserInfo()
                            .doOnSuccess {
                                userSession.user = it
                                userDao.insert(it)
                            }
            )

}