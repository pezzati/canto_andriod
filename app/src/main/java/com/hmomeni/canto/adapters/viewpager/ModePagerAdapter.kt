package com.hmomeni.canto.adapters.viewpager

import android.content.Context
import android.support.v4.view.PagerAdapter
import android.view.View
import android.view.ViewGroup
import com.hmomeni.canto.utils.dpToPx
import com.hmomeni.canto.utils.views.RoundedFrameLayout

class ModePagerAdapter(private val context: Context, private val views: Array<View>) : PagerAdapter() {
    override fun getCount() = views.size

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val roundedFrameLayout = RoundedFrameLayout(context).apply {
            layoutParams = ViewGroup.LayoutParams(dpToPx(450), dpToPx(200))
        }
        roundedFrameLayout.addView(views[position])
        container.addView(roundedFrameLayout)
        return roundedFrameLayout
    }

    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        return view == `object`
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        container.removeView(`object` as View)
    }
}