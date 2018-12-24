package com.hmomeni.canto.adapters.rcl

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.hmomeni.canto.entities.Post
import com.hmomeni.canto.utils.GlideApp
import com.hmomeni.canto.utils.dpToPx
import com.hmomeni.canto.utils.rounded
import kotlinx.android.synthetic.main.rcl_item_list_post.view.*

class ListPostsRclAdapter(private val posts: List<Post>, private val layoutResId: Int) : RecyclerView.Adapter<ListPostsRclAdapter.ListPostHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            ListPostHolder(LayoutInflater.from(parent.context).inflate(layoutResId, parent, false))

    override fun getItemCount() = posts.size

    override fun onBindViewHolder(holder: ListPostHolder, position: Int) {
        holder.bind(posts[position])
    }

    class ListPostHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(post: Post) {
            post.coverPhoto?.let {
                GlideApp.with(itemView)
                        .load(it.link)
                        .rounded(dpToPx(15))
                        .into(itemView.postImageView)
            }

            itemView.artistName.text = post.artist!!.name
            itemView.trackName.text = post.name
        }
    }
}