package com.hmomeni.canto.utils

import android.content.Context
import android.content.res.Resources
import android.graphics.Point
import android.view.View
import android.view.WindowManager
import io.reactivex.Flowable
import io.reactivex.FlowableTransformer
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers


fun <T> schedulers(): FlowableTransformer<T, T> = FlowableTransformer {
    it.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
}

fun <T> Flowable<T>.iomain(): Flowable<T> = this.compose(schedulers())
fun <T> Single<T>.iomain(): Single<T> = this.compose {
    it.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
}


private var screenDimension: Dimension? = null

fun getScreenDimensions(context: Context): Dimension {
    if (screenDimension != null) {
        return screenDimension as Dimension
    }
    val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    val display = wm.defaultDisplay
    val size = Point()
    display.getSize(size)
    screenDimension = Dimension(size.x, size.y)
    return screenDimension as Dimension
}


fun getScaledDimension(imgSize: Dimension, boundary: Dimension,
                       inflate: Boolean): Dimension {
    if (imgSize.isEitherZero) {
        return imgSize
    }

    val originalWidth = imgSize.width
    val originalHeight = imgSize.height
    val boundWidth = boundary.width
    val boundHeight = boundary.height
    var newWidth = originalWidth
    var newHeight = originalHeight

    // first check if we need to scale width
    if (originalWidth > boundWidth || inflate && originalWidth < boundWidth) {
        //scale width to fit
        newWidth = boundWidth
        //scale height to maintain aspect ratio
        newHeight = newWidth * originalHeight / originalWidth
    }

    // then check if we need to scale even with the new height
    if (newHeight > boundHeight) {
        //scale height to fit instead
        newHeight = boundHeight
        //scale width to maintain aspect ratio
        newWidth = newHeight * originalWidth / originalHeight
    }

    return Dimension(newWidth, newHeight)
}

fun setViewDimension(view: View, dimension: Dimension) {
    val ll = view.layoutParams
    ll.width = dimension.width
    ll.height = dimension.height
    view.layoutParams = ll
}

fun dpToPx(dp: Int) = (dp * Resources.getSystem().displayMetrics.density).toInt()
