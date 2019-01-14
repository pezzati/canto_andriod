package com.hmomeni.canto.adapters.rcl

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.hmomeni.canto.R
import com.hmomeni.canto.entities.Avatar
import com.hmomeni.canto.utils.GlideApp
import com.hmomeni.canto.utils.rounded
import io.reactivex.processors.PublishProcessor
import kotlinx.android.synthetic.main.rcl_item_avatar.view.*

class AvatarsRclAdapter(val items: List<Avatar>) : RecyclerView.Adapter<AvatarsRclAdapter.AvatarHolder>() {
    val clickPublisher: PublishProcessor<Int> = PublishProcessor.create()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AvatarHolder {
        return AvatarHolder(LayoutInflater.from(parent.context).inflate(R.layout.rcl_item_avatar, parent, false), clickPublisher)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: AvatarHolder, position: Int) {
        holder.bind(items[position])
    }

    class AvatarHolder(itemView: View, clickPublisher: PublishProcessor<Int>) : RecyclerView.ViewHolder(itemView) {
        init {
            itemView.setOnClickListener {
                clickPublisher.onNext(adapterPosition)
            }
        }

        fun bind(avatar: Avatar) {
            GlideApp.with(itemView)
                    .load(avatar.link)
                    .rounded(10)
                    .into(itemView.avatar)
        }
    }
}