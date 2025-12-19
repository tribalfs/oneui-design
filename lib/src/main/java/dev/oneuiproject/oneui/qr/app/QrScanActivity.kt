package dev.oneuiproject.oneui.qr.app

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.OrientationEventListener
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.CallSuper
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.os.BundleCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.oneui.floatingdock.util.doOnGlobalLayout
import com.google.mlkit.vision.common.InputImage
import dev.oneuiproject.oneui.design.R
import dev.oneuiproject.oneui.ktx.isInMultiWindowModeCompat
import dev.oneuiproject.oneui.ktx.semToast
import dev.oneuiproject.oneui.qr.app.QrScanActivity.Companion.EXTRA_QR_REGEX
import dev.oneuiproject.oneui.qr.app.QrScanActivity.Companion.EXTRA_QR_REQUIRED_PREFIX
import dev.oneuiproject.oneui.qr.app.QrScanActivity.Companion.EXTRA_QR_SCANNER_RESULT
import dev.oneuiproject.oneui.qr.app.QrScanActivity.Companion.createIntent
import dev.oneuiproject.oneui.qr.app.internal.CameraResolutionHelper.createQrResolutionSelector
import dev.oneuiproject.oneui.qr.app.internal.CropOverlayView
import dev.oneuiproject.oneui.qr.app.internal.OrientationHysteresis
import dev.oneuiproject.oneui.qr.app.internal.observeOnce
import dev.oneuiproject.oneui.qr.widget.QrCodeScannerView
import dev.oneuiproject.oneui.utils.applyEdgeToEdge
import dev.oneuiproject.oneui.utils.darkSystemBars
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.Executors

/**
 * OneUI-styled full-screen QR code scanner built on top of CameraX and ML Kit.
 *
 * This activity:
 *
 * - Manages camera permission and binds a back-camera [Preview] and [ImageAnalysis] use case.
 * - Streams camera frames into [QrCodeScannerView] for real-time QR detection.
 * - Supports scanning QR codes from both:
 *   - the live camera feed, and
 *   - images picked from the gallery.
 * - Provides visual feedback for the scanning ROI (Region of Interest), detection,
 *   and error states via [QrCodeScannerView].
 * - Returns the accepted QR content to the caller via `setResult(RESULT_OK, …)`.
 *
 * The actual decoding is performed by ML Kit’s barcode scanning APIs and wrapped inside
 * [QrCodeScannerView]. This activity wires that into a OneUI-style UX (immersive full-screen,
 * flash / gallery buttons, animations, and result propagation).
 *
 * ## Result
 *
 * When a QR code passes validation, the decoded string is returned to the caller:
 *
 * - `resultCode` → `RESULT_OK`
 * - `Intent` extra → [EXTRA_QR_SCANNER_RESULT]
 *
 * ## Validation
 *
 * Basic validation can be configured via intent extras. If the scanned content does not satisfy
 * these constraints, [QrCodeScannerView] will show its “not matched” error UI and continue scanning.
 *
 * Supported validation extras:
 *
 * - [EXTRA_QR_REQUIRED_PREFIX] (optional)
 *   If set, the QR content must start with this prefix (e.g. `"WIFI:"`, `"https://"`).
 *
 * - [EXTRA_QR_REGEX] (optional)
 *   If set, the QR content must fully match the given regular expression.
 *
 * Both checks are evaluated in [onQRDetected]. Returning `true` from that callback indicates that
 * the content is accepted and the activity should finish; returning `false` lets the view handle
 * the “not matched” state and continue scanning.
 *
 * If your validation logic requires more than `prefix` and `regex`, you can extend this activity
 * instead and override [onQRDetected] to implement your own validation.
 *
 * ## Gallery integration
 *
 * Tapping the gallery button:
 *
 * - temporarily pauses camera analysis in [QrCodeScannerView],
 * - launches a system picker via [Intent.ACTION_PICK],
 * - on successful selection, passes the chosen image as [InputImage] back into
 *   [QrCodeScannerView] for offline QR detection.
 *
 * If the user cancels the picker, analysis is resumed.
 *
 * ## Usage
 *
 * Typical usage is via the Activity Result API and the provided [createIntent] helper,
 * or via the provided custom [QrScanContract] that wraps this activity.
 *
 * Example:
 *
 * ```kotlin
 * val launcher = registerForActivityResult(QrScanContract()) { result ->
 *     if (result != null) {
 *         // handle scanned content
 *     }
 * }
 *
 * launcher.launch(
 *     QrScanConfig(
 *         title = "Scan Wi-Fi QR",
 *         requiredPrefix = "WIFI:",
 *         regex = "^WIFI:.*$"
 *     )
 * )
 * ```
 *
 * You can also start this activity manually using [createIntent] and handle the result
 * via `onActivityResult` or your own Activity Result contract.
 *
 * ## Manifest requirements
 *
 * Applications using this activity **must** declare the following in their
 * `AndroidManifest.xml`:
 *
 * ```xml
 * <uses-permission android:name="android.permission.CAMERA" />
 * <!-- Only required if minSdk is < API24-->
 * <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
 *
 * ```
 *
 * The camera permission is required for live QR scanning via CameraX.
 * The camera hardware feature is marked as required because this activity
 * does not provide a camera-less fallback mode.
 *
 * Failure to declare these entries will result in runtime failures or the
 * activity being unavailable on compatible devices.
 */
