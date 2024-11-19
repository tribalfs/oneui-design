package dev.oneuiproject.oneui.delegates

import android.os.Build
import android.view.View
import android.window.BackEvent
import android.window.OnBackAnimationCallback
import android.window.OnBackInvokedCallback
import android.window.OnBackInvokedDispatcher.PRIORITY_OVERLAY
import androidx.activity.BackEventCompat
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.addCallback
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface BackHandler{
    fun startBackProgress(backEvent: BackEventCompat)
    fun updateBackProgress(backEvent: BackEventCompat)
    fun handleBackInvoked()
    fun cancelBackProgress()
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class OnBackPressedDelegate(activity: ComponentActivity){

    private interface BackCallbackDelegate {
        fun startListening(view: View, listener: BackHandler)
        fun stopListening(view: View)
    }

    private val onBackPressedDelegate by lazy {  createBackPressedDelegate(activity) }

    fun startListening(view: View, listener: BackHandler) {
        onBackPressedDelegate.startListening(view, listener)
    }

    fun stopListening(view: View) {
        onBackPressedDelegate.stopListening(view)
    }

    @RequiresApi(34)
    private open class Api34BackCallbackDelegate : Api33BackCallbackDelegate() {
        override fun createOnBackInvokedCallback(listener: BackHandler) : OnBackInvokedCallback {
            return object : OnBackAnimationCallback {
                override fun onBackStarted(backEvent: BackEvent) = listener.startBackProgress(BackEventCompat(backEvent))
                override fun onBackProgressed(backEvent: BackEvent) = listener.updateBackProgress(BackEventCompat(backEvent))
                override fun onBackInvoked()  = listener.handleBackInvoked()
                override fun onBackCancelled()  = listener.cancelBackProgress()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private open class Api33BackCallbackDelegate : BackCallbackDelegate {
        private var obiCallback: OnBackInvokedCallback? = null

        val isListening: Boolean get() = obiCallback != null

        override fun startListening(view: View, listener: BackHandler) {
            if (isListening) return
            val onBackInvokedDispatcher = view.findOnBackInvokedDispatcher() ?: return
            obiCallback = createOnBackInvokedCallback(listener)
            onBackInvokedDispatcher.registerOnBackInvokedCallback(PRIORITY_OVERLAY, obiCallback!!)
        }

        override fun stopListening(view: View) {
            obiCallback?.let {
                view.findOnBackInvokedDispatcher()?.unregisterOnBackInvokedCallback(it)
                obiCallback = null
            }
        }

        open fun createOnBackInvokedCallback(listener: BackHandler) =
            OnBackInvokedCallback { listener.handleBackInvoked()}

    }

    private class PreApi33BackCallbackDelegate(private val activity: ComponentActivity) : BackCallbackDelegate {
        private var obpCallback:  OnBackPressedCallback? = null
        private val obpDispatcher = activity.onBackPressedDispatcher

        val isListening: Boolean get() = obpCallback?.isEnabled == true

        override fun startListening(view: View, listener: BackHandler) {
            if (obpCallback == null) {
                obpCallback = obpDispatcher.addCallback(activity, true) { listener.handleBackInvoked() }
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
            return if (Build.VERSION.SDK_INT >= 34) {
                Api34BackCallbackDelegate()
            } else if (Build.VERSION.SDK_INT == 33) {
                Api33BackCallbackDelegate()
            } else {
                PreApi33BackCallbackDelegate(activity)
            }
        }
    }
}