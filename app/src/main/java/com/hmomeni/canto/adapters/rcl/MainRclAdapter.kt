package com.hmomeni.canto.adapters.rcl

import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.hmomeni.canto.R
import com.hmomeni.canto.adapters.viewpager.BannerPagerAdapter
import com.hmomeni.canto.entities.Banner
import com.hmomeni.canto.entities.Genre
import io.reactivex.processors.PublishProcessor
import kotlinx.android.synthetic.main.rcl_item_banner.view.*
import kotlinx.android.synthetic.main.rcl_item_genre.view.*

const val TYPE_BANNER = 0
const val TYPE_GENRE = 1

class MainRclAdapter(private val banners: List<Banner>, private val genres: List<Genre>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    val clickPublisher: PublishProcessor<Pair<Int, Int>> = PublishProcessor.create()

    override fun getItemViewType(position: Int): Int {
        return when (position) {
            0 -> TYPE_BANNER
            else -> TYPE_GENRE
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_BANNER -> BannerHolder(LayoutInflater.from(parent.context).inflate(R.layout.rcl_item_banner, parent, false))
            TYPE_GENRE -> GenreHolder(LayoutInflater.from(parent.context).inflate(R.layout.rcl_item_genre, parent, false), clickPublisher)
            else -> throw RuntimeException("Invalid Item ViewType")
        }
    }

    override fun getItemCount() = genres.size + 1

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is BannerHolder -> holder.bind(banners)
            is GenreHolder -> holder.bind(genres[position - 1])
        }
    }

    class BannerHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(banners: List<Banner>) {
            itemView.bannerViewPager.adapter = BannerPagerAdapter(banners)
        }
    }

    class GenreHolder(itemView: View, clickPublisher: PublishProcessor<Pair<Int, Int>>) : RecyclerView.ViewHolder(itemView) {
        init {
            itemView.moreBtn.setOnClickListener {
                clickPublisher.onNext(Pair(adapterPosition, -1))
            }
        }

        fun bind(genre: Genre) {
            itemView.genreName.text = genre.name
            itemView.genresRecyclerView.layoutManager = LinearLayoutManager(itemView.context, LinearLayoutManager.HORIZONTAL, false)
            itemView.genresRecyclerView.adapter = PostsRclAdapter(genre.posts!!)
        }
    }
}