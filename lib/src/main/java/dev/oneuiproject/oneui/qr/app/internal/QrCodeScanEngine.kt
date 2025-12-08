package dev.oneuiproject.oneui.qr.app.internal

import android.graphics.Bitmap
import androidx.camera.core.ImageProxy
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Engine responsible for scanning QR codes from CameraX frames or single images.
 */
internal class QrCodeScanEngine(
    private val listener: Listener
) {

    /**
     * Callbacks for QR scan events.
     */
    interface Listener {

        /** Return true to temporarily pause analysis (e.g. UI is animating or dialog is open). */
        fun isAnalysisPaused(): Boolean

        /**
         * Called when a QR code is successfully detected.
         *
         * @param qrContent Decoded textual content of the QR code.
         * @param frameBitmap Bitmap of the frame used for detection (already oriented).
         * @param barcode Raw ML Kit barcode object.
         */
        fun onQrDetected(
            qrContent: String,
            frameBitmap: Bitmap,
            barcode: Barcode,
            isCameraInput: Boolean
        )

        /** Called when processing the image fails or no valid QR is found in a one-shot image. */
        fun onInputImageError(cause: Exception? = null)
    }

    private val pauseAnalysis = AtomicBoolean(false)

    private val scanner by lazy {
        BarcodeScanning.getClient(
            BarcodeScannerOptions
                .Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                .build()
        )
    }

    /**
     * Analyze a CameraX frame. Always closes [imageProxy].
     */
    fun analyze(imageProxy: ImageProxy) {
        val frameBitmap = try {
            imageProxy.toFrameBitmap()
        } catch (e: Exception) {
            imageProxy.close()
            listener.onInputImageError(e)
            return
        }

        val inputImage = InputImage.fromBitmap(frameBitmap, 0)

        scanner.process(inputImage)
            .handleResult(
                onSuccess = { barcodes ->
                    if (listener.isAnalysisPaused() || pauseAnalysis.get()) return@handleResult

                    val barcode = barcodes.firstOrNull() ?: return@handleResult
                    val result = barcode.rawValue ?: return@handleResult

                    listener.onQrDetected(
                        qrContent = result,
                        frameBitmap = frameBitmap,
                        barcode = barcode,
                        isCameraInput = true
                    )

                    // prevent duplicate callbacks until explicitly reset
                    pauseAnalysis.set(true)
                },
                onError = { e ->
                    listener.onInputImageError(e)
                },
                onFinally = {
                    imageProxy.close()
                }
            )
    }

    /**
     * Process a single [InputImage] + its original [Bitmap] (e.g. from gallery or file).
     *
     * The bitmap must match the orientation used to build the [InputImage].
     */
    fun processSingleImage(image: InputImage, originalBitmap: Bitmap) {
        scanner.process(image)
            .handleResult(
                onSuccess = { barcodes ->
                    val barcode = barcodes.firstOrNull()
                    val result = barcode?.rawValue

                    if (barcode != null && result != null) {
                        listener.onQrDetected(
                            qrContent = result,
                            frameBitmap = originalBitmap,
                            barcode = barcode,
                            isCameraInput = false
                        )
                    } else {
                        listener.onInputImageError()
                    }
                },
                onError = { e ->
                    listener.onInputImageError(e)
                }
            )
    }

    /** Allow the engine to resume emitting detections. */
    fun resumeAnalysis() {
        pauseAnalysis.set(false)
    }

    /** Pause detections until [resumeAnalysis] is called. */
    fun pauseAnalysis() {
        pauseAnalysis.set(true)
    }

    /** Clear internal state and allow analysis again. */
    fun reset() {
        pauseAnalysis.set(false)
    }

    /**
     * Small extension to reduce boilerplate on ML Kit tasks and guarantee "finally" behavior.
     */
    private inline fun <T> Task<T>.handleResult(
        crossinline onSuccess: (T) -> Unit,
        crossinline onError: (Exception) -> Unit = {},
        crossinline onFinally: () -> Unit = {}
    ) {
        addOnSuccessListener { result ->
            try {
                onSuccess(result)
            } catch (e: Exception) {
                onError(e)
            } finally {
                onFinally()
            }
        }.addOnFailureListener { e ->
            try {
                onError(e)
            } finally {
                onFinally()
            }
        }
    }
}
