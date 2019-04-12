package com.hmomeni.canto.adapters.viewpager

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.hmomeni.canto.R
import com.hmomeni.canto.entities.VideoFeedItem

class VideoFeedPagerAdapter(private val items: List<VideoFeedItem>) : RecyclerView.Adapter<VideoFeedPagerAdapter.VideoFeedHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoFeedHolder {
        return VideoFeedHolder(LayoutInflater.from(parent.context).inflate(R.layout.activity_video_play, parent, false))
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VideoFeedHolder, position: Int) {
        holder.bind(items[position])
    }

    class VideoFeedHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(item: VideoFeedItem) {}
    }
}