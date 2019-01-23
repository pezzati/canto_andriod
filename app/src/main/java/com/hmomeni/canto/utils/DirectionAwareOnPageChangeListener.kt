package com.hmomeni.canto.utils

import androidx.viewpager.widget.ViewPager
import timber.log.Timber

abstract class DirectionAwareOnPageChangeListener : ViewPager.OnPageChangeListener {
    private var sum = 0
    private var prevPos = 0
    private var lastDir: Boolean = false

    override fun onPageScrollStateChanged(state: Int) {
        when (state) {
            ViewPager.SCROLL_STATE_IDLE -> {
                sum = 0
            }
        }

    }

    override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
        if (prevPos != 0) {
            sum += prevPos - positionOffsetPixels
        }
        prevPos = positionOffsetPixels
        Timber.d("sum: %d", sum)
        onPageScrolled(position, positionOffset, positionOffsetPixels, if (sum == 0) lastDir else sum < 0)
        lastDir = sum < 0
    }

    override fun onPageSelected(position: Int) {

    }

    abstract fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int, isRight: Boolean)
}