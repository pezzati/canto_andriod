package com.hmomeni.canto.utils.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.hmomeni.canto.utils.dpToPx

class VerticalSlider : View {
    constructor(context: Context) : super(context)
    constructor(context: Context, attributeSet: AttributeSet) : super(context, attributeSet)

    private val layoutRect: RectF = RectF(0f, 0f, measuredWidth.toFloat(), measuredHeight.toFloat())
    private val layoutPaint = Paint().apply {
        color = Color.parseColor("#55ffffff")
        isAntiAlias = true
    }
    private val progressRect: RectF = RectF(0f, 0f, measuredWidth.toFloat(), measuredHeight.toFloat())
    private val progressPaint = Paint().apply {
        color = Color.WHITE
        isAntiAlias = true
    }

    var onProgressChangeListener: OnSliderProgressChangeListener? = null
    var max: Int = 10
    var progress: Int = 5
        set(value) {
            if (value > max) {
                throw RuntimeException("progress must not be larger than max")
            }
            field = value
            onProgressChangeListener?.onChanged(progress, max)
            progressRect.set(0f, (1 - calculateProgress()) * measuredHeight, measuredWidth.toFloat(), measuredHeight.toFloat())
            invalidate()
        }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        if (measuredHeight > 0 && measuredWidth > 0) {
            layoutRect.set(0f, 0f, measuredWidth.toFloat(), measuredHeight.toFloat())
            progressRect.set(0f, (1 - calculateProgress()) * measuredHeight, measuredWidth.toFloat(), measuredHeight.toFloat())
        }
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawRoundRect(layoutRect, dpToPx(15).toFloat(), dpToPx(15).toFloat(), layoutPaint)
        canvas.drawRoundRect(progressRect, dpToPx(15).toFloat(), dpToPx(15).toFloat(), progressPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                return true
            }
            MotionEvent.ACTION_MOVE, MotionEvent.ACTION_UP -> {
                val y = event.y
                val currentHeight = measuredHeight - y
                val percent = currentHeight / measuredHeight.toFloat()
                progress = when {
                    percent >= 1 -> max
                    percent <= 0 -> 0
                    else -> (max * percent).toInt()
                }
                return true
            }
        }
        return false
    }

    private fun calculateProgress(): Float {
        return progress.toFloat() / max.toFloat()
    }

    interface OnSliderProgressChangeListener {
        fun onChanged(progress: Int, max: Int)
    }
}