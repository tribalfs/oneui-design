@file:Suppress("NOTHING_TO_INLINE")

package dev.oneuiproject.oneui.qr.widget

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Point
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.RectF
import android.os.Build
import android.provider.Settings
import android.util.AttributeSet
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.content.res.AppCompatResources
import androidx.camera.core.ImageAnalysis
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
import androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.UNSET
import androidx.core.animation.doOnEnd
import androidx.core.animation.doOnStart
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import com.airbnb.lottie.LottieAnimationView
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import dev.oneuiproject.oneui.design.R
import dev.oneuiproject.oneui.ktx.dpToPx
import dev.oneuiproject.oneui.qr.app.internal.QrCodeScanEngine
import dev.oneuiproject.oneui.qr.app.internal.cropToUprightSquare
import dev.oneuiproject.oneui.qr.app.internal.distance
import dev.oneuiproject.oneui.qr.app.internal.getAngleDegrees
import dev.oneuiproject.oneui.qr.app.internal.getTargetRect
import dev.oneuiproject.oneui.qr.widget.internal.AnimatorFactory
import dev.oneuiproject.oneui.qr.widget.internal.AnimatorFactory.defaultScanningPathInterpolator
import dev.oneuiproject.oneui.qr.widget.internal.AnimatorFactory.sineInOut60
import dev.oneuiproject.oneui.widget.StrokedTextView

/**
 * A high-level, self-contained UI component for scanning QR codes.
 *
 * `QrCodeScannerView` encapsulates:
 * - CameraX frame analysis via [imageAnalyzer]
 * - QR decoding using ML Kit
 * - OneUI-styled scanning animations and visual feedback
 * - Flash and gallery controls
 * - Static image decoding with optional cropping
 * - Error handling dialogs for unsupported or failed scans
 *
 * The view is designed to be embedded directly in an Activity or Fragment layout.
 * Host components are responsible for:
 *
 * 1. Calling [initialize] once the view is inflated.
 * 2. Binding [imageAnalyzer] to a CameraX `ImageAnalysis` use case.
 * 3. Forwarding orientation changes via [updateOrientation].
 * 4. Implementing [Listener] to receive scan results and UI callbacks.
 *
 * This class manages its own internal state transitions and animations.
 */

class QrCodeScannerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr, defStyleRes) {

    /**
     * Callback interface for receiving scan results and user interactions
     * from [QrCodeScannerView].
     *
     * Implementations typically reside in the hosting Activity or Fragment.
     */
    interface Listener {
        /**
         * Determines whether touch events on the scanner view are allowed.
         *
         * This is consulted during [onInterceptTouchEvent].
         *
         * @return `true` to allow touches to pass through,
         *         `false` to consume and block them.
         */
        fun isAllowClick(): Boolean

        /**
         * Called when a QR code has been successfully decoded.
         *
         * @param content The decoded QR payload.
         * @return `true` if the content is accepted and handled.
         *         Returning `false` triggers a "not supported" error dialog.
         */
        fun onQRDetected(content: String): Boolean

        /**
         * Called when decoding fails for a static input image
         * (e.g. gallery image or cropped bitmap).
         */
        fun onInputImageError()

        /**
         * Called when the flash toggle button is pressed.
         *
         * @param isActivated Current flash state after the toggle.
         */
        fun onFlashBtnClicked(isActivated: Boolean)

        /**
         * Called when the gallery button is pressed.
         *
         * The host should launch an image picker and later supply the
         * selected bitmap via [setInputImage].
         */
        fun onGalleryBtnClicked()

        /**
         * Called when the scanner switches between live camera mode
         * and static image mode.
         *
         * @param isStaticImageMode `true` when decoding a still image,
         *                          `false` when returning to live scanning.
         */
        fun onStaticImageModeChanged(isStaticImageMode: Boolean)
    }

    private var decodedImageBitmap: Bitmap? = null
    private var listener: Listener? = null

    private val qrCodeScanEngine by lazy {
        QrCodeScanEngine(object : QrCodeScanEngine.Listener {
            override fun onQrDetected(
                qrContent: String,
                frameBitmap: Bitmap,
                barcode: Barcode,
                isCameraInput: Boolean
            ) {
                processQR(qrContent, frameBitmap, barcode, isCameraInput)
            }

            override fun onInputImageError(cause: Exception?) {
                if (isContextValid()) {
                    showDetectErrorDialog()
                    listener?.onInputImageError()
                }
            }

            override fun isAnalysisPaused(): Boolean {
                return isQrRoiAnimating()
                        || isErrorDialogShowing()
                        || isNotMatchedDialogShowing()
                        || decodedImageBitmap != null
            }

        })
    }

