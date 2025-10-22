import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.FileProvider
import java.io.File

/**
 * A utility object for handling sharing of files and text content.
 * It provides convenience functions to share single or multiple files and plain text,
 * with an optional preference for Samsung's Quick Share app if available.
 */
object SharingUtils {

    private const val SAMSUNG_QUICK_SHARE_PACKAGE = "com.samsung.android.app.sharelive"
    private const val MIME_TYPE_TEXT = "text/plain"
    private const val TAG = "SharingUtils"

    /**
     * Shares a single file.
     * @param context The context used to start the share activity.
     */
    fun File.share(context: Context, resultLauncher: ActivityResultLauncher<Intent>? = null) {
        listOf(this).share(context, resultLauncher)
    }

    /**
     * Shares a list of files.
     * @param context The context used to start the share activity.
     */
    fun List<File>.share(context: Context, resultLauncher: ActivityResultLauncher<Intent>? = null) {
        if (this.isEmpty()) {
            Log.e(TAG, "No files to share.")
            return
        }

        val contentUris = this.mapNotNull { it.getFileUri(context) }
        if (contentUris.isEmpty()) {
            Log.e(TAG, "Could not resolve any file URIs for sharing.")
            return
        }

        val intent = Intent().apply {
            if (context.isSamsungQuickShareAvailable()) {
                `package` = SAMSUNG_QUICK_SHARE_PACKAGE
            }
            action = if (contentUris.size > 1) Intent.ACTION_SEND_MULTIPLE else Intent.ACTION_SEND

            // Determine MIME type from the first file, assuming all files are of the same type.
            type = contentUris.first().getMimeType(context)

            if (contentUris.size > 1) {
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(contentUris))
            } else {
                putExtra(Intent.EXTRA_STREAM, contentUris.first())
            }

            // Set ClipData to support drag-and-drop and other intent scenarios.
            val clipData = ClipData.newUri(context.contentResolver, "files", contentUris.first())
            contentUris.drop(1).forEach { uri -> clipData.addItem(ClipData.Item(uri)) }
            this.clipData = clipData

            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        resultLauncher?.launch(intent) ?: intent.startChooser(context, "Share via")
    }

    /**
     * Shares a string as plain text.
     * @param context The context used to start the share activity.
     */
    fun String.share(context: Context, resultLauncher: ActivityResultLauncher<Intent>? = null) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            if (context.isSamsungQuickShareAvailable()) {
                `package` = SAMSUNG_QUICK_SHARE_PACKAGE
            }
            type = MIME_TYPE_TEXT
            putExtra(Intent.EXTRA_TEXT, this@share)
        }
        intent.startChooser(context, "Share text via", resultLauncher)
    }

    /**
     * Attempts to start a share intent, falling back to a generic chooser if the preferred app fails.
     * This extension function provides a safer way to start share activities.
     *
     * @param context The context to use for starting the activity.
     * @param title The title to display on the chooser dialog.
     */
    private fun Intent.startChooser(context: Context, title: CharSequence, resultLauncher: ActivityResultLauncher<Intent>? = null) {
        // Create a base chooser intent.
        val chooserIntent = Intent.createChooser(this, title)

        try {
            resultLauncher?.launch(this) ?: context.startActivity(this)
            return // Successfully launched Quick Share
        } catch (e: ActivityNotFoundException) {
            Log.w(
                TAG,
                "Failed to start Samsung Quick Share directly, falling back to chooser.",
                e
            )
            // Fallback will happen below by starting the chooserIntent.
            `package` = null // Reset package to avoid issues with the chooser
        }

        // Start the standard system chooser.
        try {
            resultLauncher?.launch(chooserIntent) ?: context.startActivity(chooserIntent)
        } catch (e: ActivityNotFoundException) {
            Log.e(TAG, "No activity found to handle the share intent.", e)
            // Optionally, show a Toast to the user that no app can handle the action.
        }
    }

    /**
     * Checks if the Samsung Quick Share application is installed on the device.
     * @return `true` if Quick Share is available, `false` otherwise.
     */
    fun Context.isSamsungQuickShareAvailable(): Boolean {
        return try {
            packageManager.getPackageInfo(SAMSUNG_QUICK_SHARE_PACKAGE, PackageManager.GET_ACTIVITIES)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }.also {
            Log.d(TAG, "Samsung Quick Share available: $it")
        }
    }
}

/**
 * Converts a [File] to a content [Uri] using a [FileProvider].
 * @param context The context used to get the authority for the FileProvider.
 * @return A content [Uri] for the file, or null on error.
 */
private fun File.getFileUri(context: Context): Uri? {
    return try {
        // Replace with your app's package name
        val authority = "${context.packageName}.provider"
        FileProvider.getUriForFile(context, authority, this)
    } catch (e: IllegalArgumentException) {
        Log.e("SharingUtils", "File URI could not be created for $this", e)
        null
    }
}

/**
 * Determines the MIME type of a file from its content [Uri].
 * @param context The context used to access the ContentResolver.
 * @return The MIME type as a string, or a generic "application/octet-stream" if it can't be determined.
 */
private fun Uri.getMimeType(context: Context): String? {
    return if (scheme == "content") {
        context.contentResolver.getType(this)
    } else {
        val fileExtension = MimeTypeMap.getFileExtensionFromUrl(this.toString())
        MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension.lowercase())
    } ?: "application/octet-stream"
}