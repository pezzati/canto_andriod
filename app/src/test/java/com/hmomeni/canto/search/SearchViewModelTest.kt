package com.hmomeni.canto.search

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.hmomeni.canto.TestApp
import com.hmomeni.canto.di.AppModule
import com.hmomeni.canto.di.DaggerTestDIComponent
import com.hmomeni.canto.di.RoomModule
import com.hmomeni.canto.entities.ApiResponse
import com.hmomeni.canto.entities.Post
import com.hmomeni.canto.vms.SearchViewModel
import io.reactivex.Single
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.`when`
import org.robolectric.RobolectricTestRunner
import javax.inject.Inject

@RunWith(RobolectricTestRunner::class)
class SearchViewModelTest {

    @Inject
    lateinit var viewModel: SearchViewModel

    @Before
    fun setup() {
        val app: Application = ApplicationProvider.getApplicationContext<TestApp>()
        DaggerTestDIComponent.builder()
                .applicationContext(app)
                .appModule(AppModule(app))
                .roomModule(RoomModule(app))
                .build()
                .inject(this)

        // Api.searchInGenres
        `when`(viewModel.api.searchInGenres(anyString()))
                .thenReturn(Single.just(ApiResponse(listOf())))

        `when`(viewModel.api.searchInGenres("bani"))
                .thenReturn(Single.just(ApiResponse(listOf(
                        Post(1, "Behnam Bani"),
                        Post(2, "Bana Behi")
                ))))
    }

    @Test
    fun `test SearchViewModel`() {
        assertEquals(0, viewModel.result.size)
    }

    @Test
    fun `test search result empty`() {
        viewModel.search("24353").test().assertOf {
            assertEquals(0, viewModel.result.size)
        }
    }

    @Test
    fun `test search result`() {
        viewModel.search("bani").test().assertOf {
            assertEquals(2, viewModel.result.size)
        }

    }

}