    /**
     * CameraX analyzer used to process frames for QR detection.
     *
     * Bind this instance to your `ImageAnalysis` use case:
     *
     * ```
     * imageAnalysis.setAnalyzer(executor, scannerView.imageAnalyzer)
     * ```
     */
    val imageAnalyzer by lazy {
        ImageAnalysis.Analyzer { image -> qrCodeScanEngine.analyze(image) }
    }

    private val centerPoint: PointF = PointF()
    internal var detectErrorDialog: AlertDialog? = null
    internal var notMatchedScanTypeErrorDialog: AlertDialog? = null
    private var flashAnimator: ValueAnimator? = null
    private var qrDetectAnimation: AnimatorSet? = null
    private var qrCodeResult: String? = null
    private var staticInputImage: InputImage? = null
    private var rotation: Float = 0f

    private var qrImageGroup: RelativeLayout
    private var qrDetectedImage: ImageView
    private var qrScanningRectangle: ImageView
    private var roi: ImageView
    private var roiGroup: FrameLayout
    private var decodedImage: ImageView
    private var roiGuideLine: View
    private var roiLottie: LottieAnimationView
    private var flashButton: LottieAnimationView
    private var galleryButton: ImageView
    /** Contains the title, flash button and gallery button */
    private var defaultViewGroup: ConstraintLayout
    private var dimBg: View
    /** Background for decodedImage */
    private var blackBg: View
    private var guideText: TextView

    init {
        LayoutInflater.from(context).inflate(R.layout.oui_des_qr_scanner_view, this)
        defaultViewGroup = findViewById(R.id.default_view_group)
        guideText = createStrokedTextView().also { defaultViewGroup.addView(it, 0) }
        qrImageGroup = findViewById(R.id.qr_image_group)
        qrDetectedImage = findViewById(R.id.qr_detected_image)
        qrScanningRectangle = findViewById(R.id.qr_scanning_rectangle)
        roi = findViewById(R.id.roi)
        roiGroup = findViewById(R.id.roi_group)
        decodedImage = findViewById(R.id.decoded_image)
        roiGuideLine = findViewById(R.id.roi_guide_line)
        roiLottie = findViewById(R.id.roi_lottie)
        flashButton = findViewById(R.id.flash_button)
        galleryButton = findViewById(R.id.gallery_button)
        dimBg = findViewById(R.id.dim_bg)
        blackBg = findViewById(R.id.black_bg)
    }

