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
    private val bracketPaint = Paint().apply {
        color = Color.BLACK
        textSize = dpToPx(14).toFloat()
        isAntiAlias = true
        textAlignment = View.TEXT_ALIGNMENT_CENTER
    }
    private val mainLine = RectF()
    private val progressLine = RectF()
    private val leftAnchor = RectF()
    private val rightAnchor = RectF()
    private val mainLineHeight = dpToPx(8)
    val anchorWidth = dpToPx(24).toFloat()
    private val radius = dpToPx(5).toFloat()

    var progress = 0
        set(value) {
            if (value > 100) {
                throw RuntimeException("progress must not exceed 100")
            }
            field = value
//            progressLine.right = trimWidth * field / 100f
            requestLayout()
        }

    var trimWidth: Int = 80
        set(value) {
            field = value
            requestLayout()
        }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        setMeasuredDimension((trimWidth + anchorWidth * 2).toInt(), anchorWidth.toInt())
        if (measuredWidth > 0 && measuredHeight > 0) {
            mainLine.set(anchorWidth, (measuredHeight / 2f) - (mainLineHeight / 2), measuredWidth.toFloat() - anchorWidth, (measuredHeight / 2f) + (mainLineHeight / 2))
            progressLine.set(anchorWidth, (measuredHeight / 2f) - (mainLineHeight / 2), anchorWidth + (trimWidth * progress / 100f), (measuredHeight / 2f) + (mainLineHeight / 2))
            leftAnchor.set(0f, 0f, measuredHeight.toFloat(), measuredHeight.toFloat())
            rightAnchor.set((measuredWidth - measuredHeight).toFloat(), 0f, measuredWidth.toFloat(), measuredHeight.toFloat())
        }
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawRect(mainLine, whitePaint)
        canvas.drawRoundRect(leftAnchor, radius, radius, whitePaint)
        canvas.drawRoundRect(rightAnchor, radius, radius, whitePaint)
        canvas.drawRect(progressLine, greenPaint)
//        canvas.drawText("[", leftAnchor.centerX() - anchorWidth / 2, leftAnchor.centerY() + anchorWidth, bracketPaint)
//        canvas.drawText("]", rightAnchor.centerX() - anchorWidth / 2, rightAnchor.centerY() + anchorWidth, bracketPaint)
    }

}