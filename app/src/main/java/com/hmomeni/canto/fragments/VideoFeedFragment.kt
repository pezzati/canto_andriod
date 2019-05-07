package com.hmomeni.canto.fragments

import android.media.MediaPlayer
import android.os.Bundle
import android.view.*
import androidx.constraintlayout.widget.ConstraintLayout
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
    val mediaPlayer = MediaPlayer()
    lateinit var items: List<VideoFeedItem>

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
                    items = it
                    viewPager.adapter = VideoFeedPagerAdapter(it)
                }, {
                    Timber.e(it)
                }).addTo(compositeDisposable)
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                val surfaceView = ((viewPager.getChildAt(0) as ViewGroup).getChildAt(1) as ConstraintLayout).getChildAt(1) as SurfaceView
                mediaPlayer.reset()
                mediaPlayer.setDataSource(items[position].track?.filePath)
                mediaPlayer.setOnPreparedListener {
                    it.start()
                }
                mediaPlayer.prepareAsync()

                if (surfaceView.holder.surface.isValid) {
                    mediaPlayer.setSurface(surfaceView.holder.surface)
                } else {
                    surfaceView.holder.addCallback(object : SurfaceHolder.Callback {
                        override fun surfaceChanged(holder: SurfaceHolder, format: Int, w: Int, h: Int) {

                        }

                        override fun surfaceDestroyed(holder: SurfaceHolder) {
                        }

                        override fun surfaceCreated(holder: SurfaceHolder) {
                            Timber.d("Surface Ready!")
                            mediaPlayer.setSurface(holder.surface)
                        }
                    })
                }
            }
        })
    }
}