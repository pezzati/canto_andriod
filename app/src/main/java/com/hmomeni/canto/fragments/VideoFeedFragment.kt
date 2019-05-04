package com.hmomeni.canto.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProviders
import androidx.viewpager2.widget.ViewPager2
import com.hmomeni.canto.R
import com.hmomeni.canto.adapters.viewpager.VideoFeedPagerAdapter
import com.hmomeni.canto.entities.VideoFeedItem
import com.hmomeni.canto.utils.ViewModelFactory
import com.hmomeni.canto.utils.app
import com.hmomeni.canto.utils.iomain
import com.hmomeni.canto.vms.ProfileViewModel
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import kotlinx.android.synthetic.main.fragment_videofeed.*
import timber.log.Timber

class VideoFeedFragment : BaseFragment() {

    private lateinit var viewModel: ProfileViewModel
    private val compositeDisposable = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProviders.of(this, ViewModelFactory(context!!.app()))[ProfileViewModel::class.java]
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
            inflater.inflate(R.layout.fragment_videofeed, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewPager.orientation = ViewPager2.ORIENTATION_VERTICAL
        viewModel.projectDao.fetchCompleteProjects()
                .map { cp ->
                    cp.map {
                        VideoFeedItem(
                                projectId = it.projectId,
                                project = viewModel.projectDao.getProject(it.projectId).blockingGet(),
                                post = it.post,
                                track = viewModel.trackDao.fetchFinalTrackForProject(it.projectId).blockingGet()
                        )
                    }
                }
                .iomain()
                .subscribe({
                    viewPager.adapter = VideoFeedPagerAdapter(it)
                }, {
                    Timber.e(it)
                }).addTo(compositeDisposable)
    }
}