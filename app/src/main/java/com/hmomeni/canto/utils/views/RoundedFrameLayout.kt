package com.hmomeni.canto.utils.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.widget.FrameLayout
import com.hmomeni.canto.utils.dpToPx

class RoundedFrameLayout : FrameLayout {
    constructor(context: Context) : super(context)
    constructor(context: Context, attributeSet: AttributeSet) : super(context, attributeSet)

    val path = Path()
    val roudRect = RectF()

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        if (measuredWidth > 0 && measuredHeight > 0) {
        }
    }

    override fun dispatchDraw(canvas: Canvas) {

        canvas.clipPath(Path().apply {
            addRoundRect(RectF(0f, 0f, measuredWidth.toFloat() * 5 / 7, measuredHeight.toFloat()), dpToPx(15).toFloat(), dpToPx(15).toFloat(), Path.Direction.CW)
        })
        super.dispatchDraw(canvas)
    }
}