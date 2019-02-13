package com.hmomeni.canto.utils

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.crashlytics.android.Crashlytics
import com.hmomeni.canto.api.Api
import okhttp3.MediaType
import okhttp3.RequestBody
import timber.log.Timber
import javax.inject.Inject

class UserActionSyncWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    @Inject
    lateinit var api: Api
    @Inject
    lateinit var userSession: UserSession

    override fun doWork(): Result {
        applicationContext.app().di.inject(this)

        val actions = getUserActionsRaw()

        if (actions == "[]" || !userSession.isUser()) {
            Timber.d("No actions to sync or not logged in")
            return Result.success()
        }
        Timber.d("Syncing actions: $actions")
        val body = RequestBody.create(MediaType.parse("application/json"), actions)

        val result = api.syncActions(body).blockingGet()

        if (result != null) {
            Crashlytics.setString("actions", actions)
            Crashlytics.logException(result)
            Timber.e(result)
            return Result.retry()
        }

        purgeUserActions()

        return Result.success()
    }
}