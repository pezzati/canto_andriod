package com.hmomeni.canto.di

import android.content.Context
import com.hmomeni.canto.App
import com.hmomeni.canto.activities.*
import com.hmomeni.canto.adapters.rcl.ListPostsRclAdapter
import com.hmomeni.canto.adapters.rcl.PostsRclAdapter
import com.hmomeni.canto.fragments.RecorderFragment
import com.hmomeni.canto.services.DownloadService
import com.hmomeni.canto.services.MuxerService
import com.hmomeni.canto.utils.UserActionSyncWorker
import com.hmomeni.canto.vms.*
import dagger.BindsInstance
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
    fun inject(app: App)
    fun inject(dubsmashActivity: DubsmashActivity)
    fun inject(editViewModel: EditViewModel)
    fun inject(profileViewModel: ProfileViewModel)
    fun inject(videoPlayActivity: VideoPlayActivity)
    fun inject(karaokeActivity: KaraokeActivity)
    fun inject(paymentViewModel: PaymentViewModel)
    fun inject(postHolder: PostsRclAdapter.PostHolder)
    fun inject(muxerService: MuxerService)
    fun inject(listPostHolder: ListPostsRclAdapter.ListPostHolder)
    fun inject(editUserViewModel: EditUserViewModel)
    fun inject(baseActivity: BaseActivity)
    fun inject(userActionSyncWorker: UserActionSyncWorker)

    fun searchViewModelFactory(): ViewModelFactory<SearchViewModel>


    @Component.Builder
    interface Builder {
        @BindsInstance
        fun applicationContext(applicationContext: Context): Builder

        fun appModule(appModule: AppModule): Builder
        fun apiModule(apiModule: ApiModule): Builder
        fun roomModule(roomModule: RoomModule): Builder

        fun build(): DIComponent
    }
}