package com.hmomeni.canto.adapters.viewpager

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.hmomeni.canto.R
import com.hmomeni.canto.entities.VideoFeedItem
import com.hmomeni.canto.utils.GlideApp
import com.hmomeni.canto.utils.gone
import kotlinx.android.synthetic.main.activity_video_play.view.*

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
        fun clear() {
            GlideApp.with(itemView.context).clear(itemView.preview)
            GlideApp.with(itemView.context).clear(itemView.artistPhoto)
        }

        fun bind(item: VideoFeedItem) {
            itemView.progressBar.gone()
            GlideApp.with(itemView.context)
                    .load(item.coverPhoto.link)
                    .into(itemView.artistPhoto)
            GlideApp.with(itemView.context)
                    .load(item.song.thumbnail)
                    .into(itemView.preview)
        }
    }
}