package com.hmomeni.canto.fragments

import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.graphics.Matrix
import android.graphics.RectF
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.hardware.camera2.CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP
import android.hardware.camera2.CameraCharacteristics.SENSOR_ORIENTATION
import android.hardware.camera2.CameraDevice.TEMPLATE_PREVIEW
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentActivity
import android.support.v4.content.ContextCompat.checkSelfPermission
import android.support.v4.view.ViewPager
import android.util.Size
import android.view.*
import android.widget.Toast
import android.widget.Toast.LENGTH_SHORT
import com.crashlytics.android.Crashlytics
import com.example.android.camera2video.CompareSizesByArea
import com.hmomeni.canto.R
import com.hmomeni.canto.activities.DubsmashActivity
import com.hmomeni.canto.activities.KaraokeActivity
import com.hmomeni.canto.adapters.viewpager.ModePagerAdapter
import com.hmomeni.canto.api.Api
import com.hmomeni.canto.entities.PROJECT_TYPE_DUBSMASH
import com.hmomeni.canto.entities.PROJECT_TYPE_SINGING
import com.hmomeni.canto.utils.*
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.tmall.ultraviewpager.transformer.UltraScaleTransformer
import kotlinx.android.synthetic.main.fragment_recorder.*
import timber.log.Timber
import java.util.*
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import javax.inject.Inject


class RecorderFragment : Fragment() {
    private val FRAGMENT_DIALOG = "dialog"

    private var selectPostId: Int = 1085

    private val CAMERA_FRONT = "1"
    private val CAMERA_BACK = "0"

