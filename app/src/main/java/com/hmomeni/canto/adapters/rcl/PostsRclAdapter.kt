package com.hmomeni.canto.adapters.rcl

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.hmomeni.canto.R
import com.hmomeni.canto.entities.Post
import kotlinx.android.synthetic.main.rcl_item_post.view.*

class PostsRclAdapter(private val posts: List<Post>) : RecyclerView.Adapter<PostsRclAdapter.PostHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostHolder {
        return PostHolder(LayoutInflater.from(parent.context).inflate(R.layout.rcl_item_post, parent, false))
    }

    override fun getItemCount() = posts.size

    override fun onBindViewHolder(holder: PostHolder, position: Int) {
        holder.bind(posts[position])
    }

    class PostHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(post: Post) {
            post.coverPhoto?.let {
                Glide.with(itemView)
                        .load(it.link)
                        .into(itemView.postImageView)
            }

        }
    }
}