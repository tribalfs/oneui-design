package dev.oneuiproject.oneui.ktx

import androidx.activity.BackEventCompat
import androidx.activity.OnBackPressedCallback
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch


/**
 * Convenience method to register [OnBackPressedCallback.handleOnBackPressed].
 * Requires `android:enableOnBackInvokedCallback="true"` be set in the Manifest.
 *
 * @param triggerStateFlow (Optional) Boolean StateFlow to trigger enabling (`true`) and disabling (`false`)
 * custom the callbacks provided in the params. Set none or `null` to keep it enabled.
 *
 * @param onBackPressed             Lambda function for handling the [OnBackPressedCallback.handleOnBackPressed]
 */
inline fun AppCompatActivity.invokeOnBackPressed(
    triggerStateFlow: StateFlow<Boolean>? = null,
    crossinline onBackPressed: ()-> Unit
) {
    invokeOnBack(triggerStateFlow, onBackPressed)
}


/**
 * Convenience method to implement custom [OnBackPressedCallback].
 * Requires `android:enableOnBackInvokedCallback="true"` be set in the Manifest.
 *
 * @param triggerStateFlow (Optional) Boolean StateFlow to trigger enabling (`true`) and disabling (`false`)
 * custom the callbacks provided in the params. Set none or `null` to keep it enabled.
 *
 * @param onBackPressed             Lambda function for handling the [OnBackPressedCallback.handleOnBackPressed]
 * @param onBackStarted             (Optional) Lambda function for handling the [OnBackPressedCallback.handleOnBackStarted].
 *                                  This is only available to api 34+.
 * @param onBackProgressed          (Optional) Lambda function for handling the [OnBackPressedCallback.handleOnBackProgressed].
 *                                  This is only available to api 34+.
 * @param onBackCancelled           (Optional) Lambda function for handling the [OnBackPressedCallback.handleOnBackCancelled].
 *                                   This is only available to api 34+.
 */
inline fun AppCompatActivity.invokeOnBack(
    triggerStateFlow: StateFlow<Boolean>? = null,
    crossinline onBackPressed: ()-> Unit,
    crossinline onBackStarted: (backEvent: BackEventCompat)-> Unit = {},
    crossinline onBackProgressed: (backEvent: BackEventCompat)-> Unit = {},
    crossinline onBackCancelled: ()-> Unit = {}
) {

    val callback = object : OnBackPressedCallback(triggerStateFlow?.value?:true) {
        override fun handleOnBackPressed() {
            onBackPressed()
        }

        override fun handleOnBackStarted(backEvent: BackEventCompat) {
            onBackStarted(backEvent)
        }

        override fun handleOnBackProgressed(backEvent: BackEventCompat) {
            onBackProgressed(backEvent)
        }

        override fun handleOnBackCancelled() {
            onBackCancelled()
        }
    }

    onBackPressedDispatcher.addCallback(this, callback)

    triggerStateFlow?.apply {
        lifecycleScope.launch {
            triggerStateFlow
                .flowWithLifecycle(lifecycle, Lifecycle.State.STARTED)
                .collectLatest { enable ->
                    callback.isEnabled = enable
                }
        }
    }

}