    @Inject
    lateinit var api: Api

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        context!!.app().di.inject(this)
        arguments?.let {
            selectPostId = it.getInt("post_id")
        }
    }

    /**
     * [TextureView.SurfaceTextureListener] handles several lifecycle events on a
     * [TextureView].
     */
    private val surfaceTextureListener = object : TextureView.SurfaceTextureListener {

        override fun onSurfaceTextureAvailable(texture: SurfaceTexture, width: Int, height: Int) {
            openCamera(width, height)
        }

        override fun onSurfaceTextureSizeChanged(texture: SurfaceTexture, width: Int, height: Int) {
            configureTransform(width, height)
        }

        override fun onSurfaceTextureDestroyed(surfaceTexture: SurfaceTexture) = true

        override fun onSurfaceTextureUpdated(surfaceTexture: SurfaceTexture) = Unit

    }

    /**
     * A reference to the opened [android.hardware.camera2.CameraDevice].
     */
    private var cameraDevice: CameraDevice? = null

    /**
     * A reference to the current [android.hardware.camera2.CameraCaptureSession] for
     * preview.
     */
    private var captureSession: CameraCaptureSession? = null

    /**
     * The [android.util.Size] of camera preview.
     */
    private lateinit var previewSize: Size

    /**
     * The [android.util.Size] of video recording.
     */
    private lateinit var videoSize: Size
    /**
     * An additional thread for running tasks that shouldn't block the UI.
     */
    private var backgroundThread: HandlerThread? = null

    /**
     * A [Handler] for running tasks in the background.
     */
    private var backgroundHandler: Handler? = null

    /**
     * A [Semaphore] to prevent the app from exiting before closing the camera.
     */
    private val cameraOpenCloseLock = Semaphore(1)

    /**
     * [CaptureRequest.Builder] for the camera preview
     */
    private lateinit var previewRequestBuilder: CaptureRequest.Builder

    /**
     * Orientation of the camera sensor
     */
    private var sensorOrientation = 0

    /**
     * [CameraDevice.StateCallback] is called when [CameraDevice] changes its status.
     */
    private val stateCallback = object : CameraDevice.StateCallback() {

        override fun onOpened(cameraDevice: CameraDevice) {
            cameraOpenCloseLock.release()
            this@RecorderFragment.cameraDevice = cameraDevice
            startPreview()
            configureTransform(textureView.width, textureView.height)
        }

        override fun onDisconnected(cameraDevice: CameraDevice) {
            cameraOpenCloseLock.release()
            cameraDevice.close()
            this@RecorderFragment.cameraDevice = null
        }

        override fun onError(cameraDevice: CameraDevice, error: Int) {
            cameraOpenCloseLock.release()
            cameraDevice.close()
            this@RecorderFragment.cameraDevice = null
            activity?.finish()
        }

    }

    private lateinit var textureView: TextureView
    private lateinit var textureView2: TextureView

    private var mediaRecorder: MediaRecorder? = null
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
            inflater.inflate(R.layout.fragment_recorder, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        textureView = TextureView(context!!)
        textureView2 = TextureView(context!!)

        val karaokeView = View(context!!).apply {
            setBackgroundColor(context!!.resources.getColor(R.color.md_green_400))
            layoutParams = ViewGroup.LayoutParams(dpToPx(200), dpToPx(365))
        }
        viewPager.setMultiScreen(0.6f)
        viewPager.setPageTransformer(false, UltraScaleTransformer())



        viewPager.adapter = ModePagerAdapter(context!!, arrayOf(textureView, textureView2, karaokeView))

        textureView.setOnClickListener {
            val loadingDialog = ProgressDialog(context)
            api.getSinglePost(postId = selectPostId)
                    .iomain()
                    .doOnSubscribe { loadingDialog.show() }
                    .doAfterTerminate { loadingDialog.dismiss() }
                    .subscribe({
                        startActivity(Intent(context, DubsmashActivity::class.java).putExtra("post", it).putExtra("type", PROJECT_TYPE_DUBSMASH))
                    }, {
                        Timber.e(it)
                    })
        }

        textureView2.setOnClickListener {
            val loadingDialog = ProgressDialog(context)
            api.getSinglePost(postId = selectPostId)
                    .iomain()
                    .doOnSubscribe { loadingDialog.show() }
                    .doAfterTerminate { loadingDialog.dismiss() }
                    .subscribe({
                        startActivity(Intent(context, DubsmashActivity::class.java).putExtra("post", it).putExtra("type", PROJECT_TYPE_SINGING))
                    }, {
                        Timber.e(it)
                    })
        }

        karaokeView.setOnClickListener {
            startActivity(Intent(context!!, KaraokeActivity::class.java))
        }

        nextTabTitle.alpha = 0f
        nextTabTitle.translationY = -100f

        nextTabDesc.alpha = 0f
        nextTabDesc.translationY = -100f
        var lastPage = 0
        viewPager.setOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrollStateChanged(state: Int) {

            }

            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
                if (positionOffset != 0f) {
                    when (position) {
                        0 -> {
                            currentTabTitle.alpha = 1 - positionOffset
                            currentTabTitle.translationY = 100 * positionOffset

                            currentTabDesc.alpha = 1 - positionOffset
                            currentTabDesc.translationY = 100 * positionOffset

                            nextTabTitle.alpha = positionOffset
                            nextTabTitle.translationY = -(100 - 100 * positionOffset)

                            nextTabDesc.alpha = positionOffset
                            nextTabDesc.translationY = -(100 - 100 * positionOffset)
                        }
                        1 -> {
                            nextTabTitle.alpha = 1 - positionOffset
                            nextTabTitle.translationY = 100 * positionOffset

                            nextTabDesc.alpha = 1 - positionOffset
                            nextTabDesc.translationY = 100 * positionOffset

                            currentTabTitle.alpha = positionOffset
                            currentTabTitle.translationY = -(100 - 100 * positionOffset)

                            currentTabDesc.alpha = positionOffset
                            currentTabDesc.translationY = -(100 - 100 * positionOffset)
                        }
                    }
                } else {
                    when (position) {
                        0 -> {
                            nextTabTitle.alpha = 0f
                            nextTabTitle.translationY = -100f

                            nextTabDesc.alpha = 0f
                            nextTabDesc.translationY = -100f

                            nextTabTitle.setText(R.string.sing)
                            nextTabTitle.setText(R.string.sing_desc)
                        }
                        1 -> {
                            currentTabTitle.alpha = 0f
                            currentTabTitle.translationY = -100f

                            currentTabDesc.alpha = 0f
                            currentTabDesc.translationY = -100f

                            if (lastPage == 0) {
                                currentTabTitle.setText(R.string.karaoke)
                                currentTabDesc.setText(R.string.karaoke_desc)
                            } else {
                                currentTabTitle.setText(R.string.dubsmash)
                                currentTabDesc.setText(R.string.dubsmash_desc)
                            }
                        }
                    }
                    lastPage = position
                }
            }

            override fun onPageSelected(position: Int) {

            }

        })
    }

    override fun onResume() {
        super.onResume()
        startBackgroundThread()

        // When the screen is turned off and turned back on, the SurfaceTexture is already
        // available, and "onSurfaceTextureAvailable" will not be called. In that case, we can open
        // a camera and start preview from here (otherwise, we wait until the surface is ready in
        // the SurfaceTextureListener).
        if (textureView.isAvailable) {
            openCamera(textureView.width, textureView.height)
        } else {
            textureView.surfaceTextureListener = surfaceTextureListener
        }
    }

    override fun onPause() {
        closeCamera()
        stopBackgroundThread()
        super.onPause()
    }

    /**
     * Starts a background thread and its [Handler].
     */
    private fun startBackgroundThread() {
        backgroundThread = HandlerThread("CameraBackground")
        backgroundThread?.start()
        backgroundHandler = Handler(backgroundThread?.looper)
    }

    /**
     * Stops the background thread and its [Handler].
     */
    private fun stopBackgroundThread() {
        backgroundThread?.quitSafely()
        try {
            backgroundThread?.join()
            backgroundThread = null
            backgroundHandler = null
        } catch (e: InterruptedException) {
            Timber.e(e)
        }
    }

    private fun hasPermissionsGranted(permissions: Array<String>) =
            permissions.none {
                checkSelfPermission((activity as FragmentActivity), it) != PERMISSION_GRANTED
            }

    private fun requestVideoPermissions() {
        Dexter.withActivity(activity)
                .withPermissions(VIDEO_PERMISSIONS)
                .withListener(object : MultiplePermissionsListener {
                    override fun onPermissionsChecked(report: MultiplePermissionsReport) {
                        if (report.grantedPermissionResponses.size == 3) {
                            openCamera(textureView.width, textureView.height)
                        }
                    }

                    override fun onPermissionRationaleShouldBeShown(permissions: MutableList<PermissionRequest>?, token: PermissionToken) {
                        token.continuePermissionRequest()
                    }
                }).check()

    }

    /**
     * Tries to open a [CameraDevice]. The result is listened by [stateCallback].
     *
     * Lint suppression - permission is checked in [hasPermissionsGranted]
     */
    @SuppressLint("MissingPermission")
    private fun openCamera(width: Int, height: Int) {
        if (!hasPermissionsGranted(VIDEO_PERMISSIONS.toTypedArray())) {
            requestVideoPermissions()
            return
        }
        val cameraActivity = activity
        if (cameraActivity == null || cameraActivity.isFinishing) return

        val manager = cameraActivity.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            if (!cameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                Timber.e("Acquiring camera lock failed, retrying openCamera ")
                Crashlytics.logException(CameraAccessException(CameraAccessException.CAMERA_ERROR, "Acquiring camera lock failed, retrying openCamera"))
                return
            }
            val cameraId = manager.cameraIdList.firstOrNull { it == CAMERA_FRONT } ?: CAMERA_BACK

            // Choose the sizes for camera preview and video recording
            val characteristics = manager.getCameraCharacteristics(cameraId)
            val map = characteristics.get(SCALER_STREAM_CONFIGURATION_MAP)
                    ?: throw RuntimeException("Cannot get available preview/video sizes")
            sensorOrientation = characteristics.get(SENSOR_ORIENTATION)
            videoSize = chooseVideoSize(map.getOutputSizes(MediaRecorder::class.java))
            previewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture::class.java),
                    width, height, videoSize)

            /*if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                textureView.setAspectRatio(previewSize.width, previewSize.height)
                textureView2.setAspectRatio(previewSize.width, previewSize.height)
            } else {
                textureView.setAspectRatio(previewSize.height, previewSize.width)
                textureView2.setAspectRatio(previewSize.height, previewSize.width)
            }*/
            configureTransform(width, height)
            mediaRecorder = MediaRecorder()
            manager.openCamera(cameraId, stateCallback, null)
        } catch (e: CameraAccessException) {
            showToast("Cannot access the camera.")
            cameraActivity.finish()
        } catch (e: NullPointerException) {
            // Currently an NPE is thrown when the Camera2API is used but not supported on the
            // device this code runs.
            ErrorDialog.newInstance(getString(R.string.camera_error))
                    .show(childFragmentManager, FRAGMENT_DIALOG)
        } catch (e: InterruptedException) {
            throw RuntimeException("Interrupted while trying to lock camera opening.")
        }
    }

    /**
     * Close the [CameraDevice].
     */
    private fun closeCamera() {
        try {
            cameraOpenCloseLock.acquire()
            closePreviewSession()
            cameraDevice?.close()
            cameraDevice = null
            mediaRecorder?.release()
            mediaRecorder = null
        } catch (e: InterruptedException) {
            throw RuntimeException("Interrupted while trying to lock camera closing.", e)
        } finally {
            cameraOpenCloseLock.release()
        }
    }

    /**
     * Start the camera preview.
     */
    private fun startPreview() {
        if (cameraDevice == null || !textureView.isAvailable) return

        try {
            closePreviewSession()
            val texture = textureView.surfaceTexture
            val texture2 = textureView2.surfaceTexture
            texture.setDefaultBufferSize(previewSize.width, previewSize.height)
            texture2.setDefaultBufferSize(previewSize.width, previewSize.height)
            previewRequestBuilder = cameraDevice!!.createCaptureRequest(TEMPLATE_PREVIEW)

            val previewSurface = Surface(texture)
            val previewSurface2 = Surface(texture2)
            previewRequestBuilder.addTarget(previewSurface)
            previewRequestBuilder.addTarget(previewSurface2)

            cameraDevice?.createCaptureSession(listOf(previewSurface, previewSurface2),
                    object : CameraCaptureSession.StateCallback() {

                        override fun onConfigured(session: CameraCaptureSession) {
                            captureSession = session
                            updatePreview()
                        }

                        override fun onConfigureFailed(session: CameraCaptureSession) {

                        }
                    }, backgroundHandler)
        } catch (e: CameraAccessException) {
            Timber.e(e)
        }

    }

    /**
     * Update the camera preview. [startPreview] needs to be called in advance.
     */
    private fun updatePreview() {
        if (cameraDevice == null) return

        try {
            setUpCaptureRequestBuilder(previewRequestBuilder)
            HandlerThread("CameraPreview").start()
            captureSession?.setRepeatingRequest(previewRequestBuilder.build(),
                    null, backgroundHandler)
        } catch (e: CameraAccessException) {
            Timber.e(e)
        }

    }

    private fun setUpCaptureRequestBuilder(builder: CaptureRequest.Builder?) {
        builder?.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO)
    }

    /**
     * Configures the necessary [android.graphics.Matrix] transformation to `textureView`.
     * This method should not to be called until the camera preview size is determined in
     * openCamera, or until the size of `textureView` is fixed.
     *
     * @param viewWidth  The width of `textureView`
     * @param viewHeight The height of `textureView`
     */
    private fun configureTransform(viewWidth: Int, viewHeight: Int) {
        activity ?: return
        val rotation = (activity as FragmentActivity).windowManager.defaultDisplay.rotation
        val matrix = Matrix()
        val viewRect = RectF(0f, 0f, viewWidth.toFloat(), viewHeight.toFloat())
        val bufferRect = RectF(0f, 0f, previewSize.height.toFloat(), previewSize.width.toFloat())
        val centerX = viewRect.centerX()
        val centerY = viewRect.centerY()

        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY())
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL)
            val scale = Math.max(
                    viewHeight.toFloat() / previewSize.height,
                    viewWidth.toFloat() / previewSize.width)
            with(matrix) {
                postScale(scale, scale, centerX, centerY)
                postRotate((90 * (rotation - 2)).toFloat(), centerX, centerY)
            }
        }
        if (viewRect.height() / viewRect.width() != bufferRect.height() / bufferRect.width()) {
            val df = viewRect.height() / bufferRect.height()

            val sx = bufferRect.width() * df / viewRect.width()
            val sy = bufferRect.height() * df / viewRect.height()

            matrix.setScale(sx, sy, viewRect.centerX(), viewRect.centerY())
        }

        textureView.setTransform(matrix)
        textureView2.setTransform(matrix)
    }

    private fun closePreviewSession() {
        captureSession?.close()
        captureSession = null
    }


    private fun showToast(message: String) = Toast.makeText(activity, message, LENGTH_SHORT).show()

    /**
     * In this sample, we choose a video size with 3x4 aspect ratio. Also, we don't use sizes
     * larger than 1080p, since MediaRecorder cannot handle such a high-resolution video.
     *
     * @param choices The list of available sizes
     * @return The video size
     */
    private fun chooseVideoSize(choices: Array<Size>) = choices.firstOrNull {
        it.width == it.height * 16 / 9
    } ?: choices[0]

    /**
     * Given [choices] of [Size]s supported by a camera, chooses the smallest one whose
     * width and height are at least as large as the respective requested values, and whose aspect
     * ratio matches with the specified value.
     *
     * @param choices     The list of sizes that the camera supports for the intended output class
     * @param width       The minimum desired width
     * @param height      The minimum desired height
     * @param aspectRatio The aspect ratio
     * @return The optimal [Size], or an arbitrary one if none were big enough
     */
    private fun chooseOptimalSize(
            choices: Array<Size>,
            width: Int,
            height: Int,
            aspectRatio: Size
    ): Size {

        // Collect the supported resolutions that are at least as big as the preview Surface
        val w = aspectRatio.width
        val h = aspectRatio.height
        val bigEnough = choices.filter {
            it.height == it.width * h / w && it.width >= width && it.height >= height
        }

        // Pick the smallest of those, assuming we found any
        return if (bigEnough.isNotEmpty()) {
            Collections.min(bigEnough, CompareSizesByArea())
        } else {
            choices[0]
        }
    }

}