package com.hmomeni.canto.di

import android.content.Context
import com.hmomeni.canto.list.ListViewModelUnitTest
import com.hmomeni.canto.search.SearchViewModelTest
import com.hmomeni.canto.vms.SearchViewModel
import com.hmomeni.canto.vms.ViewModelFactory
import dagger.BindsInstance
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = [AppModule::class, TestApiModule::class, RoomModule::class])
interface TestDIComponent {
    interface Injectable {
        fun inject(diComponent: TestDIComponent)
    }

    fun searchViewModelFactory(): ViewModelFactory<SearchViewModel>

    fun inject(searchViewModelTest: SearchViewModelTest)
    fun inject(searchViewModelTest: ListViewModelUnitTest)


    @Component.Builder
    interface Builder {
        @BindsInstance
        fun applicationContext(applicationContext: Context): Builder

        fun appModule(appModule: AppModule): Builder
        fun apiModule(apiModule: TestApiModule): Builder
        fun roomModule(roomModule: RoomModule): Builder

        fun build(): TestDIComponent
    }
}