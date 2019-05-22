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
    private var mediaPlayer: MediaPlayer? = null
    lateinit var items: List<VideoFeedItem>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProviders.of(this, injector.videoFeedViewModelFactory())[VideoFeedViewModel::class.java]
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
            inflater.inflate(R.layout.fragment_videofeed, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewPager.orientation = ViewPager2.ORIENTATION_VERTICAL

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                val surfaceView = ((viewPager.getChildAt(0) as ViewGroup).getChildAt(1) as ConstraintLayout).getChildAt(1) as SurfaceView
                mediaPlayer = MediaPlayer()
                mediaPlayer?.setDataSource(items[position].song.fileUrl)
                mediaPlayer?.setOnPreparedListener {
                    it.start()
                }
                mediaPlayer?.prepareAsync()

                if (surfaceView.holder.surface.isValid) {
                    mediaPlayer?.setSurface(surfaceView.holder.surface)
                } else {
                    surfaceView.holder.addCallback(object : SurfaceHolder.Callback {
                        override fun surfaceChanged(holder: SurfaceHolder, format: Int, w: Int, h: Int) {

                        }

                        override fun surfaceDestroyed(holder: SurfaceHolder) {
                            mediaPlayer?.setSurface(null)
                        }

                        override fun surfaceCreated(holder: SurfaceHolder) {
                            Timber.d("Surface Ready!")
                            mediaPlayer?.setSurface(holder.surface)
                        }
                    })
                }
            }
        })

        viewModel.api.getVideoFeed()
                .iomain()
                .subscribe({
                    items = it.data
                    viewPager.adapter = VideoFeedPagerAdapter(it.data)
                }, {
                    Timber.e(it)
                }).addTo(compositeDisposable)
    }
}