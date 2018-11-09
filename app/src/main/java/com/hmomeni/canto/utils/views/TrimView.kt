package com.hmomeni.canto.utils.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import com.hmomeni.canto.utils.dpToPx

class TrimView : View {
    constructor(context: Context) : super(context)
    constructor(context: Context, attributeSet: AttributeSet) : super(context, attributeSet)

    private val whitePaint = Paint().apply {
        color = Color.WHITE
        isAntiAlias = true
    }
    private val greenPaint = Paint().apply {
        color = Color.GREEN
        isAntiAlias = true
        strokeCap = Paint.Cap.ROUND
    }
    private val mainLine = RectF()
    private val progressLine = RectF()
    private val leftAnchor = RectF()
    private val rightAnchor = RectF()
    private val mainLineHeight = dpToPx(8)
    private val anchorWidth = dpToPx(5).toFloat()

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        if (measuredWidth > 0 && measuredHeight > 0) {
            mainLine.set(0f, (measuredHeight / 2f) - (mainLineHeight / 2), measuredWidth.toFloat(), (measuredHeight / 2f) + (mainLineHeight / 2))
            progressLine.set(measuredWidth / 4f, (measuredHeight / 2f) - (mainLineHeight / 2), measuredWidth * 3 / 4f, (measuredHeight / 2f) + (mainLineHeight / 2))
            leftAnchor.set(0f, 0f, measuredHeight.toFloat(), measuredHeight.toFloat())
            rightAnchor.set((measuredWidth - measuredHeight).toFloat(), 0f, measuredWidth.toFloat(), measuredHeight.toFloat())
        }
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawRect(mainLine, whitePaint)
        canvas.drawRoundRect(leftAnchor, anchorWidth, anchorWidth, whitePaint)
        canvas.drawRoundRect(rightAnchor, anchorWidth, anchorWidth, whitePaint)
        canvas.drawRect(progressLine, greenPaint)
    }

}