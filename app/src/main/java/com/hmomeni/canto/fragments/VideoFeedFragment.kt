package com.hmomeni.canto.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProviders
import androidx.viewpager2.widget.ViewPager2
import com.hmomeni.canto.R
import com.hmomeni.canto.adapters.viewpager.VideoFeedFragmentPagerAdapter
import com.hmomeni.canto.entities.VideoFeedItem
import com.hmomeni.canto.utils.iomain
import com.hmomeni.canto.vms.VideoFeedViewModel
import com.hmomeni.canto.vms.injector
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import kotlinx.android.synthetic.main.fragment_videofeed.*
import timber.log.Timber
import javax.inject.Inject

class VideoFeedFragment : BaseFragment() {

    @Inject
    lateinit var viewModel: VideoFeedViewModel
    private val compositeDisposable = CompositeDisposable()
    private var items: MutableList<VideoFeedItem> = mutableListOf()

    private lateinit var pagerAdapter: VideoFeedFragmentPagerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProviders.of(this, injector.videoFeedViewModelFactory())[VideoFeedViewModel::class.java]
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
            inflater.inflate(R.layout.fragment_videofeed, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewPager.orientation = ViewPager2.ORIENTATION_VERTICAL
        pagerAdapter = VideoFeedFragmentPagerAdapter(activity!!, items)
        viewPager.adapter = pagerAdapter

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                pagerAdapter.pagerEventPublisher.onNext(position)
            }
        })

        viewModel.api.getVideoFeed()
                .iomain()
                .subscribe({
                    items.addAll(it.data)
                    pagerAdapter.notifyDataSetChanged()
                }, {
                    Timber.e(it)
                }).addTo(compositeDisposable)
    }
}