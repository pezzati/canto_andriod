package com.hmomeni.canto.list

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.runner.AndroidJUnit4
import com.hmomeni.canto.TestApp
import com.hmomeni.canto.di.AppModule
import com.hmomeni.canto.di.DaggerTestDIComponent
import com.hmomeni.canto.entities.ApiResponse
import com.hmomeni.canto.entities.Post
import com.hmomeni.canto.vms.ListViewModel
import io.reactivex.Single
import junit.framework.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.`when`
import org.mockito.Mockito.anyString
import javax.inject.Inject

@RunWith(AndroidJUnit4::class)
class ListViewModelUnitTest {
    @Inject
    lateinit var viewModel: ListViewModel
    val POSTS = listOf(
            Post(1, "Behnam Bani"),
            Post(2, "Bana Behi"),
            Post(2, "Bana Behi"),
            Post(2, "Bana Behi"),
            Post(2, "Bana Behi"),
            Post(2, "Bana Behi"),
            Post(2, "Bana Behi")
    )

    @Before
    fun setup() {
        val app: Application = ApplicationProvider.getApplicationContext<TestApp>()
        DaggerTestDIComponent.builder()
                .applicationContext(app)
                .appModule(AppModule(app))
                .build()
                .inject(this)

        `when`(viewModel.api.getGenrePosts(anyString())).thenReturn(Single.just(ApiResponse(listOf())))

        `when`(viewModel.api.getGenrePosts("error")).thenReturn(Single.error(Exception()))

        `when`(viewModel.api.getGenrePosts("some")).thenReturn(Single.just(ApiResponse(POSTS)))

        `when`(viewModel.api.getGenrePosts("more")).thenReturn(Single.just(ApiResponse(POSTS, "more1")))

        `when`(viewModel.api.getGenrePosts("more1")).thenReturn(Single.just(ApiResponse(POSTS, "more1")))
    }

    @Test
    fun `test empty result`() {
        viewModel.urlPath = "empty"
        viewModel.loadPosts().test().assertOf {
            assertEquals(0, viewModel.posts.size)
        }
    }

    @Test
    fun `test some result`() {
        viewModel.urlPath = "some"
        viewModel.loadPosts().test().assertOf {
            assertEquals(7, viewModel.posts.size)
        }
    }

    @Test
    fun `test error result`() {
        viewModel.urlPath = "error"
        viewModel.loadPosts().test().assertError(Exception::class.java).assertOf {
            assertEquals(0, viewModel.posts.size)
        }
    }

    @Test
    fun `test more result nextUrl`() {
        viewModel.urlPath = "more"
        viewModel.loadPosts().test().assertOf {
            assertEquals(7, viewModel.posts.size)
            assertEquals("more1", viewModel.nextUrl)
        }
    }

    @Test
    fun `test more result`() {
        viewModel.urlPath = "more"
        viewModel.loadPosts().test().assertOf {
            viewModel.loadNextPage()!!.test().assertOf {
                assertEquals(14, viewModel.posts.size)
            }
        }
    }

    @Test
    fun `test more result never`() {
        viewModel.urlPath = "more"
        viewModel.loadPosts().test().assertOf {
            viewModel.loadNextPage()!!.test().assertOf {
                viewModel.loadNextPage()!!.test().assertEmpty()
            }
        }
    }
}