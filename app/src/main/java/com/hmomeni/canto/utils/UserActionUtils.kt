package com.hmomeni.canto.utils

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.gson.reflect.TypeToken
import com.hmomeni.canto.App
import com.hmomeni.canto.entities.UserAction
import com.pixplicity.easyprefs.library.Prefs

const val USER_ACTION_PREF = "user_actions"

fun addUserAction(action: UserAction) {
    val userActions = getUserActions()

    val newActions = userActions.toMutableList().apply {
        add(action)
    }

    Prefs.putString(USER_ACTION_PREF, App.gson.toJson(newActions))
}

fun getUserActions(): List<UserAction> {
    val actions = getUserActionsRaw()
    val type = object : TypeToken<List<UserAction>>() {}.type
    return App.gson.fromJson(actions, type)
}

fun getUserActionsRaw(): String = Prefs.getString(USER_ACTION_PREF, "[]")

fun purgeUserActions() = Prefs.remove(USER_ACTION_PREF)

fun Context.logAnalyticsEvent(event: String, bundle: Bundle) {
    FirebaseAnalytics.getInstance(this)
            .logEvent(event, bundle)
}

fun Fragment.logAnalyticsEvent(event: String, bundle: Bundle) {
    FirebaseAnalytics.getInstance(context!!)
            .logEvent(event, bundle)
}