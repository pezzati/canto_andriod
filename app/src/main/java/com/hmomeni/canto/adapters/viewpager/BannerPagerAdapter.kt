package com.hmomeni.canto.adapters.viewpager

import android.support.v4.view.PagerAdapter
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.hmomeni.canto.adapters.rcl.MainRclAdapter
import com.hmomeni.canto.entities.Banner
import com.hmomeni.canto.utils.GlideApp
import com.hmomeni.canto.utils.dpToPx
import com.hmomeni.canto.utils.rounded
import io.reactivex.processors.PublishProcessor

class BannerPagerAdapter(private val clickPublisher: PublishProcessor<MainRclAdapter.ClickEvent>) : PagerAdapter() {
    lateinit var banners: List<Banner>

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val padding = dpToPx(8)
        val frameLayout = FrameLayout(container.context).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            setPadding(padding, padding, padding, 0)
        }
        val imageView = ImageView(container.context).apply {
            layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }
        GlideApp.with(container.context)
                .load(banners[position].file)
                .rounded(dpToPx(15))
                .into(imageView)
        frameLayout.addView(imageView)
        container.addView(frameLayout)
        frameLayout.tag = position
        frameLayout.setOnClickListener {
            clickPublisher.onNext(MainRclAdapter.ClickEvent(MainRclAdapter.ClickEvent.Type.BANNER, -1, it.tag as Int))
        }
        return frameLayout
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        Glide.with(`object` as FrameLayout).clear(`object`.getChildAt(0) as ImageView)
        container.removeView(`object`)
    }

    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        return view == `object`
    }

    override fun getCount() = banners.size
}