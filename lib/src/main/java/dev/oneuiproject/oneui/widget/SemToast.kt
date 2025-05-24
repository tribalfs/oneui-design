@file:Suppress("NOTHING_TO_INLINE")

package dev.oneuiproject.oneui.widget

import android.content.Context
import android.content.res.Resources
import android.view.LayoutInflater
import android.widget.TextView
import androidx.annotation.StringRes
import dev.oneuiproject.oneui.design.R

/**
 * Custom [Toast][android.widget.Toast] widget with no icon.
 */
open class SemToast (context: Context?) : android.widget.Toast(context) {
    companion object {
        @get:JvmStatic
        const val LENGTH_SHORT = android.widget.Toast.LENGTH_SHORT
        @get:JvmStatic
        const val LENGTH_LONG = android.widget.Toast.LENGTH_LONG

        @JvmStatic
        fun makeText(context: Context, text: CharSequence?, duration: Int): SemToast {
            return SemToast(context).apply {
                val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
                setView(
                    inflater.inflate(R.layout.oui_des_transient_notification, null).apply {
                        findViewById<TextView>(android.R.id.message).text = text
                    }
                )
                setDuration(duration)
            }
        }

        @JvmStatic
        @Throws(Resources.NotFoundException::class)
        fun makeText(context: Context, @StringRes resId: Int, duration: Int): SemToast {
            return makeText(context, context.getResources().getText(resId), duration)
        }
    }
}

@Deprecated("Use SemToast instead", ReplaceWith("SemToast"), DeprecationLevel.WARNING)
typealias Toast = SemToast
