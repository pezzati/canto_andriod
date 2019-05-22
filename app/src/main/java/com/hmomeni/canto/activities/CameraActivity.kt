package com.hmomeni.canto.activities

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Matrix
import android.graphics.RectF
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.media.MediaRecorder
import android.os.Handler
import android.os.HandlerThread
import android.util.Range
import android.util.Size
import android.util.SparseIntArray
import android.view.Surface
import android.view.TextureView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.crashlytics.android.Crashlytics
import com.hmomeni.canto.R
import com.hmomeni.canto.utils.CompareSizesByArea
import com.hmomeni.canto.utils.ErrorDialog
import com.hmomeni.canto.utils.VIDEO_PERMISSIONS
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import kotlinx.android.synthetic.main.activity_dubsmash.*
import timber.log.Timber
import java.io.IOException
import java.util.*
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit

@SuppressLint("Registered")
abstract class CameraActivity : BaseFullActivity() {
    abstract fun getTextureView(): TextureView
    abstract fun onRecordStarted()
    abstract fun onRecordStopped()
    abstract fun onRecordError()
    abstract fun onTextureAvailable(width: Int, height: Int)

    private val CAMERA_FRONT = "1"
    private val CAMERA_BACK = "0"

    private var cameraId = CAMERA_FRONT

    private val FRAGMENT_DIALOG = "dialog"
    private val SENSOR_ORIENTATION_DEFAULT_DEGREES = 90
    private val SENSOR_ORIENTATION_INVERSE_DEGREES = 270
    private val DEFAULT_ORIENTATIONS = SparseIntArray().apply {
        append(Surface.ROTATION_0, 90)
        append(Surface.ROTATION_90, 0)
        append(Surface.ROTATION_180, 270)
        append(Surface.ROTATION_270, 180)
    }
    private val INVERSE_ORIENTATIONS = SparseIntArray().apply {
        append(Surface.ROTATION_0, 270)
        append(Surface.ROTATION_90, 180)
        append(Surface.ROTATION_180, 90)
        append(Surface.ROTATION_270, 0)
    }

    var ratio = 16f / 9f
        set(value) {
            field = value
            closeCamera()
            openCamera(textureView.width, textureView.height)
        }

    private val surfaceTextureListener = object : TextureView.SurfaceTextureListener {

        override fun onSurfaceTextureAvailable(texture: SurfaceTexture, width: Int, height: Int) {
            openCamera(width, height)
            onTextureAvailable(width, height)
        }

        override fun onSurfaceTextureSizeChanged(texture: SurfaceTexture, width: Int, height: Int) {
            configureTransform(width, height)
        }

        override fun onSurfaceTextureDestroyed(surfaceTexture: SurfaceTexture) = true

        override fun onSurfaceTextureUpdated(surfaceTexture: SurfaceTexture) = Unit

    }


    private var cameraDevice: CameraDevice? = null

    private var captureSession: CameraCaptureSession? = null

    private lateinit var previewSize: Size

    private lateinit var videoSize: Size

    protected var isRecordingVideo = false

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

    private lateinit var previewRequestBuilder: CaptureRequest.Builder


    private var sensorOrientation = 0

    /**
     * [CameraDevice.StateCallback] is called when [CameraDevice] changes its status.
     */
    private val stateCallback = object : CameraDevice.StateCallback() {

        override fun onOpened(cameraDevice: CameraDevice) {
            cameraOpenCloseLock.release()
            this@CameraActivity.cameraDevice = cameraDevice
            startPreview()
            configureTransform(getTextureView().width, getTextureView().height)
        }

        override fun onDisconnected(cameraDevice: CameraDevice) {
            cameraOpenCloseLock.release()
            cameraDevice.close()
            this@CameraActivity.cameraDevice = null
        }

        override fun onError(cameraDevice: CameraDevice, error: Int) {
            cameraOpenCloseLock.release()
            cameraDevice.close()
            this@CameraActivity.cameraDevice = null
            finish()
        }

    }

    private var nextVideoAbsolutePath: String? = null

    private var mediaRecorder: MediaRecorder? = null


