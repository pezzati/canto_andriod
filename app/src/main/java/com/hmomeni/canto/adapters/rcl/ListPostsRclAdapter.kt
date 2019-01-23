package com.hmomeni.canto.adapters.rcl

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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

class ListPostsRclAdapter(val posts: List<Post>, private val layoutResId: Int) : androidx.recyclerview.widget.RecyclerView.Adapter<ListPostsRclAdapter.ListPostHolder>() {
    val clickPublisher: PublishProcessor<Int> = PublishProcessor.create()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            ListPostHolder(LayoutInflater.from(parent.context).inflate(layoutResId, parent, false), clickPublisher)

    override fun getItemCount() = posts.size

    override fun onBindViewHolder(holder: ListPostHolder, position: Int) {
        holder.bind(posts[position])
    }

    class ListPostHolder(itemView: View, clickPublisher: PublishProcessor<Int>) : androidx.recyclerview.widget.RecyclerView.ViewHolder(itemView) {
        init {
            itemView.context.app().di.inject(this)
            itemView.setOnClickListener {
                clickPublisher.onNext(adapterPosition)
            }
        }

        @Inject
        lateinit var userInventory: UserInventory

        fun bind(post: Post) {
            post.coverPhoto?.let {
                GlideApp.with(itemView)
                        .load(it.link)
                        .rounded(dpToPx(10))
                        .into(itemView.postImageView)
            }

            itemView.artistName.text = post.artist!!.name
            itemView.trackName.text = post.name

            val count = userInventory.items.get(post.id, -1)
            if (count > 0) {
                itemView.price.text = "X %d".format(Locale.ENGLISH, count)
                itemView.price.setCompoundDrawables(null, null, null, null)
                itemView.price.visibility = View.VISIBLE
                itemView.giftView.visibility = View.GONE
            } else {
                if (post.price == 0L) {
                    itemView.price.visibility = View.GONE
                    itemView.giftView.visibility = View.VISIBLE
                } else {
                    itemView.price.visibility = View.VISIBLE
                    itemView.giftView.visibility = View.GONE
                    itemView.price.text = post.price.toString()
                }
            }
        }
    }
}