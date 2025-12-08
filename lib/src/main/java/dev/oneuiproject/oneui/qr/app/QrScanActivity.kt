package dev.oneuiproject.oneui.qr.app

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Bitmap.CompressFormat
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.OrientationEventListener
import android.view.View
import android.widget.FrameLayout
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.oneui.floatingdock.util.doOnGlobalLayout
import com.google.mlkit.vision.common.InputImage
import dev.oneuiproject.oneui.design.R
import dev.oneuiproject.oneui.ktx.isInMultiWindowModeCompat
import dev.oneuiproject.oneui.ktx.semToast
import dev.oneuiproject.oneui.qr.app.internal.CameraResolutionHelper.createQrResolutionSelector
import dev.oneuiproject.oneui.qr.app.internal.CropOverlayView
import dev.oneuiproject.oneui.qr.app.internal.OrientationHysteresis
import dev.oneuiproject.oneui.qr.app.internal.observeOnce
import dev.oneuiproject.oneui.qr.widget.QrCodeScannerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.Executors

/**
 * An Activity that provides a full-featured QR code scanning interface.
 *
 * This activity handles camera initialization, preview display, and image analysis using ML Kit.
 * It supports scanning both from the live camera feed and from static images picked from the gallery.
 *
 * Key features include:
 * - **Live Camera Scanning:** Uses CameraX to stream frames and detect QR codes in real-time.
 * - **Gallery Pick & Crop:** Allows users to pick an image, crop it using a custom overlay, and scan the cropped region.
 * - **Orientation Handling:** Locks to portrait for scanning but supports orientation changes during the static image editing phase.
 * - **Validation:** Supports optional validation of scanned content via prefix matching or Regex patterns.
 * - **UI Integration:** Implements OneUI-style design patterns, including immersive system bars and floating action buttons.
 *
 * To launch this activity with specific configurations, use [createIntent].
 *
 * @see QrCodeScannerView
 */
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
 * ## Gallery integration
 *
 * Tapping the gallery button:
 *
 * - temporarily pauses camera analysis in [QrCodeScannerView],
 * - launches a system picker via [ActivityResultContracts.GetContent],
 * - on successful selection, passes the chosen image as [InputImage] back into
 *   [QrCodeScannerView] for offline QR detection.
 *
 * If the user cancels the picker, analysis is resumed.
 *
 * ## Usage
 *
 * Typical usage is via the Activity Result API and the provided [createIntent] helper,
 * or via a custom [ActivityResultContracts] (e.g. `QrScanContract`) that wraps this activity.
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
 */
open class QrScanActivity : AppCompatActivity(), QrCodeScannerView.Listener {

    private val cameraExecutor = Executors.newSingleThreadExecutor()
    private var camera: Camera? = null
    private var insetsController: WindowInsetsControllerCompat? = null
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
    private var currentCroppedFile: File? = null // New

