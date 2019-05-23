package com.hmomeni.canto.fragments

import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.SurfaceHolder
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.hmomeni.canto.R
import com.hmomeni.canto.entities.VideoFeedItem
import com.hmomeni.canto.utils.GlideApp
import com.hmomeni.canto.utils.dpToPx
import com.hmomeni.canto.utils.gone
import com.hmomeni.canto.utils.rounded
import io.reactivex.disposables.Disposable
import io.reactivex.processors.PublishProcessor
import kotlinx.android.synthetic.main.activity_video_play.*
import timber.log.Timber

class VideoPlayFragment(private val position: Int, private val videoFeedItem: VideoFeedItem, private val pagerEventPublisher: PublishProcessor<Int>) : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.activity_video_play, container, false)
    }

    private var disposable: Disposable? = null
    val mediaPlayer = MediaPlayer()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        progressBar.gone()

        fastBack.gone()
        fastForward.gone()
        fastBackText.gone()
        fastForwardText.gone()
        shareBtn.gone()
        trackName.gone()
        backBtn.gone()

        GlideApp.with(artistPhoto)
                .load(videoFeedItem.coverPhoto.link)
                .rounded(dpToPx(5))
                .into(artistPhoto)
        artistName.text = videoFeedItem.song.karaoke.artistName

        mediaPlayer.setDataSource(videoFeedItem.song.fileUrl)
        mediaPlayer.setOnPreparedListener {
            it.start()
            timer()
        }
        mediaPlayer.prepareAsync()

        if (surfaceView.holder.surface.isValid) {
            mediaPlayer.setSurface(surfaceView.holder.surface)
        } else {
            surfaceView.holder.addCallback(object : SurfaceHolder.Callback {
                override fun surfaceChanged(holder: SurfaceHolder, format: Int, w: Int, h: Int) {

                }

                override fun surfaceDestroyed(holder: SurfaceHolder) {
                    mediaPlayer.setSurface(null)
                }

                override fun surfaceCreated(holder: SurfaceHolder) {
                    Timber.d("Surface Ready!")
                    mediaPlayer.setSurface(holder.surface)
                }
            })
        }

        disposable = pagerEventPublisher.subscribe {
            if (position != it && mediaPlayer.isPlaying) {
                mediaPlayer.stop()
            } else if (position == it && !mediaPlayer.isPlaying) {
                mediaPlayer.start()
                timer()
            }
        }
    }

    private val handler = Handler()
    private fun timer() {
        if (isAdded) {
            val time = mediaPlayer.currentPosition / 1000
            timerText.text = "%02d:%02d".format(time / 60, time % 60)
            if (mediaPlayer.isPlaying) {
                handler.postDelayed({ timer() }, 500)
            }
        }
    }

    override fun onDestroyView() {
        disposable?.dispose()

        mediaPlayer.stop()
        mediaPlayer.release()

        super.onDestroyView()
    }
}