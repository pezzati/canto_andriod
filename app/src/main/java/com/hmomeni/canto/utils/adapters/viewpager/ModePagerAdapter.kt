package com.hmomeni.canto.utils.adapters.viewpager

import android.content.Context
import android.view.View
import android.view.ViewGroup
import com.hmomeni.canto.utils.RecordMode
import com.hmomeni.canto.utils.views.AutoFitTextureView
import com.tiagosantos.enchantedviewpager.EnchantedViewPager
import com.tiagosantos.enchantedviewpager.EnchantedViewPagerAdapter

class ModePagerAdapter(private val context: Context, private val textureViews: Array<AutoFitTextureView>, modes: List<RecordMode>) : EnchantedViewPagerAdapter(modes) {
    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        textureViews[position].tag = EnchantedViewPager.ENCHANTED_VIEWPAGER_POSITION + position
        container.addView(textureViews[position])
        return textureViews[position]
    }

    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        return view == `object`
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        container.removeView(`object` as View)
    }
}