    private val requestCameraPermLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) startCamera()
            else semToast(getString(R.string.oui_des_camera_perm_not_granted))
        }

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode != RESULT_OK) {
                // User backed out → resume camera analysis
                exitStaticImageMode()
                qrCodeScannerView.pauseAnalysis(false)
                return@registerForActivityResult
            }

            val uri = result.data?.data
            if (uri == null) {
                exitStaticImageMode()
                qrCodeScannerView.pauseAnalysis(false)
                return@registerForActivityResult
            }

            currentImageUri = uri
            restoreNormRect = null

            showPickedImage(uri)
        }

    private val resolutionSelector by lazy(LazyThreadSafetyMode.NONE) {
        createQrResolutionSelector(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lockToPortrait()
        setContentView(R.layout.oui_des_qr_scanner_activity)
        initViews()

        WindowCompat.setDecorFitsSystemWindows(window, true)
        hideSystemBars()
        ViewCompat.setOnApplyWindowInsetsListener(cropBottomMenu) { v, insets ->
            val systemAndCutoutInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updatePadding(bottom = systemAndCutoutInsets.bottom)
            insets
        }

        initOrientationListener()

        restoreInstanceState(savedInstanceState)

        // If we're restoring static mode, avoid showing live UI for a frame.
        if (shouldRestoreStaticMode) {
            forceHideLiveUiImmediately()
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
            // Scenario A: We have a confirmed cropped file.
            // Load that directly and skip the cropping UI.
            if (currentCroppedFile != null && currentCroppedFile!!.exists()) {
                showFinalCroppedImage(currentCroppedFile!!)
            }
            // Scenario B: We were in the middle of cropping.
            // Load original URI and apply the crop rect.
            else if (currentImageUri != null) {
                enterStaticImageMode()
                showPickedImage(currentImageUri!!)
            }
        }
    }

    private fun initViews() {
        mainLayout = findViewById(R.id.mainLayout)
        qrCodeScannerView = findViewById(R.id.qr_scanner_view)
        previewView = findViewById(R.id.preview_view)
        cropBottomMenu = findViewById(R.id.crop_bottom_menu)
        cropOverlayView = findViewById(R.id.qr_crop_overlay)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // Re-apply system UI visibility flags as they might be lost on rotation
        hideSystemBars()

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
        if (orientationListener?.canDetectOrientation() == true) orientationListener?.enable()
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
        currentImageUri = state.getParcelable(KEY_IMAGE_URI, Uri::class.java)

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
    }

    // ------------------------------------------------------------------------------------
    // Orientation / system bars
    // ------------------------------------------------------------------------------------

    private fun hideSystemBars() {
        insetsController = WindowCompat.getInsetsController(window, window.decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        insetsController?.hide(WindowInsetsCompat.Type.systemBars())
        @Suppress("DEPRECATION")
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            or View.SYSTEM_UI_FLAG_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    )
        }
    }

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
        orientationListener?.disable()
        allowOrientationChange()
    }

    private fun exitStaticImageMode() {
        isStaticImageMode = false
        currentCroppedFile = null
        currentImageUri = null
        lockToPortrait()
        if (orientationListener?.canDetectOrientation() == true) orientationListener?.enable()
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
                R.id.menu_item_crop_cancel -> {
                    onGalleryBtnClicked(); cancelCrop()
                }
            }
            true
        }
    }

    private fun showPickedImage(uri: Uri) {
        try {
            forceHideLiveUiImmediately()

            val image = InputImage.fromFilePath(this, uri)
            qrCodeScannerView.setInputImageNoDecode(image.bitmapInternal!!, image.rotationDegrees)

            cropBottomMenu.isVisible = true
            cropOverlayView.isVisible = true
            initCropBottomMenu()

            cropOverlayView.post {
                val bounds = qrCodeScannerView.getDecodedImageBounds()
                if (!bounds.isEmpty) {
                    cropOverlayView.setImageBounds(bounds, restoreNormRect)
                } else {
                    // Fallback: keep overlay hidden if we can't determine bounds yet
                    cropOverlayView.isVisible = false
                }
            }

        } catch (e: Exception) {
            e.message?.let { semToast(it) }
            qrCodeScannerView.pauseAnalysis(false)
        }
    }

    private fun showFinalCroppedImage(file: File) {
        enterStaticImageMode()
        forceHideLiveUiImmediately()

        val bitmap = BitmapFactory.decodeFile(file.absolutePath)
        if (bitmap != null) {
            qrCodeScannerView.showDecodedImage(bitmap)
            qrCodeScannerView.showBlackBackground()
        }
    }

    private fun confirmCrop() {
        val cropRect = cropOverlayView.getCropRect()
        // After confirm, we no longer need the overlay;
        // clear visible UI.
        cropOverlayView.isVisible = false
        cropBottomMenu.isVisible = false

        qrCodeScannerView.cropAndDecode(cropRect)
        lifecycleScope.launch(Dispatchers.IO) {
            qrCodeScannerView.getDecodedImage()?.let {
                val file = File(cacheDir, "cropped_qr_${System.currentTimeMillis()}.png")
                FileOutputStream(file).use { out ->
                    it.compress(CompressFormat.PNG, 100, out)
                }
                currentCroppedFile = file // Track this file
            }
        }
        // Clear restoration state (this crop has been applied)
        restoreNormRect = null
        currentImageUri = null
        cropOverlayView.reset()
    }

    private fun cancelCrop() {
        restoreNormRect = null
        currentImageUri = null

        cropOverlayView.isVisible = false
        cropBottomMenu.isVisible = false

        qrCodeScannerView.resetDecodedImage()
        cropOverlayView.reset()
    }

    // ------------------------------------------------------------------------------------
    // QrCodeScannerView.Listener
    // ------------------------------------------------------------------------------------

    override fun isAllowClick(): Boolean = true

    override fun onFlashBtnClicked(isActivated: Boolean) {
        camera?.cameraControl?.enableTorch(isActivated)
    }

    override fun onGalleryBtnClicked() {
        qrCodeScannerView.pauseAnalysis(true)
        val pickIntent = Intent(Intent.ACTION_PICK).apply {
            type = "image/*"
           // putExtra("crop", "true");
           // putExtra("animationDuration", 200);
        }
        pickImageLauncher.launch(pickIntent)
    }

    fun getGalleryIntentForQrScanner(): Intent {
        val uriForFile = FileProvider.getUriForFile(
            applicationContext, applicationContext.getPackageName() + ".provider",
            File(cacheDir, "qr_scan_temp.jpg")
        )
        return Intent("android.intent.action.GET_CONTENT", null).apply {
            setPackage("com.sec.android.gallery3d")
            setType("image/*")
            putExtra("crop", "true");
            putExtra("animationDuration", 200);
            //putExtra("output", uriForFile);
            addFlags(3);
            //setClipData(ClipData.newRawUri("output", uriForFile));
        }
    }


    override fun onStaticImageModeChanged(isStaticImageMode: Boolean) {
        if (isStaticImageMode) enterStaticImageMode() else exitStaticImageMode()
    }

    override fun onQRDetected(content: String): Boolean {
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
        private const val KEY_CROPPED_FILE_PATH = "key_cropped_file_path"

        private const val TAG = "QrScanActivity"

        const val EXTRA_QR_SCANNER_TITLE = "title"
        const val EXTRA_QR_SCANNER_RESULT = "scan_result"
        const val EXTRA_QR_REQUIRED_PREFIX = "qr_required_prefix"
        const val EXTRA_QR_REGEX = "qr_regex"

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
