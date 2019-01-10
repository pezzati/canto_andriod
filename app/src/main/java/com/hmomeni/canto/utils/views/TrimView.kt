package com.hmomeni.canto.utils.views

import android.content.Context
import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.hmomeni.canto.R
import com.hmomeni.canto.utils.getBitmapFromVectorDrawable
import timber.log.Timber

class TrimView : View {
    constructor(context: Context) : super(context)
    constructor(context: Context, attributeSet: AttributeSet) : super(context, attributeSet)

    private val bgPaint = Paint().apply {
        color = Color.parseColor("#99ffffff")
        isAntiAlias = true
        strokeCap = Paint.Cap.ROUND
    }
    private val whitePaint = Paint().apply {
        color = Color.WHITE
        isAntiAlias = true
    }
    private val progressPaint = Paint().apply {
        color = Color.parseColor("#007AFF")
        isAntiAlias = true
        strokeCap = Paint.Cap.ROUND
    }
    private val bracketPaint = Paint().apply {
        color = Color.WHITE
        textSize = dpToPx(14).toFloat()
        isAntiAlias = true
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
//            textAlignment = View.TEXT_ALIGNMENT_CENTER
//        }
    }
    private val anchorPaint = Paint().apply {
        color = Color.parseColor("#FB861F")
        isAntiAlias = true
    }

    private val leftBracket = getBitmapFromVectorDrawable(context, R.drawable.ic_bracket_left)
    private val rightBracket = getBitmapFromVectorDrawable(context, R.drawable.ic_bracket_right)
    private val bgLine = RectF()
    private val mainLine = RectF()
    private val progressLine = RectF()
    private val leftAnchor = RectF()
    private val rightAnchor = RectF()
    private val mainLineHeight = dpToPx(8)
    private val anchorWidth = dpToPx(24).toFloat()
    private val radius = dpToPx(5).toFloat()

    var onTrimChangeListener: TrimChangeListener? = null

    var max: Int = 100

    var progress = 0
        set(value) {
            field = value
            calculateProgress(true)
        }

    var trim: Int = max / 3
        set(value) {
            field = value
            calculateLeftandRight()
        }
    var trimStart: Int = 0
    var minTrim: Int = 0
    var maxTrim: Int = max

    private var maxPx = 0

