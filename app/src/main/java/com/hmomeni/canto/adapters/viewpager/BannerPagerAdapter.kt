package com.hmomeni.canto.adapters.viewpager

import android.support.v4.view.PagerAdapter
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.hmomeni.canto.entities.Banner
import com.hmomeni.canto.utils.dpToPx

class BannerPagerAdapter(private val banners: List<Banner>) : PagerAdapter() {
    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val padding = dpToPx(8)
        val imageView = ImageView(container.context).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            setPadding(padding, padding, padding, padding)
        }
        Glide.with(container.context)
                .load(banners[position].file)
                .into(imageView)
        container.addView(imageView)
        return imageView
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        Glide.with(`object` as ImageView).clear(`object`)
        container.removeView(`object`)
    }

    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        return view == `object`
    }

    override fun getCount() = banners.size
}