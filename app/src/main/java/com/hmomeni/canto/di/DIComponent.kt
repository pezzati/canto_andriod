package com.hmomeni.canto.di

import com.hmomeni.canto.activities.MainActivity
import com.hmomeni.canto.vms.ListViewModel
import com.hmomeni.canto.vms.LoginViewModel
import com.hmomeni.canto.vms.MainViewModel
import com.hmomeni.canto.vms.SearchViewModel
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = [AppModule::class, ApiModule::class])
interface DIComponent {
    interface Injectable {
        fun inject(diComponent: DIComponent)
    }
    fun inject(mainActivity: MainActivity)
    fun inject(mainActivity: LoginViewModel)
    fun inject(mainViewModel: MainViewModel)
    fun inject(listViewModel: ListViewModel)
    fun inject(searchViewModel: SearchViewModel)
}