package com.hmomeni.canto.adapters.viewpager

import android.content.Context
import android.support.v4.view.PagerAdapter
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.RelativeLayout
import com.hmomeni.canto.R
import com.hmomeni.canto.utils.dpToPx
import com.hmomeni.canto.utils.views.RoundedRelativeLayout

class ModePagerAdapter(private val context: Context, private val views: Array<View>) : PagerAdapter() {
    override fun getCount() = views.size

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val roundedLayout = RoundedRelativeLayout(context).apply {
            layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                gravity = Gravity.CENTER
            }
        }
        val targetView = views[position].apply {
            id = View.generateViewId()
            layoutParams = RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                addRule(RelativeLayout.CENTER_IN_PARENT)
            }
        }
        roundedLayout.addView(targetView)

        if (position in 0..1) {
            roundedLayout.addView(View(context).apply {
                layoutParams = RelativeLayout.LayoutParams(dpToPx(203), dpToPx(75)).apply {
                    addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
                    addRule(RelativeLayout.CENTER_HORIZONTAL)
                }
                setBackgroundResource(R.drawable.ic_record_model)
            })
        }

        val view = View(context).apply {
            layoutParams = RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                addRule(RelativeLayout.ALIGN_START, targetView.id)
                addRule(RelativeLayout.ALIGN_TOP, targetView.id)
                addRule(RelativeLayout.ALIGN_END, targetView.id)
                addRule(RelativeLayout.ALIGN_BOTTOM, targetView.id)
            }
            setBackgroundResource(when (position) {
                0 -> R.drawable.gradient_dubsmash
                1 -> R.drawable.gradient_singing
                3 -> R.drawable.gradient_karaoke
                else -> 0
            })
        }
        roundedLayout.addView(view)
        val frameLayout = FrameLayout(context).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            addView(roundedLayout)
        }

        container.addView(frameLayout)
        return frameLayout
    }

    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        return view == `object`
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        container.removeView(`object` as View)
    }
}