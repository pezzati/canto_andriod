package com.hmomeni.canto.utils

import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider
import com.hmomeni.canto.App
import com.hmomeni.canto.di.DIComponent

class ViewModelFactory(private var app: App) : ViewModelProvider.NewInstanceFactory() {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val t = super.create(modelClass)
        if (t is DIComponent.Injectable) {
            (t as DIComponent.Injectable).inject(app.di)
        }
        return t
    }
}