    /**
     * Initializes the scanner UI and enables QR scanning.
     *
     * This must be called once after view inflation and before analysis begins.
     *
     * @param title Optional guide text displayed above the scanning area.
     * @param isFlashAvailable Whether the device camera supports flash.
     * @param listener Callback receiver for scan results and UI actions.
     */
    fun initialize(
        title: CharSequence? = null,
        isFlashAvailable: Boolean,
        listener: Listener
    ) {
        title?.let { guideText.text = it }
        this.listener = listener

        if (isReduceTransparencyOn(getContext())) {
            galleryButton.setBackground(
                AppCompatResources.getDrawable(
                    context,
                    R.drawable.oui_des_qr_gallery_high_contrast
                )
            )
            flashButton.setBackground(
                AppCompatResources.getDrawable(
                    context,
                    R.drawable.oui_des_qr_flash_high_contrast_background
                )
            )
        } else {
            galleryButton.setBackground(
                AppCompatResources.getDrawable(
                    context,
                    R.drawable.oui_des_qr_gallery_btn
                )
            )
        }

        disableFlashButton(!isFlashAvailable)

        if (isFlashAvailable) {
            flashButton.setOnClickListener { view ->
                animateFlashButton()
                updateFlashButtonDescription()
                listener.onFlashBtnClicked(view.isActivated)
            }
        }

        updateFlashButtonDescription()

        galleryButton.setOnClickListener {
            cancelAnimation()
            listener.onStaticImageModeChanged(true)
            listener.onGalleryBtnClicked()
        }

        roiLottie.addAnimatorListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animator: Animator) {
                roiLottie.visibility = INVISIBLE
                roi.visibility = VISIBLE
                staticInputImage?.let {
                    qrCodeScanEngine.processSingleImage(it, it.bitmapInternal!!)
                }
            }
        })
    }

    /**
     * Pauses or resumes QR analysis and related UI animations.
     *
     * @param pause `true` to stop analysis and hide scanning UI,
     *              `false` to resume live scanning.
     */
    fun pauseAnalysis(pause: Boolean) {
        if (pause) {
            hideDefaultViews()
            resetRoiGroup()
            qrCodeScanEngine.pauseAnalysis()
        } else {
            showDefaultViews()
            startQrRoiAnimation()
            qrCodeScanEngine.resumeAnalysis()
        }
    }

    /**
     * Supplies a bitmap for static (non-camera) QR decoding.
     *
     * @param image Bitmap to decode.
     * @param imageRotation Rotation applied to the bitmap in degrees.
     * @param decodeNow Whether decoding should start immediately.
     */
    fun setInputImage(image: Bitmap, imageRotation: Int, decodeNow: Boolean) {
        cancelAnimation()
        listener?.onStaticImageModeChanged(true)
        resetRoiGroup()
        showBlackBackground()
        hideDefaultViewsImmediate()
        showDecodedImage(image, imageRotation, decodeNow)
        if (decodeNow) {
            startQrRoiAnimation()
        }
    }

    /**
     * Crops the currently displayed decoded image and attempts QR decoding
     * within the cropped region.
     *
     * @param cropRectView Crop rectangle in view coordinates.
     * @param onDecodedImageUpdated Callback invoked after decoded image is updated
     * to the cropped version.
     */
    fun cropAndDecode(cropRectView: RectF, onDecodedImageUpdated: () -> Unit) {
        decodedImage.visibility = INVISIBLE

        val bitmap = decodedImageBitmap ?: run {
            resetAndAnimateRoi()
            return
        }

        // Bounds of decodedImage inside this view
        val imageBounds = getDecodedImageBounds() // RectF in view coords

        // Intersect to be safe
        val effectiveCrop = RectF(cropRectView)
        if (!effectiveCrop.intersect(imageBounds)) {
            // Nothing valid to crop; reset
            resetAndAnimateRoi()
            return
        }

        // Normalize crop rect relative to the visible image bounds
        val relLeft = (effectiveCrop.left - imageBounds.left) / imageBounds.width()
        val relTop = (effectiveCrop.top - imageBounds.top) / imageBounds.height()
        val relRight = (effectiveCrop.right - imageBounds.left) / imageBounds.width()
        val relBottom = (effectiveCrop.bottom - imageBounds.top) / imageBounds.height()

        // Convert to bitmap coordinates
        val bmpWidth = bitmap.width.toFloat()
        val bmpHeight = bitmap.height.toFloat()

        var cropX = (relLeft * bmpWidth).toInt()
        var cropY = (relTop * bmpHeight).toInt()
        var cropW = ((relRight - relLeft) * bmpWidth).toInt()
        var cropH = ((relBottom - relTop) * bmpHeight).toInt()

        // Clamp to valid region
        if (cropX < 0) cropX = 0
        if (cropY < 0) cropY = 0
        if (cropX + cropW > bitmap.width) cropW = bitmap.width - cropX
        if (cropY + cropH > bitmap.height) cropH = bitmap.height - cropY
        if (cropW <= 0 || cropH <= 0) {
            resetAndAnimateRoi()
            return
        }

        val cropped = Bitmap.createBitmap(bitmap, cropX, cropY, cropW, cropH)
        updateToDecodedImageLayout()
        showDecodedImage(cropped, true)
        startQrRoiAnimation()
        onDecodedImageUpdated()
    }

    /**
     * Clears the currently decoded image and related state.
     */
    fun resetDecodedImage() {
        decodedImage.visibility = INVISIBLE
        decodedImage.setImageBitmap(null)
        decodedImage.rotation = 0f
        decodedImageBitmap = null
        staticInputImage = null
    }

    /**
     * Displays a black background behind the decoded image.
     */
    fun showBlackBackground() {
        blackBg.alpha = 1.0f
        blackBg.visibility = VISIBLE
    }

    /**
     * Displays a decoded image bitmap.
     *
     * @param bitmap Bitmap to display.
     * @param rotationDegrees Rotation applied before display.
     * @param updateInputImage Whether this bitmap should be used for decoding.
     */
    fun showDecodedImage(bitmap: Bitmap, rotationDegrees: Int, updateInputImage: Boolean = false) {
        val rotatedBitmap = getRotatedBitmap(bitmap, rotationDegrees)
        showDecodedImage(rotatedBitmap, updateInputImage)
    }

    /**
     * Displays a decoded image bitmap.
     *
     * @param bitmap Bitmap to display.
     * @param updateInputImage Whether this bitmap should be used for decoding.
     */
    fun showDecodedImage(bitmap: Bitmap, updateInputImage: Boolean = false) {
        decodedImageBitmap = bitmap
        decodedImage.visibility = VISIBLE
        decodedImage.setImageBitmap(bitmap)
        if (updateInputImage) {
            staticInputImage = InputImage.fromBitmap(bitmap, 0)
        }
    }

    /**
     * Returns the currently displayed decoded bitmap, if any.
     */
    fun getDecodedImage(): Bitmap? = decodedImageBitmap

    /**
     * Returns the bounds of the decoded image within this view's coordinate space.
     */
    fun getDecodedImageBounds(): RectF = RectF().apply {
        decodedImage.drawable?.let {
            decodedImage.imageMatrix.mapRect(this, RectF(it.bounds))
        }
    }

    /**
     * Starts the ROI scanning animation.
     *
     * Typically called after returning from static image mode or
     * when resuming live scanning.
     */
    fun startQrRoiAnimation() {
        if (blackBg.isVisible && !decodedImage.isVisible) {
            blackBg.animate().apply {
                alpha(0.0f)
                duration = 100
                withEndAction {
                    blackBg.visibility = INVISIBLE
                    roiLottie.visibility = VISIBLE
                    roiLottie.playAnimation()
                }
            }
        } else {
            roiLottie.visibility = VISIBLE
            roiLottie.playAnimation()
        }
    }

    /**
     * Cancels any running scan or detection animations.
     */
    fun cancelAnimation() {
        qrDetectAnimation?.apply {
            if (isStarted) cancel()
        }

        if (isQrRoiAnimating()) {
            roiLottie.cancelAnimation()
        }
    }

    /**
     * Updates the visual orientation of scanner UI elements.
     *
     * This does not affect camera output, only overlay UI.
     *
     * @param rotation Rotation angle in degrees.
     */
    fun updateOrientation(rotation: Float) {
        this.rotation = rotation
        AnimatorFactory.rotation(flashButton, to = rotation).start()
        AnimatorFactory.rotation(galleryButton, to = rotation).start()
        qrScanningRectangle.rotation = rotation
        qrDetectedImage.rotation = rotation
        updateDecodedImageOrientation(rotation.toInt())
    }

    /**
     * Displays a decode failure dialog for static images.
     */
    fun showDetectErrorDialog() {
        listener?.onStaticImageModeChanged(true)
        detectErrorDialog = AlertDialog.Builder(getContext()).apply {
            setTitle(R.string.oui_des_qrdialog_decode_error_title)
            setMessage(R.string.oui_des_qrdialog_decode_error_body)
            setPositiveButton(android.R.string.ok) { d, _ -> d.dismiss() }
            setOnDismissListener {
                detectErrorDialog = null
                listener?.onStaticImageModeChanged(false)
                showDefaultViews()
                resetAndAnimateRoi()
                qrCodeScanEngine.resumeAnalysis()
            }

        }.create().apply {
            seslSetBackgroundBlurEnabled()
            show()
        }
    }

    /**
     * Displays a dialog indicating the scanned QR content
     * does not match the expected type.
     */
    fun showNotMatchedRequestedScanTypeErrorDialog() {
        listener?.onStaticImageModeChanged(true)
        notMatchedScanTypeErrorDialog = AlertDialog.Builder(getContext()).apply {
            setTitle(R.string.oui_des_qrdialog_not_matched_request_type_error_title)
            setMessage(R.string.oui_des_qrdialog_not_matched_request_type_error_body)
            setPositiveButton(android.R.string.ok) { d, _ -> d.dismiss() }
            setOnDismissListener {
                notMatchedScanTypeErrorDialog = null
                listener?.onStaticImageModeChanged(false)
                showDefaultViews()
                resetAndAnimateRoi()
                qrCodeScanEngine.resumeAnalysis()
            }
        }.create().apply {
            seslSetBackgroundBlurEnabled()
            show()
        }
    }

    /**
     * @return `true` if a decode error dialog is currently visible.
     */
    fun isErrorDialogShowing(): Boolean = detectErrorDialog?.isShowing() == true

    /**
     * @return `true` if a "not matched" error dialog is currently visible.
     */
    fun isNotMatchedDialogShowing(): Boolean = notMatchedScanTypeErrorDialog?.isShowing() == true

    /**
     * Dismisses any currently displayed error dialogs.
     */
    fun dismissDialog() {
        detectErrorDialog?.dismiss()
        notMatchedScanTypeErrorDialog?.dismiss()
    }

    private fun getDetectedImageRect(rectF: RectF): RectF {
        val dimension = resources.getDimension(R.dimen.oui_des_qr_scanner_detected_image_padding)
        return RectF(
            rectF.left - dimension,
            rectF.top - dimension,
            rectF.right + dimension,
            rectF.bottom + dimension
        )
    }

    private fun getQrImageShowAnimator(
        detectedBitmap: Bitmap?,
        targetBounds: RectF,
        rotationDegrees: Float
    ): Animator {
        qrImageGroup.apply {
            updateLayoutParams<ConstraintLayout.LayoutParams> {
                width = targetBounds.width().toInt()
                height = targetBounds.height().toInt()
            }
            translationX = targetBounds.centerX() - this@QrCodeScannerView.centerPoint.x
            translationY = targetBounds.centerY() - this@QrCodeScannerView.centerPoint.y
            rotation = rotationDegrees
            scaleX = 0.8f
            scaleY = 0.8f
            alpha = 0.0f
            visibility = VISIBLE
        }

        qrDetectedImage.apply {
            setImageBitmap(detectedBitmap)
            visibility = VISIBLE
        }

        qrScanningRectangle.visibility = INVISIBLE

        val scaleUpAnimator = AnimatorFactory.scale(
            qrImageGroup,
            0.8f,
            1.0f,
            200,
            defaultScanningPathInterpolator()
        )

        val fadeInAnimator = AnimatorFactory.alpha(qrImageGroup, 0.0f, 1.0f)

        return AnimatorSet().apply {
            playTogether(fadeInAnimator, scaleUpAnimator)
        }
    }

    private fun getRoiRect(rectF: RectF): RectF {
        val dimension = resources.getDimension(R.dimen.oui_des_qr_scanner_roi_padding)
        return RectF(
            rectF.left - dimension,
            rectF.top - dimension,
            rectF.right + dimension,
            rectF.bottom + dimension
        )
    }

    private fun getRoiScaleAnimator(isShrink: Boolean): Animator {
        val from = if (isShrink) 1.0f else 0.8f
        val to = if (isShrink) 0.8f else 1.0f

        return AnimatorFactory.scale(
            view = roi,
            from = from,
            to = to,
            duration = 150L,
            interpolator = AnimatorFactory.sineInOut60()
        ).apply {
            if (isShrink) {
                startDelay = 250L
            }
        }
    }

    private fun getRoiToTargetAnimator(targetRect: RectF, targetRotation: Float): Animator {
        targetRect.offset((-roiGroup.left).toFloat(), (-roiGroup.top).toFloat())
        val roiOriginalWidth = roi.width
        val roiOriginalHeight = roi.height

        return ValueAnimator.ofPropertyValuesHolder(
            PropertyValuesHolder.ofFloat(TRANSLATION_X, targetRect.centerX() - roi.getPivotX()),
            PropertyValuesHolder.ofFloat(TRANSLATION_Y, targetRect.centerY() - roi.getPivotY()),
            PropertyValuesHolder.ofFloat(SCALE_X, 1.0f, targetRect.width() / roiOriginalWidth),
            PropertyValuesHolder.ofFloat(SCALE_Y, 1.0f, targetRect.height() / roiOriginalHeight),
            PropertyValuesHolder.ofFloat(ROTATION, targetRotation)
        ).apply {
            interpolator = sineInOut60()
            duration = 400
            addUpdateListener { animator ->
                roi.apply {
                    translationX = animator.getAnimatedValue(TRANSLATION_X.name) as Float
                    roi.translationY = animator.getAnimatedValue(TRANSLATION_Y.name) as Float
                    roi.rotation = animator.getAnimatedValue(ROTATION.name) as Float
                    updateLayoutParams<FrameLayout.LayoutParams> {
                        width =
                            (roiOriginalWidth * (animator.getAnimatedValue(SCALE_X.name) as Float)).toInt()
                        height =
                            (roiOriginalHeight * (animator.getAnimatedValue(SCALE_Y.name) as Float)).toInt()
                    }
                }
            }
        }
    }

    private fun resetQrImageGroup() {
        qrImageGroup.apply {
            translationX = 0.0f
            translationY = 0.0f
            rotation = 0.0f
            scaleX = 1.0f
            scaleY = 1.0f
            visibility = INVISIBLE
        }
        qrDetectedImage.background = null
        qrScanningRectangle.visibility = INVISIBLE
    }

    private fun resetRoiGroup() {
        roiGroup.updateLayoutParams<ConstraintLayout.LayoutParams> {
            topToBottom = roiGuideLine.id
            topMargin = resources.getDimensionPixelSize(R.dimen.oui_des_qr_scanner_roi_top_margin)
            topToTop = UNSET
            bottomToBottom = UNSET
        }

        roi.apply {
            updateLayoutParams<FrameLayout.LayoutParams> {
                width = MATCH_PARENT
                height = MATCH_PARENT
            }
            translationX = 0.0f
            translationY = 0.0f
            scaleX = 1.0f
            scaleY = 1.0f
            rotation = 0.0f
            alpha = 1.0f
            visibility = INVISIBLE
        }

        roiLottie.progress = 0.0f
        roiLottie.visibility = INVISIBLE
    }

    private fun updateDecodedImageOrientation(rotationDegrees: Int) {
        decodedImageBitmap?.let {
            decodedImage.setImageBitmap(getRotatedBitmap(it, rotationDegrees))
        }
    }

    private fun getRotatedBitmap(bitmap: Bitmap, rotationDegrees: Int): Bitmap {
        return Bitmap.createBitmap(
            bitmap,
            0,
            0,
            bitmap.getWidth(),
            bitmap.getHeight(),
            Matrix().apply { postRotate(rotationDegrees.toFloat()) },
            true
        )
    }


    fun hideDefaultViewsImmediate() {
        defaultViewGroup.animate().cancel()
        defaultViewGroup.alpha = 0f
        defaultViewGroup.visibility = INVISIBLE
    }

    private fun disableFlashButton(disable: Boolean) {
        flashButton.setClickable(!disable)
        if (!disable) {
            flashButton.imageAlpha = 255
        } else {
            resetFlashButton()
            flashButton.imageAlpha = 115
        }
    }

    private fun disableGalleryButton(disable: Boolean) {
        galleryButton.isClickable = !disable
        galleryButton.alpha = if (disable) 0.45f else 1.0f
    }

    private fun hideDefaultViews() {
        defaultViewGroup.animate().apply {
            alpha(0.0f)
            duration = 100L
            withEndAction { defaultViewGroup.visibility = INVISIBLE }
        }
    }


    private fun updateFlashButtonDescription() {
        val desc = context.getString(
            R.string.oui_des_tts_qr_flashlight,
            context.getString(if (flashButton.isActivated) R.string.oui_des_common_on else R.string.oui_des_common_off)
        )
        flashButton.contentDescription = desc
        if (Build.VERSION.SDK_INT >= 26) {
            flashButton.tooltipText = desc
        }
    }

    private fun isReduceTransparencyOn(context: Context): Boolean =
        Settings.System.getInt(
            context.getContentResolver(),
            "accessibility_reduce_transparency",
            0
        ) != 0


    private fun animateFlashButton() {
        val isActivated = flashButton.isActivated
        val start = if (isActivated) 1.0f else 0.0f
        val end = if (isActivated) 0.0f else 1.0f

        flashAnimator?.cancel()
        flashAnimator = ValueAnimator.ofFloat(start, end).apply {
            duration = flashButton.duration
            addUpdateListener { animation ->
                flashButton.progress = animation.animatedValue as Float
            }
            start()
        }

        flashButton.isActivated = !isActivated
    }

    fun isQrRoiAnimating(): Boolean = roiLottie.isAnimating

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        return if (ev.action == MotionEvent.ACTION_UP && listener?.isAllowClick() == false) {
            true
        } else {
            super.onInterceptTouchEvent(ev)
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        centerPoint.set(measuredWidth / 2.0f, measuredHeight / 2.0f)
    }


    fun updateToDecodedImageLayout() {
        roiGroup.updateLayoutParams<ConstraintLayout.LayoutParams> {
            topToTop = PARENT_ID
            bottomToBottom = PARENT_ID
            topToBottom = UNSET
            topMargin = UNSET
        }
    }

    private fun resetFlashButton() {
        flashAnimator?.cancel()
        flashButton.isActivated = false
        flashButton.progress = 0.0f
    }

    private fun showDefaultViews() {
        defaultViewGroup.alpha = 1.0f
        defaultViewGroup.visibility = VISIBLE
    }

    private fun startQrDetectAnimation(
        qrCodeImage: Bitmap?,
        targetQrBounds: RectF,
        rotationDegrees: Float
    ) {
        val detectedQrBounds = getDetectedImageRect(targetQrBounds)
        val roiMoveToTargetAnim =
            getRoiToTargetAnimator(getRoiRect(detectedQrBounds), rotationDegrees)
        val roiShrinkAnim = getRoiScaleAnimator(true)
        val roiExpandAnim = getRoiScaleAnimator(false)

        val roiFadeOutAnim = AnimatorFactory.alpha(roi, 1.0f, 0.0f, 150)

        val qrImageShowAnimator = getQrImageShowAnimator(qrCodeImage, detectedQrBounds, rotationDegrees)
        val dimBackgroundFadeInAnim = AnimatorFactory.alpha(dimBg, 0.0f, 1.0f)

        qrDetectAnimation = AnimatorSet().apply {
            playTogether(roiMoveToTargetAnim, qrImageShowAnimator, roiShrinkAnim)
            playSequentially(qrImageShowAnimator, dimBackgroundFadeInAnim)
            playSequentially(roiShrinkAnim, roiExpandAnim, roiFadeOutAnim)
            playTogether(roiFadeOutAnim, getScanningAnimator())
            doOnEnd {
                resetQrImageGroup()
                dispatchScanResult()
                @Suppress("DEPRECATION")
                announceForAccessibility(getContext().getString(R.string.oui_des_qrcode_scan_tts))
                disableGalleryButton(false)
            }
            start()
        }
    }

    private fun getScanningAnimator() = ValueAnimator.ofFloat(0.0f, 1.0f).apply {
        startDelay = 100
        duration = 400
        interpolator = AnimatorFactory.defaultScanningPathInterpolator()
        addUpdateListener { animator ->
            qrScanningRectangle.drawable.level = (animator.getAnimatedValue() as Float * 10_000.0f).toInt()
        }
        doOnStart {
            qrScanningRectangle.drawable.level = 0
            qrScanningRectangle.visibility = VISIBLE
        }
    }

    private fun dispatchScanResult() {
        if (listener?.onQRDetected(qrCodeResult ?: "") != true) {
            if (isContextValid()) {
                showNotMatchedRequestedScanTypeErrorDialog()
            }
        }
    }

    private fun startQrImageHideAnimation() {
        AnimatorFactory.alpha(qrImageGroup, 1.0f, 0.0f, 100).start()
    }


    private fun isContextValid(): Boolean {
        val ctx = context
        return ctx !is Activity || !ctx.isFinishing && !ctx.isDestroyed
    }

    private inline fun processQR(
        qrContent: String,
        frameBitmap: Bitmap,
        barcode: Barcode,
        isCameraInput: Boolean
    ) {
        disableGalleryButton(true)

        this@QrCodeScannerView.qrCodeResult = qrContent
        performHapticFeedback(HapticFeedbackConstants.CONFIRM)

        val uprightQRImage = frameBitmap.cropToUprightSquare(barcode.cornerPoints) ?: return
        val angleDeg = getAngleDegrees(barcode.cornerPoints) ?: 0f

        val minSize = resources.getDimension(R.dimen.oui_des_qr_scanner_detected_image_min_size)

        val targetRect = if (isCameraInput) {
            val (previewWidth, previewHeight) = with(rootView) {
                width.toFloat() to height.toFloat()
            }
            getTargetRect(
                barcode.cornerPoints,
                barcode.boundingBox,
                previewWidth,
                previewHeight,
                frameBitmap,
                minSize
            )
        } else {
            // Use decodedImageBounds-aware mapping when coming from gallery / cropped image
            getTargetRectForDecodedImage(
                barcode.cornerPoints,
                barcode.boundingBox,
                frameBitmap,
                minSize
            )
        }

        hideDefaultViews()
        startQrDetectAnimation(uprightQRImage, targetRect, angleDeg)

        if (!isCameraInput) {
            staticInputImage = null
        }
    }

    private fun resetView() {
        dimBg.alpha = 0.0f
        resetDecodedImage()
        resetQrImageGroup()
        resetRoiGroup()
    }

    private fun resetAndAnimateRoi() {
        resetView()
        startQrRoiAnimation()
    }

    private fun getTargetRectForDecodedImage(
        cornerPoints: Array<Point>?,
        boundingBox: Rect?,
        frameBitmap: Bitmap,
        minSize: Float
    ): RectF {
        val imageBounds = getDecodedImageBounds()   // already in view coords
        if (imageBounds.isEmpty) return RectF()

        val bmpWidth = frameBitmap.width.toFloat()
        val bmpHeight = frameBitmap.height.toFloat()

        fun mapPoint(px: Int, py: Int): PointF {
            val nx = px.toFloat() / bmpWidth      // 0..1
            val ny = py.toFloat() / bmpHeight     // 0..1
            return PointF(
                imageBounds.left + nx * imageBounds.width(),
                imageBounds.top + ny * imageBounds.height()
            )
        }

        return if (cornerPoints != null && cornerPoints.size >= 4) {
            val mapped = cornerPoints.take(4).map { p -> mapPoint(p.x, p.y) }

            val cx = mapped.map { it.x }.average().toFloat()
            val cy = mapped.map { it.y }.average().toFloat()

            val sideTop = distance(mapped[0], mapped[1])
            val sideLeft = distance(mapped[0], mapped[3])

            val halfSide = maxOf(sideTop, sideLeft, minSize) / 2f

            RectF(
                cx - halfSide,
                cy - halfSide,
                cx + halfSide,
                cy + halfSide
            )
        } else if (boundingBox != null) {
            // Fallback using bounding box
            val tl = mapPoint(boundingBox.left, boundingBox.top)
            val br = mapPoint(boundingBox.right, boundingBox.bottom)

            val mapped = RectF(tl.x, tl.y, br.x, br.y)
            val halfSide = maxOf(mapped.width(), mapped.height(), minSize) / 2f
            val cx = mapped.centerX()
            val cy = mapped.centerY()

            RectF(
                cx - halfSide,
                cy - halfSide,
                cx + halfSide,
                cy + halfSide
            )
        } else {
            RectF()
        }
    }

    private fun createStrokedTextView(): StrokedTextView {
        return StrokedTextView(ContextThemeWrapper(context, androidx.appcompat.R.style.RobotoMedium)).apply {
            layoutParams = ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.MATCH_PARENT,
                resources.getDimensionPixelSize(R.dimen.oui_des_qr_scanner_guide_text_height)
            ).apply {
                resources.getDimensionPixelSize(R.dimen.oui_des_qr_scanner_guide_text_horizontal_margin).let {
                    marginStart = it
                    marginEnd = it
                }
                topMargin = resources.getDimensionPixelSize(R.dimen.oui_des_qr_scanner_guide_text_top_margin)
                endToEnd = PARENT_ID
                startToStart = PARENT_ID
                topToTop = PARENT_ID
                gravity = Gravity.CENTER
            }
            ViewCompat.setFocusedByDefault(this, true)
            maxLines = 2
            setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.oui_des_qr_scanner_guide_text_font_size))
            setTextColor(ContextCompat.getColor( context,R.color.oui_des_qr_scanner_guide_text_color))
            setStroke(true)
            setStrokeColor(resources.getColor(R.color.oui_des_qr_scanner_guide_text_stroke_color, context.theme))
            setStrokeWidth(2.dpToPx(resources))
            text = resources.getString(R.string.oui_des_scan_qr_code)
        }
    }
}

