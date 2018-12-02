package com.hmomeni.canto.di

import com.hmomeni.canto.DownloadService
import com.hmomeni.canto.activities.MainActivity
import com.hmomeni.canto.fragments.RecorderFragment
import com.hmomeni.canto.vms.*
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = [AppModule::class, ApiModule::class, RoomModule::class])
interface DIComponent {
    interface Injectable {
        fun inject(diComponent: DIComponent)
    }

    fun inject(mainActivity: MainActivity)
    fun inject(mainActivity: LoginViewModel)
    fun inject(mainViewModel: MainViewModel)
    fun inject(listViewModel: ListViewModel)
    fun inject(searchViewModel: SearchViewModel)
    fun inject(recorderFragment: RecorderFragment)
    fun inject(downloadService: DownloadService)
    fun inject(dubsmashViewModel: DubsmashViewModel)
}