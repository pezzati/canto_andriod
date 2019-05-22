package com.hmomeni.canto.fragments

import android.media.MediaPlayer
import android.os.Bundle
import android.view.LayoutInflater
import android.view.SurfaceHolder
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.hmomeni.canto.R
import com.hmomeni.canto.entities.VideoFeedItem
import com.hmomeni.canto.utils.gone
import kotlinx.android.synthetic.main.activity_video_play.*
import timber.log.Timber

class VideoPlayFragment(private val videoFeedItem: VideoFeedItem) : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.activity_video_play, container, false)
    }

    val mediaPlayer = MediaPlayer()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        progressBar.gone()

        mediaPlayer.setDataSource(videoFeedItem.song.fileUrl)
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
                    mediaPlayer.setSurface(null)
                }

                override fun surfaceCreated(holder: SurfaceHolder) {
                    Timber.d("Surface Ready!")
                    mediaPlayer.setSurface(holder.surface)
                }
            })
        }
    }

    override fun onPause() {
        super.onPause()
        Timber.d("onPause")
    }
}