package com.hmomeni.canto.adapters.rcl

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager.widget.ViewPager
import com.hmomeni.canto.R
import com.hmomeni.canto.adapters.viewpager.BannerPagerAdapter
import com.hmomeni.canto.entities.Banner
import com.hmomeni.canto.entities.Genre
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.processors.PublishProcessor
import io.reactivex.rxkotlin.plusAssign
import kotlinx.android.synthetic.main.rcl_item_banner.view.*
import kotlinx.android.synthetic.main.rcl_item_genre.view.*

const val TYPE_BANNER = 0
const val TYPE_GENRE = 1

class MainRclAdapter(val banners: List<Banner>, private val genres: List<Genre>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    val clickPublisher: PublishProcessor<ClickEvent> = PublishProcessor.create()

    override fun getItemViewType(position: Int): Int {
        return when (position) {
            0 -> TYPE_BANNER
            else -> TYPE_GENRE
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_BANNER -> BannerHolder(LayoutInflater.from(parent.context).inflate(R.layout.rcl_item_banner, parent, false), clickPublisher)
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

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        if (holder is GenreHolder) {
            holder.clear()
        }
    }

    class BannerHolder(itemView: View, clickPublisher: PublishProcessor<ClickEvent>) : RecyclerView.ViewHolder(itemView) {

        private val mAdapter = BannerPagerAdapter(clickPublisher)
        private var switchPageStarted = false

        init {
            itemView.bannerViewPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
                override fun onPageScrollStateChanged(state: Int) {

                }

                override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {

                }

                override fun onPageSelected(position: Int) {
                    itemView.pageIndicatorView.selection = position
                }
            })
        }

        fun bind(banners: List<Banner>) {
            mAdapter.banners = banners
            mAdapter.notifyDataSetChanged()
            itemView.bannerViewPager.adapter = mAdapter
            itemView.pageIndicatorView.count = banners.size
            if (!switchPageStarted && mAdapter.count > 0) {
                switchPage()
                switchPageStarted = true
            }
        }

        private fun switchPage() {
            itemView.postDelayed({
                var currentItem = itemView.bannerViewPager.currentItem
                if (currentItem >= mAdapter.banners.size - 1) {
                    currentItem = -1
                }
                itemView.bannerViewPager.currentItem = ++currentItem
                switchPage()
            }, 5000)
        }
    }

    class GenreHolder(itemView: View, private val clickPublisher: PublishProcessor<ClickEvent>) : RecyclerView.ViewHolder(itemView) {
        init {
            itemView.moreBtn.setOnClickListener {
                clickPublisher.onNext(ClickEvent(ClickEvent.Type.GENRE, adapterPosition, -1))
            }
        }

        private val compositeDisposable = CompositeDisposable()

        fun bind(genre: Genre) {
            itemView.genreName.text = genre.name
            itemView.genresRecyclerView.layoutManager = LinearLayoutManager(itemView.context, LinearLayoutManager.HORIZONTAL, false)
            itemView.genresRecyclerView.adapter = PostsRclAdapter(genre.posts!!).also {
                compositeDisposable += it.clickPublisher.subscribe { pos ->
                    clickPublisher.onNext(ClickEvent(ClickEvent.Type.GENRE, adapterPosition, pos))
                }
            }
        }

        fun clear() {
            compositeDisposable.clear()
        }

    }

    class ClickEvent(val type: Type, val row: Int, val item: Int) {
        enum class Type {
            BANNER, GENRE
        }
    }
}