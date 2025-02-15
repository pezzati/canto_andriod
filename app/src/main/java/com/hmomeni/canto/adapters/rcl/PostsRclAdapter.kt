package com.hmomeni.canto.adapters.rcl

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.hmomeni.canto.R
import com.hmomeni.canto.entities.Post
import com.hmomeni.canto.entities.UserInventory
import com.hmomeni.canto.utils.GlideApp
import com.hmomeni.canto.utils.app
import com.hmomeni.canto.utils.dpToPx
import com.hmomeni.canto.utils.rounded
import io.reactivex.processors.PublishProcessor
import kotlinx.android.synthetic.main.rcl_item_post_rect.view.*
import java.util.*
import javax.inject.Inject

class PostsRclAdapter(private val posts: List<Post>) : RecyclerView.Adapter<PostsRclAdapter.PostHolder>() {

    val clickPublisher: PublishProcessor<Int> = PublishProcessor.create()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostHolder {
        return PostHolder(LayoutInflater.from(parent.context).inflate(R.layout.rcl_item_post_rect, parent, false), clickPublisher)
    }

    override fun getItemCount() = posts.size

    override fun onBindViewHolder(holder: PostHolder, position: Int) {
        holder.bind(posts[position])
    }

    class PostHolder(itemView: View, clickPublisher: PublishProcessor<Int>) : RecyclerView.ViewHolder(itemView) {

        @Inject
        lateinit var userInventory: UserInventory

        init {
            itemView.context.app().di.inject(this)
            itemView.setOnClickListener {
                clickPublisher.onNext(adapterPosition)
            }
        }

        fun bind(post: Post) {
            GlideApp.with(itemView)
                    .load(post.coverPhoto?.link)
                    .placeholder(R.drawable.post_placeholder)
                    .rounded(dpToPx(15))
                    .into(itemView.postImageView)

            itemView.artistName.text = post.artist!!.name
            itemView.trackName.text = post.name
            val count = userInventory.items.get(post.id, -1)
            if (count > 0) {
                itemView.price.text = "X %d".format(Locale.ENGLISH, count)
                itemView.price.setCompoundDrawables(null, null, null, null)
            } else {
                if (post.price == 0L) {
                    itemView.price.setText(R.string.free)
                } else {
                    itemView.price.text = post.price.toString()
                }
            }
        }
    }
}