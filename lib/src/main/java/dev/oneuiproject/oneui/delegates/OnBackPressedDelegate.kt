package dev.oneuiproject.oneui.delegates

import android.os.Build
import android.view.View
import android.window.OnBackInvokedCallback
import android.window.OnBackInvokedDispatcher.PRIORITY_OVERLAY
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.addCallback
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class OnBackPressedDelegate(activity: ComponentActivity){

    private interface BackCallbackDelegate {
        fun startListening(view: View, cb: () -> Unit)
        fun stopListening(view: View)
    }

    private val onBackPressedDelegate by lazy {  createBackPressedDelegate(activity) }

    fun startListening(view: View, cb: () -> Unit) {
        onBackPressedDelegate.startListening(view, cb)
    }

    fun stopListening(view: View) {
        onBackPressedDelegate.stopListening(view)
    }
    
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private open class Api33BackCallbackDelegate : BackCallbackDelegate {
        private var obiCallback: OnBackInvokedCallback? = null

        val isListening: Boolean get() = obiCallback != null

        override fun startListening(view: View, cb: () -> Unit) {
            if (isListening) return
            val onBackInvokedDispatcher = view.findOnBackInvokedDispatcher() ?: return
            obiCallback = createOnBackInvokedCallback(cb)
            onBackInvokedDispatcher.registerOnBackInvokedCallback(PRIORITY_OVERLAY, obiCallback!!)
        }

        override fun stopListening(view: View) {
            obiCallback?.let {
                view.findOnBackInvokedDispatcher()?.unregisterOnBackInvokedCallback(it)
                obiCallback = null
            }
        }

        open fun createOnBackInvokedCallback(cb: () -> Unit): OnBackInvokedCallback {
            return OnBackInvokedCallback { cb() }
        }
    }

    private class PreApi33BackCallbackDelegate(private val activity: ComponentActivity) : BackCallbackDelegate {
        private var obpCallback:  OnBackPressedCallback? = null
        private val obpDispatcher = activity.onBackPressedDispatcher

        val isListening: Boolean get() = obpCallback?.isEnabled == true

        override fun startListening(view: View, cb: () -> Unit) {
            if (obpCallback == null) {
                obpCallback = obpDispatcher.addCallback(activity, true) { cb() }
            }
            obpCallback!!.isEnabled = true
        }

        override fun stopListening(view: View) {
            obpCallback?.let{
                it.isEnabled = false
                obpCallback = null
            }
        }
    }

    companion object {
        private fun createBackPressedDelegate(activity: ComponentActivity): BackCallbackDelegate {
             return if (Build.VERSION.SDK_INT >= 33) {
                Api33BackCallbackDelegate()
            } else {
                PreApi33BackCallbackDelegate(activity)
            }
        }
    }
}