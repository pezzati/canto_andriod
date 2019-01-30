package com.hmomeni.canto.utils

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Point
import android.graphics.drawable.Drawable
import android.media.MediaMetadataRetriever
import android.provider.Settings
import android.view.View
import android.view.WindowManager
import androidx.core.content.ContextCompat
import com.bumptech.glide.RequestBuilder
import com.google.gson.JsonParser
import com.hmomeni.canto.App
import io.reactivex.*
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import jp.wasabeef.glide.transformations.RoundedCornersTransformation
import retrofit2.Response
import java.io.File


fun Application.app(): App {
    return this as App
}

fun Context.app(): App {
    return applicationContext as App
}

fun <T> schedulers(): FlowableTransformer<T, T> = FlowableTransformer {
    it.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
}

fun <T> Flowable<T>.iomain(): Flowable<T> = this.compose(schedulers())
fun <T> Single<T>.iomain(): Single<T> = this.compose {
    it.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
}

fun Completable.iomain(): Completable = this.compose {
    it.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
}

fun <T> Maybe<T>.iomain(): Maybe<T> = this.compose {
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

fun validatePhoneNumber(string: String) = string.matches(Regex("^(0?9|989)[0-9]{9}$"))

fun RequestBuilder<Drawable>.rounded(radius: Int, margin: Int = 3): RequestBuilder<Drawable> =
        this.apply(com.bumptech.glide.request.RequestOptions().transform(RoundedCornersTransformation(radius, margin)))

fun GlideRequest<Drawable>.rounded(radius: Int, margin: Int = 3): GlideRequest<Drawable> =
        this.apply(com.bumptech.glide.request.RequestOptions().transform(RoundedCornersTransformation(radius, margin)))

fun GlideRequest<Bitmap>.roundedBitmap(radius: Int, margin: Int = 3): GlideRequest<Bitmap> =
        this.apply(com.bumptech.glide.request.RequestOptions().transform(RoundedCornersTransformation(radius, margin)))

fun getDuration(filePath: String): Long {
    val mediaMetadataRetriever = MediaMetadataRetriever()
    mediaMetadataRetriever.setDataSource(filePath)
    val durationStr = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
    return durationStr.toLong()
}

fun getBitmapFromVectorDrawable(context: Context, drawableId: Int): Bitmap {
    val drawable = ContextCompat.getDrawable(context, drawableId)

    val bitmap = Bitmap.createBitmap(drawable!!.intrinsicWidth,
            drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)

    return bitmap
}

@SuppressLint("HardwareIds")
fun getDeviceId(context: Context): String = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)


fun installWatermark(context: Context): String {
    val outFile = File(context.filesDir, "watermark.png")
    if (outFile.exists()) {
        return outFile.absolutePath
    }
    val inputStream = context.assets.open("watermark.png")
    inputStream.use { i ->
        outFile.outputStream().use {
            i.copyTo(it)
        }
    }
    return outFile.absolutePath
}

fun Response<*>.errorString(): String? {
    val jo = JsonParser().parse(this.errorBody()?.string())
    return if (jo.isJsonNull) {
        null
    } else {
        jo.asJsonArray[0].asJsonObject["error"].asString
    }
}

fun isFFMpegAvailable(context: Context): Boolean {
    val ffmpeg = File(context.filesDir, "ffmpeg")
    return ffmpeg.exists()
}

fun View.gone() {
    this.visibility = View.GONE
}

fun View.visible() {
    this.visibility = View.VISIBLE
}