    override fun onResume() {
        super.onResume()
        startBackgroundThread()

        // When the screen is turned off and turned back on, the SurfaceTexture is already
        // available, and "onSurfaceTextureAvailable" will not be called. In that case, we can open
        // a camera and start preview from here (otherwise, we wait until the surface is ready in
        // the SurfaceTextureListener).
        if (getTextureView().isAvailable) {
            openCamera(getTextureView().width, getTextureView().height)
        } else {
            getTextureView().surfaceTextureListener = surfaceTextureListener
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
                ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
            }

    private fun requestVideoPermissions() {
        Dexter.withActivity(this)
                .withPermissions(VIDEO_PERMISSIONS)
                .withListener(object : MultiplePermissionsListener {
                    override fun onPermissionsChecked(report: MultiplePermissionsReport) {
                        if (report.grantedPermissionResponses.size == 2) {
                            openCamera(getTextureView().width, getTextureView().height)
                        }
                    }

                    override fun onPermissionRationaleShouldBeShown(permissions: MutableList<PermissionRequest>?, token: PermissionToken) {
                        token.continuePermissionRequest()
                    }
                }).check()

    }

    protected fun switchCamera() {
        closeCamera()
        cameraId = if (cameraId == CAMERA_BACK) CAMERA_FRONT else CAMERA_BACK
        openCamera(getTextureView().width, getTextureView().height)
    }

    /**
     * Tries to open a [CameraDevice]. The result is listened by [stateCallback].
     *
     * Lint suppression - permission is checked in [hasPermissionsGranted]
     */
    @SuppressLint("MissingPermission")
    protected fun openCamera(w: Int, h: Int): Boolean {

        if (!hasPermissionsGranted(VIDEO_PERMISSIONS.toTypedArray())) {
            requestVideoPermissions()
            return false
        }
        val cameraActivity = this
        if (cameraActivity.isFinishing) return false

        val manager = cameraActivity.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            if (!cameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                Timber.e("Acquiring camera lock failed, retrying openCamera")
                Crashlytics.logException(CameraAccessException(CameraAccessException.CAMERA_ERROR, "Acquiring camera lock failed, retrying openCamera"))
                openCamera(h, w)
                return false
            }

            if (!manager.cameraIdList.contains(cameraId)) {
                return false
            }

            // Choose the sizes for camera preview and video recording
            val characteristics = manager.getCameraCharacteristics(cameraId)
            val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                    ?: throw RuntimeException("Cannot get available preview/video sizes")
            sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION)
            videoSize = chooseVideoSize(map.getOutputSizes(MediaRecorder::class.java))
            Timber.d("Selected Video Size=%s", videoSize)
            previewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture::class.java),
                    h, w, videoSize)
            Timber.d("Selected Preview Video Size=%s", previewSize)

            configureTransform(h, w)
            mediaRecorder = MediaRecorder()
            manager.openCamera(cameraId, stateCallback, null)
            return true
        } catch (e: CameraAccessException) {
            showToast("Cannot access the camera.")
            cameraActivity.finish()
            return false
        } catch (e: NullPointerException) {
            ErrorDialog.newInstance(getString(R.string.camera_error))
                    .show(supportFragmentManager, FRAGMENT_DIALOG)
            return false
        } catch (e: InterruptedException) {
            throw RuntimeException("Interrupted while trying to lock camera opening.")
        }

    }

    protected fun closeCamera() {
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

    private fun startPreview() {
        if (cameraDevice == null || !getTextureView().isAvailable) return

        try {
            closePreviewSession()
            val texture = getTextureView().surfaceTexture
            texture.setDefaultBufferSize(previewSize.width, previewSize.height)
            previewRequestBuilder = cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)

            previewRequestBuilder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, getRange())

            val previewSurface = Surface(texture)
            previewRequestBuilder.addTarget(previewSurface)

            cameraDevice?.createCaptureSession(listOf(previewSurface),
                    object : CameraCaptureSession.StateCallback() {

                        override fun onConfigured(session: CameraCaptureSession) {
                            captureSession = session
                            updatePreview()
                        }

                        override fun onConfigureFailed(session: CameraCaptureSession) {
                            Timber.e("Camera Capture Session configure failed")
                        }
                    }, backgroundHandler)
        } catch (e: CameraAccessException) {
            Timber.e(e)
            Crashlytics.logException(e)
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
        val rotation = windowManager.defaultDisplay.rotation
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

        getTextureView().setTransform(matrix)
    }

    @Throws(IOException::class)
    private fun setUpMediaRecorder() {
        val cameraActivity = this

        if (nextVideoAbsolutePath.isNullOrEmpty()) {
            nextVideoAbsolutePath = getVideoFilePath()
        }

        val rotation = cameraActivity.windowManager.defaultDisplay.rotation
        when (sensorOrientation) {
            SENSOR_ORIENTATION_DEFAULT_DEGREES ->
                mediaRecorder?.setOrientationHint(DEFAULT_ORIENTATIONS.get(rotation))
            SENSOR_ORIENTATION_INVERSE_DEGREES ->
                mediaRecorder?.setOrientationHint(INVERSE_ORIENTATIONS.get(rotation))
        }

        mediaRecorder?.apply {
            //            setAudioSource(MediaRecorder.AudioSource.MIC)
            setVideoSource(MediaRecorder.VideoSource.SURFACE)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setOutputFile(nextVideoAbsolutePath)
            setVideoEncodingBitRate(10000000)
            setVideoFrameRate(60)
            setVideoSize(videoSize.width, videoSize.height)
            setVideoEncoder(MediaRecorder.VideoEncoder.H264)
//            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOnErrorListener { _, what, extra ->
                Timber.e("MediaRecorder Error, what: %d, extra: %d", what, extra)
                Crashlytics.logException(Exception("MediaRecorder Error what: $what, extra: $extra"))
            }
            prepare()
        }
    }

    abstract fun getVideoFilePath(): String

    protected fun startRecordingVideo() {
        if (cameraDevice == null || !getTextureView().isAvailable) return

        try {
            closePreviewSession()
            setUpMediaRecorder()
            val texture = getTextureView().surfaceTexture.apply {
                setDefaultBufferSize(previewSize.width, previewSize.height)
            }

            // Set up Surface for camera preview and MediaRecorder
            val previewSurface = Surface(texture)
            val recorderSurface = mediaRecorder!!.surface
            val surfaces = ArrayList<Surface>().apply {
                add(previewSurface)
                add(recorderSurface)
            }
            previewRequestBuilder = cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_RECORD).apply {
                addTarget(previewSurface)
                addTarget(recorderSurface)
            }
            previewRequestBuilder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, getRange())

            // Start a capture session
            // Once the session starts, we can update the UI and start recording
            cameraDevice?.createCaptureSession(surfaces,
                    object : CameraCaptureSession.StateCallback() {

                        override fun onConfigured(cameraCaptureSession: CameraCaptureSession) {
                            Timber.d("Camera Capture Session Configured")
                            captureSession = cameraCaptureSession
                            updatePreview()
                            runOnUiThread {
                                isRecordingVideo = true
                                mediaRecorder?.start()
                                onRecordStarted()
                            }
                        }

                        override fun onConfigureFailed(cameraCaptureSession: CameraCaptureSession) {
                            showToast("Failed")
                            onRecordError()
                        }
                    }, backgroundHandler)
        } catch (e: CameraAccessException) {
            Timber.e(e)
            onRecordError()
        } catch (e: IOException) {
            Timber.e(e)
            onRecordError()
        }

    }

    private fun closePreviewSession() {
        captureSession?.close()
        captureSession = null
    }

    protected fun stopRecordingVideo() {
        try {
            isRecordingVideo = false
            onRecordStopped()
            mediaRecorder?.stop()
            mediaRecorder?.reset()
            nextVideoAbsolutePath = null
        } catch (e: Exception) {
            Timber.e(e)
            Crashlytics.logException(e)
        }
    }

    private fun showToast(message: String) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

    /**
     * In this sample, we choose a video size with 3x4 aspect ratio. Also, we don't use sizes
     * larger than 1080p, since MediaRecorder cannot handle such a high-resolution video.
     *
     * @param choices The list of available sizes
     * @return The video size
     */
    private fun chooseVideoSize(choices: Array<Size>) = choices.filter {
        it.width <= 1080 || it.height <= 1080
    }.firstOrNull {
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

    private fun getRange(): Range<Int>? {

        val manager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val chars = manager.getCameraCharacteristics(cameraId)
        val ranges = chars.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES)!!

        var result: Range<Int>? = null

        for (range in ranges) {
            val upper = range.upper

            // 10 - min range upper for my needs
            if (upper >= 10) {
                if (result == null || upper < result.upper) {
                    result = range
                }
            }
        }

        if (result == null) {
            result = ranges[0]
        }

        return result
    }

}