open class QrScanActivity : AppCompatActivity(), QrCodeScannerView.Listener {

    private val cameraExecutor = Executors.newSingleThreadExecutor()
    private var camera: Camera? = null
    private var orientationListener: OrientationEventListener? = null
    private val orientationHysteresis = OrientationHysteresis()

    private var isBottomCropMenuInit = false
    private var isStaticImageMode = false

    private lateinit var mainLayout: FrameLayout
    private lateinit var qrCodeScannerView: QrCodeScannerView
    private lateinit var previewView: PreviewView
    private lateinit var cropBottomMenu: BottomNavigationView
    private lateinit var cropOverlayView: CropOverlayView

    private var currentImageUri: Uri? = null
    private var restoreNormRect: CropOverlayView.NormRect? = null
    private var shouldRestoreStaticMode: Boolean = false
    private var currentCroppedFile: File? = null
    private var isDecodePending = false

    private val cancelCropOnBackPressed by lazy(LazyThreadSafetyMode.NONE) {
        onBackPressedDispatcher.addCallback(this, false) { cancelCrop() }
    }

    private var isCroppingActive: Boolean = false
        set(value) {
            if (field == value) return
            field = value
            cancelCropOnBackPressed.isEnabled = value
        }

    private val requestCameraPermLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) startCamera()
            else {
                semToast(getString(R.string.oui_des_camera_perm_not_granted))
                qrCodeScannerView.initialize(
                    intent.getStringExtra(EXTRA_QR_SCANNER_TITLE),
                    false,
                    this@QrScanActivity
                )
            }
        }

    private val requestStoragePermLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                currentImageUri?.let { showPickedImage(it) }
            } else {
                currentImageUri = null
                exitStaticImageMode()
                semToast(getString(R.string.oui_des_storage_perm_not_granted))
            }
        }

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val uri = result.data?.data
            if (result.resultCode != RESULT_OK || uri == null) {
                exitStaticImageMode()
                qrCodeScannerView.pauseAnalysis(false)
                return@registerForActivityResult
            }

            currentImageUri = uri
            restoreNormRect = null

            // Pre-24 pickers may return MediaStore URIs which require
            // READ_EXTERNAL_STORAGE. From API 24 onward, pickers reliably
            // grant per-URI read access, making storage permission unnecessary
            // for reading the returned content URI.
            if (Build.VERSION.SDK_INT <= 23 &&
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
               requestStoragePermLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            } else {
               showPickedImage(uri)
            }
        }

    private val resolutionSelector by lazy(LazyThreadSafetyMode.NONE) {
        createQrResolutionSelector(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        applyEdgeToEdge { darkSystemBars() }
        hideStatusBar()
        super.onCreate(savedInstanceState)

        inflateViews()
        setContentView(mainLayout)

        ViewCompat.setOnApplyWindowInsetsListener(cropBottomMenu) { v, insets ->
            val systemBarsInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updatePadding(bottom = systemBarsInsets.bottom)
            insets
        }

        // This updates `shouldRestoreStaticMode`
        restoreInstanceState(savedInstanceState)

        if (shouldRestoreStaticMode) {
            forceHideLiveUiImmediately()
            enterStaticImageMode()
        } else {
            exitStaticImageMode()
        }

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            startCamera()
        } else {
            requestCameraPermLauncher.launch(Manifest.permission.CAMERA)
        }

        if (shouldRestoreStaticMode) {
            if (currentCroppedFile != null && currentCroppedFile!!.exists()) {
                showFinalCroppedImage(currentCroppedFile!!)
            }
            else if (currentImageUri != null) {
                enterStaticImageMode()
                showPickedImage(currentImageUri!!)
            }
        }
    }

    private fun hideStatusBar() {
        WindowCompat.getInsetsController(window, window.decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.statusBars())
        }
    }

    private fun inflateViews() {
        mainLayout = layoutInflater.inflate(R.layout.oui_des_qr_scanner_activity, null) as FrameLayout
        previewView = PreviewView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            isClickable = false
        }

        qrCodeScannerView = QrCodeScannerView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            clipChildren = false
            layoutDirection = View.LAYOUT_DIRECTION_LOCALE
        }

        cropOverlayView = CropOverlayView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            visibility = View.GONE
        }

        cropBottomMenu = BottomNavigationView(
            this,
            null,
            0,
            R.style.OneUI_QRScannerBottomNavigationView
        ).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            }
            visibility = View.GONE
            inflateMenu(R.menu.oui_des_menu_qr_crop)
        }

        val scannerFrame = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0
            ).apply { weight = 1f }
        }
        scannerFrame.apply {
            addView(qrCodeScannerView, 0)
            addView(cropOverlayView, 1)
        }

        val scannerOverlay = LinearLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            orientation = LinearLayout.VERTICAL
        }

        scannerOverlay.addView(scannerFrame, 0)
        scannerOverlay.addView(cropBottomMenu, 1)

        mainLayout.addView(previewView, 0)
        mainLayout.addView(scannerOverlay, 1)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (isStaticImageMode) {
            cropOverlayView.doOnGlobalLayout {
                val bounds = qrCodeScannerView.getDecodedImageBounds()
                cropOverlayView.setImageBounds(bounds, null)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (isInMultiWindowModeCompat) {
            semToast(getString(R.string.oui_des_not_supported_feature_in_multiwindow))
            finishAfterTransition()
        }
    }

    override fun onStart() {
        super.onStart()
        orientationListener?.let {
            if (it.canDetectOrientation()) it.enable()
        }
    }

    override fun onStop() {
        super.onStop()
        orientationListener?.disable()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        camera = null
        ProcessCameraProvider.getInstance(this).get().unbindAll()
    }

    // ------------------------------------------------------------------------------------
    // Save / Restore
    // ------------------------------------------------------------------------------------

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putBoolean(KEY_STATIC_MODE, isStaticImageMode)
        outState.putBoolean(KEY_ERROR_DIALOG_SHOWING, qrCodeScannerView.isErrorDialogShowing())
        outState.putBoolean(
            KEY_NOT_MATCHED_DIALOG_SHOWING,
            qrCodeScannerView.isNotMatchedDialogShowing()
        )
        outState.putBoolean(KEY_DECODE_PENDING, isDecodePending)

        currentImageUri?.let { outState.putParcelable(KEY_IMAGE_URI, it) }

        cropOverlayView.getCropRectNormalized()?.let { n ->
            outState.putFloatArray(KEY_CROP_NORM, floatArrayOf(n.l, n.t, n.r, n.b))
        }

        currentCroppedFile?.let {
            outState.putString(KEY_CROPPED_FILE_PATH, it.absolutePath)
        }
    }

    private fun restoreInstanceState(state: Bundle?) {
        if (state == null) return

        shouldRestoreStaticMode = state.getBoolean(KEY_STATIC_MODE, false)
        currentImageUri = BundleCompat.getParcelable(state, KEY_IMAGE_URI, Uri::class.java)

        state.getFloatArray(KEY_CROP_NORM)?.let { arr ->
            if (arr.size == 4) {
                restoreNormRect = CropOverlayView.NormRect(arr[0], arr[1], arr[2], arr[3])
            }
        }

        state.getString(KEY_CROPPED_FILE_PATH)?.let { path ->
            currentCroppedFile = File(path)
        }

        state.getBoolean(KEY_ERROR_DIALOG_SHOWING).let {
            if (it) qrCodeScannerView.showDetectErrorDialog()
        }

        state.getBoolean(KEY_NOT_MATCHED_DIALOG_SHOWING).let {
            if (it) qrCodeScannerView.showNotMatchedRequestedScanTypeErrorDialog()
        }

        isDecodePending = state.getBoolean(KEY_DECODE_PENDING)
    }

    // ------------------------------------------------------------------------------------
    // Orientation
    // ------------------------------------------------------------------------------------

    private fun initOrientationListener() {
        orientationListener = object : OrientationEventListener(this) {
            override fun onOrientationChanged(orientation: Int) {
                if (isStaticImageMode) return
                val snappedRotation = orientationHysteresis.update(orientation)
                snappedRotation?.let { qrCodeScannerView.updateOrientation(it) }
            }
        }
    }

    private fun lockToPortrait() {
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }

    private fun allowOrientationChange() {
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR
    }

    private fun enterStaticImageMode() {
        isStaticImageMode = true
        if (isRequestOrientationAllowed()) {
            orientationListener?.disable()
            allowOrientationChange()
        }
    }

    private fun exitStaticImageMode() {
        isStaticImageMode = false
        currentCroppedFile = null
        currentImageUri = null
        if (isRequestOrientationAllowed()) {
            lockToPortrait()
            if (orientationListener == null) {
                initOrientationListener()
            }
            orientationListener!!.let {
                if (it.canDetectOrientation()) it.enable()
            }
        }
    }

    // ------------------------------------------------------------------------------------
    // Camera
    // ------------------------------------------------------------------------------------

    private fun startCamera() {
        ProcessCameraProvider.getInstance(this).apply {
            addListener({
                val cameraProvider = get()
                cameraProvider.unbindAll()

                val preview = Preview.Builder()
                    .setResolutionSelector(resolutionSelector)
                    .build()
                    .also { it.setSurfaceProvider(previewView.surfaceProvider) }

                val analysis = ImageAnalysis.Builder()
                    .setResolutionSelector(resolutionSelector)
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also { it.setAnalyzer(cameraExecutor, qrCodeScannerView.imageAnalyzer) }

                camera = cameraProvider.bindToLifecycle(
                    this@QrScanActivity,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    analysis
                )

                qrCodeScannerView.initialize(
                    intent.getStringExtra(EXTRA_QR_SCANNER_TITLE),
                    camera!!.cameraInfo.hasFlashUnit(),
                    this@QrScanActivity
                )

                previewView.previewStreamState.observeOnce(this@QrScanActivity) {
                    if (it == PreviewView.StreamState.STREAMING) {
                        // In static mode we do NOT want the live UI showing.
                        if (!isStaticImageMode) {
                            qrCodeScannerView.startQrRoiAnimation()
                        }
                        true
                    } else false
                }

            }, ContextCompat.getMainExecutor(this@QrScanActivity))
        }
    }

    // ------------------------------------------------------------------------------------
    // Gallery -> static image
    // ------------------------------------------------------------------------------------

    private fun initCropBottomMenu() {
        if (isBottomCropMenuInit) return
        isBottomCropMenuInit = true

        cropBottomMenu.setOnItemSelectedListener { menuItem ->
            cropBottomMenu.isVisible = false
            when (menuItem.itemId) {
                R.id.menu_item_crop_ok -> confirmCrop()
                R.id.menu_item_crop_cancel -> cancelCrop()
            }
            true
        }
    }

    private fun showPickedImage(uri: Uri) {
        try {
            isCroppingActive = true
            forceHideLiveUiImmediately()

            val image = InputImage.fromFilePath(this, uri)
            qrCodeScannerView.setInputImage(image.bitmapInternal!!, image.rotationDegrees, false)

            cropBottomMenu.isVisible = true
            cropOverlayView.isVisible = true
            initCropBottomMenu()

            qrCodeScannerView.postDelayed( {
                val bounds = qrCodeScannerView.getDecodedImageBounds()
                if (!bounds.isEmpty) {
                    cropOverlayView.setImageBounds(bounds, restoreNormRect)
                } else {
                    // Fallback: keep overlay hidden if we can't determine bounds yet
                    cropOverlayView.isVisible = false
                }
            }, 300)

        } catch (e: Exception) {
            e.message?.let { semToast(it) }
            qrCodeScannerView.pauseAnalysis(false)
            isCroppingActive = false
        }
    }

    private fun showFinalCroppedImage(file: File) {
        enterStaticImageMode()
        forceHideLiveUiImmediately()
        val bitmap = BitmapFactory.decodeFile(file.absolutePath)
        if (bitmap != null) {
            qrCodeScannerView.showDecodedImage(bitmap, isDecodePending)
            qrCodeScannerView.showBlackBackground()
            if (isDecodePending) {
                qrCodeScannerView.updateToDecodedImageLayout()
                qrCodeScannerView.startQrRoiAnimation()
            }
        }
    }

    private fun confirmCrop() {
        val cropRect = cropOverlayView.getCropRect()
        // After confirm, we no longer need the overlay;
        // clear visible UI.
        cropOverlayView.isVisible = false
        cropBottomMenu.isVisible = false
        isDecodePending = true

        qrCodeScannerView.cropAndDecode(cropRect) {
            qrCodeScannerView.getDecodedImage()?.let {
                // Ensure this survives Activity destruction
                @OptIn(DelicateCoroutinesApi::class)
                GlobalScope.launch(Dispatchers.IO) {
                    try {
                        val fileName: String
                        val format: Bitmap.CompressFormat
                        if (Build.VERSION.SDK_INT >= 30) {
                            fileName = "cropped_qr_${System.currentTimeMillis()}.webp"
                            format = Bitmap.CompressFormat.WEBP_LOSSLESS
                        } else {
                            fileName = "cropped_qr_${System.currentTimeMillis()}.jpg"
                            format = Bitmap.CompressFormat.JPEG
                        }
                        currentCroppedFile = File(cacheDir, fileName).apply {
                            FileOutputStream(this).use { out -> it.compress(format, 100, out) }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }

        restoreNormRect = null
        currentImageUri = null
        cropOverlayView.reset()
    }

    private fun cancelCrop() {
        // Always go back to picker when cancelling
        onGalleryBtnClicked()

        restoreNormRect = null
        currentImageUri = null

        cropOverlayView.isVisible = false
        cropBottomMenu.isVisible = false

        qrCodeScannerView.resetDecodedImage()
        cropOverlayView.reset()
        isCroppingActive = false
    }

    // ------------------------------------------------------------------------------------
    // QrCodeScannerView.Listener
    // ------------------------------------------------------------------------------------

    override fun isAllowClick(): Boolean = true

    @CallSuper
    override fun onFlashBtnClicked(isActivated: Boolean) {
        camera?.cameraControl?.enableTorch(isActivated)
    }

    @CallSuper
    override fun onGalleryBtnClicked() {
        qrCodeScannerView.pauseAnalysis(true)
        val pickIntent = Intent(Intent.ACTION_PICK).apply { type = "image/*" }
        pickImageLauncher.launch(pickIntent)
    }

    @CallSuper
    override fun onStaticImageModeChanged(isStaticImageMode: Boolean) {
        if (isStaticImageMode) enterStaticImageMode() else exitStaticImageMode()
    }

    override fun onQRDetected(content: String): Boolean {
        isDecodePending = false
        isCroppingActive = false
        val requiredPrefix = intent.getStringExtra(EXTRA_QR_REQUIRED_PREFIX)
        val regexPattern = intent.getStringExtra(EXTRA_QR_REGEX)

        if (!requiredPrefix.isNullOrEmpty() && !content.startsWith(requiredPrefix)) return false

        if (!regexPattern.isNullOrEmpty()) {
            val regex = runCatching { Regex(regexPattern) }.getOrNull()
            if (regex != null && !regex.matches(content)) return false
        }

        setResultAndFinish(content)
        return true
    }

    @CallSuper
    override fun onInputImageError() {
        isDecodePending = false
        isCroppingActive = false
    }

    private fun isRequestOrientationAllowed(): Boolean =
        resources.configuration.smallestScreenWidthDp < 600 || applicationInfo.targetSdkVersion < 36

    private fun setResultAndFinish(text: String) {
        setResult(RESULT_OK, Intent().apply {
            putExtra(EXTRA_QR_SCANNER_RESULT, text)
        })
        finish()
    }

    // ------------------------------------------------------------------------------------
    // Helpers: hide live UI immediately (avoid one-frame flash)
    // ------------------------------------------------------------------------------------

    private fun forceHideLiveUiImmediately() {
        qrCodeScannerView.hideDefaultViewsImmediate()
        // Avoid showing crop UI until we’re ready
        cropBottomMenu.isVisible = false
        cropOverlayView.isVisible = false
    }

    companion object {
        private const val KEY_STATIC_MODE = "qr_static_mode"
        private const val KEY_ERROR_DIALOG_SHOWING = "qr_error_dialog_showing"
        private const val KEY_NOT_MATCHED_DIALOG_SHOWING = "qr_not_matched_dialog_showing"
        private const val KEY_IMAGE_URI = "qr_crop_image_uri"
        private const val KEY_CROP_NORM = "qr_crop_norm"
        private const val KEY_CROPPED_FILE_PATH = "qr_cropped_file_path"
        private const val KEY_DECODE_PENDING = "qr_decode_pending"

        private const val TAG = "QrScanActivity"

        /**
         * Intent extra key for specifying a custom title displayed at the top of
         * the QR scanner UI.
         *
         * If not provided, a default localized title (for example, “Scan QR code”)
         * is shown.
         *
         * Type: [String]
         */
        const val EXTRA_QR_SCANNER_TITLE = "title"

        /**
         * Intent extra key that defines a required prefix for accepted QR content.
         *
         * When set, only QR codes whose decoded string starts with this prefix
         * are considered valid.
         *
         * Examples:
         * - `"WIFI:"` to accept only Wi-Fi configuration QR codes
         * - `"https://"` to accept only secure URLs
         *
         * If the scanned content does not start with this prefix, the scan is
         * treated as a “not matched” result and the scanner continues running.
         *
         * Type: [String]
         */
        const val EXTRA_QR_REQUIRED_PREFIX = "qr_required_prefix"


        /**
         * Intent extra key for supplying a regular expression used to validate
         * the scanned QR content.
         *
         * When provided, the decoded QR string must fully match the given regex.
         * Partial matches are not accepted.
         *
         * If validation fails, the scanner displays a “not matched” error and
         * resumes scanning.
         *
         * The pattern is compiled using Kotlin’s [Regex] class.
         *
         * Type: [String]
         */
        const val EXTRA_QR_REGEX = "qr_regex"

        /**
         * Intent extra key used to retrieve the decoded QR content from the
         * activity result.
         *
         * When [QrScanActivity] finishes with [android.app.Activity.RESULT_OK],
         * the result [Intent] will contain the raw QR string under this key.
         *
         * Type: [String]
         */
        const val EXTRA_QR_SCANNER_RESULT = "scan_result"

        /**
         * Creates an [Intent] for launching [QrScanActivity] with optional
         * validation configuration.
         *
         * This helper is intended to be used with the Activity Result API
         * or a custom `ActivityResultContract`.
         *
         * @param context Context used to create the intent.
         * @param title Optional title displayed in the scanner UI.
         * @param requiredPrefix Optional prefix that the QR content must start with.
         *                       If the scanned content does not match, it is treated
         *                       as invalid and scanning continues.
         * @param regex Optional regular expression that the QR content must fully match.
         *              If validation fails, the scanner continues scanning.
         *
         * @return An [Intent] configured to start [QrScanActivity].
         */
        @JvmOverloads
        fun createIntent(
            context: Context,
            title: String? = null,
            requiredPrefix: String? = null,
            regex: String? = null
        ): Intent = Intent(context, QrScanActivity::class.java).apply {
            putExtra(EXTRA_QR_SCANNER_TITLE, title)
            putExtra(EXTRA_QR_REQUIRED_PREFIX, requiredPrefix)
            putExtra(EXTRA_QR_REGEX, regex)
        }
    }
}