    private val anchorCompensate = 20f

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        setMeasuredDimension(measuredWidth, anchorWidth.toInt())
        if (measuredWidth > 0 && measuredHeight > 0) {
            maxPx = (measuredWidth - (2 * anchorWidth)).toInt()
            bgLine.set(
                    anchorWidth,
                    0f,
                    anchorWidth + maxPx,
                    measuredHeight.toFloat()
            )

            mainLine.set(
                    -anchorCompensate,
                    0f,
                    -anchorCompensate,
                    measuredHeight.toFloat()
            )
            progressLine.set(
                    -anchorCompensate,
                    0f,
                    -anchorCompensate,
                    measuredHeight.toFloat()
            )
            leftAnchor.set(0f, 0f, 0f, measuredHeight.toFloat())
            rightAnchor.set(
                    0f,
                    0f,
                    0f,
                    measuredHeight.toFloat()
            )

            calculateLeftandRight(false)
            calculateProgress(false)
        }
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawRoundRect(bgLine, radius, radius, bgPaint)
        canvas.drawRect(mainLine, whitePaint)
        canvas.drawRect(progressLine, progressPaint)
        canvas.drawRoundRect(leftAnchor, radius, radius, anchorPaint)
        canvas.drawRoundRect(rightAnchor, radius, radius, anchorPaint)
        canvas.drawBitmap(leftBracket, null, leftAnchor, null)
        canvas.drawBitmap(rightBracket, null, rightAnchor, null)
    }

    private var captured: Captured = Captured.WHOLE

    private var initTrim = 0
    private var initTrimStart = 0
    private var initx = 0f
    private var initrx = 0f
    private var initlx = 0f

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                initTrim = trim
                initTrimStart = trimStart
                initx = event.x
                captured = when {
                    rightAnchor.contains(event.x, event.y) -> {
                        initrx = rightAnchor.left
                        Captured.RIGHT
                    }
                    leftAnchor.contains(event.x, event.y) -> {
                        initlx = leftAnchor.left
                        Captured.LEFT
                    }
                    else -> {
                        initrx = rightAnchor.left
                        initlx = leftAnchor.left
                        Captured.WHOLE
                    }
                }
                onTrimChangeListener?.onDragStarted(trimStart, trim)
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = event.x - initx
                when (captured) {
                    Captured.LEFT -> {
                        val newx = initlx + dx
                        val newTrimStart = initTrimStart + (dx * max / (maxPx)).toInt()
                        val newTrim = initTrim - newTrimStart + initTrimStart
                        if (
                                newTrim in minTrim..maxTrim
                                && newx >= 0
                        ) {
                            trimStart = newTrimStart
                            trim = newTrim
                            calculateLeftandRight()
                            onTrimChangeListener?.onLeftEdgeChanged(trimStart, trim)
                        }
                    }
                    Captured.RIGHT -> {
                        val newx = initrx + dx
                        val newTrim = initTrim + (dx * max / (maxPx)).toInt()
                        if (
                                newTrim in minTrim..maxTrim
                                && newx + anchorWidth <= measuredWidth
                        ) {
                            trim = newTrim
                            if (progress > trim) {
                                progress = trim
                            }
                            calculateLeftandRight()
                            onTrimChangeListener?.onRightEdgeChanged(trimStart, trim)
                        }
                    }
                    Captured.WHOLE -> {
                        if (initrx + dx + anchorWidth <= measuredWidth && initlx + dx >= 0) {
                            trimStart = initTrimStart + (dx * max / (measuredWidth - 2 * anchorWidth)).toInt()
                            calculateLeftandRight()
                            onTrimChangeListener?.onRangeChanged(trimStart, trim)
                        }
                    }
                }
            }
            MotionEvent.ACTION_UP -> {
                onTrimChangeListener?.onDragStopped(trimStart, trim)
            }
        }

        return true
    }

    private fun calculateLeftandRight(invalidate: Boolean = true) {
        val trimStartPx = trimStart * maxPx / max
        val trimPx = trim * maxPx / max

        leftAnchor.left = trimStartPx.toFloat()
        leftAnchor.right = leftAnchor.left + anchorWidth
        mainLine.left = leftAnchor.right - anchorCompensate

        rightAnchor.left = (trimStartPx + trimPx).toFloat() + anchorWidth
        rightAnchor.right = rightAnchor.left + anchorWidth
        mainLine.right = rightAnchor.left + anchorCompensate

        calculateProgress(false)

//        report()

        if (invalidate)
            invalidate()
    }

    private fun calculateProgress(invalidate: Boolean) {
        val progressPx = progress * maxPx / max
        progressLine.left = mainLine.left
        progressLine.right = progressLine.left + progressPx + anchorCompensate

        if (invalidate)
            invalidate()
    }

    private fun report() {
        Timber.d("trimStart=%d, trim=%d, progress=%d", trimStart, trim, progress)
    }

    enum class Captured {
        LEFT, RIGHT, WHOLE
    }

    abstract class TrimChangeListener {
        open fun onDragStarted(trimStart: Int, trim: Int) {}
        open fun onLeftEdgeChanged(trimStart: Int, trim: Int) {}
        open fun onRightEdgeChanged(trimStart: Int, trim: Int) {}
        open fun onRangeChanged(trimStart: Int, trim: Int) {}
        open fun onDragStopped(trimStart: Int, trim: Int) {}
    }

    private fun dpToPx(dp: Int) = (dp * Resources.getSystem().displayMetrics.density).toInt()

}