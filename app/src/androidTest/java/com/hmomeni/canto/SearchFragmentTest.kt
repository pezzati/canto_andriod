package com.hmomeni.canto

import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.filters.LargeTest
import androidx.test.runner.AndroidJUnit4
import com.hmomeni.canto.fragments.SearchFragment
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class SearchFragmentTest {

    @Test
    fun test_RecyclerView_presence() {
        launchFragmentInContainer<SearchFragment>()
        onView(withId(R.id.recyclerView)).check(matches(isDisplayed()))
    }
}