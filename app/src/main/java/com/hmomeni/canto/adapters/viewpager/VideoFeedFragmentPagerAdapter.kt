package com.hmomeni.canto.adapters.viewpager

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.hmomeni.canto.entities.VideoFeedItem
import com.hmomeni.canto.fragments.VideoPlayFragment
import io.reactivex.processors.PublishProcessor

class VideoFeedFragmentPagerAdapter(activity: FragmentActivity, val items: List<VideoFeedItem>) : FragmentStateAdapter(activity) {

    val pagerEventPublisher = PublishProcessor.create<Int>()
    override fun getItem(position: Int): Fragment {
        return VideoPlayFragment(position, items[position], pagerEventPublisher)
    }

    override fun getItemCount() = items.size
}