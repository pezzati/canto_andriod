package com.hmomeni.canto.utils.adapters.viewpager

import android.content.Context
import android.support.v4.view.PagerAdapter
import android.view.View
import android.view.ViewGroup
import com.tiagosantos.enchantedviewpager.EnchantedViewPager

class ModePagerAdapter(private val context: Context, private val views: Array<View>) : PagerAdapter() {
    override fun getCount() = views.size

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        views[position].tag = EnchantedViewPager.ENCHANTED_VIEWPAGER_POSITION + position
        container.addView(views[position])
        return views[position]
    }

    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        return view == `object`
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        container.removeView(`object` as View)
    }
}