package com.hmomeni.canto.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import com.google.firebase.analytics.FirebaseAnalytics

open class BaseFragment : Fragment() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseAnalytics.getInstance(context!!)
                .setCurrentScreen(activity!!, this.javaClass.simpleName, null)
    }
}