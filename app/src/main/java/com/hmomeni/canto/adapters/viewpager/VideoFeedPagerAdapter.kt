package com.hmomeni.canto.adapters.viewpager

import android.media.MediaPlayer
import android.view.LayoutInflater
import android.view.SurfaceHolder
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.hmomeni.canto.R
import com.hmomeni.canto.entities.VideoFeedItem
import com.hmomeni.canto.utils.GlideApp
import kotlinx.android.synthetic.main.activity_video_play.view.*
import timber.log.Timber

class VideoFeedPagerAdapter(private val items: List<VideoFeedItem>) : RecyclerView.Adapter<VideoFeedPagerAdapter.VideoFeedHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoFeedHolder {
        return VideoFeedHolder(LayoutInflater.from(parent.context).inflate(R.layout.activity_video_play, parent, false))
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VideoFeedHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun onViewRecycled(holder: VideoFeedHolder) {
        holder.clear()
        super.onViewRecycled(holder)
    }

    class VideoFeedHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val mediaPlayer: MediaPlayer = MediaPlayer()
        fun clear() {
            mediaPlayer.reset()
            mediaPlayer.release()
        }

        fun bind(item: VideoFeedItem) {
            GlideApp.with(itemView.context)
                    .load(item.post?.coverPhoto)
                    .into(itemView.artistPhoto)
            mediaPlayer.reset()
            mediaPlayer.setDataSource(item.track?.filePath)
            mediaPlayer.setOnPreparedListener {
                it.start()
            }
            mediaPlayer.prepareAsync()

            if (itemView.surfaceView.holder.surface.isValid) {
                mediaPlayer.setSurface(itemView.surfaceView.holder.surface)
            } else {
                itemView.surfaceView.holder.addCallback(object : SurfaceHolder.Callback {
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
    }
}