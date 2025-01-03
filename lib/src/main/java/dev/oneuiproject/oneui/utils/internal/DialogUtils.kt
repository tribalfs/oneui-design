@file:Suppress("NOTHING_TO_INLINE")

package dev.oneuiproject.oneui.utils.internal

import android.app.Dialog
import android.content.res.Configuration.ORIENTATION_PORTRAIT
import android.util.TypedValue
import androidx.annotation.RestrictTo
import dev.oneuiproject.oneui.ktx.dpToPx
import dev.oneuiproject.oneui.ktx.getThemeAttributeValue

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@JvmOverloads
inline fun Dialog.updateWidth(centerX: Int? = null){
    val dialogWidth = semGetDialogWidth()
    val dialogWindowLp = this.window!!.attributes
    dialogWindowLp.width = dialogWidth
    centerX?.let {
        dialogWindowLp.x = it - (dialogWidth / 2)
    }
    this.window!!.setAttributes(dialogWindowLp)
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun Dialog.semGetDialogWidth(): Int {
    val context= context
    val res = context.resources
    val config = res.configuration
    val displayMetrics = res.displayMetrics
    val screenWidth =  config.screenWidthDp.dpToPx(displayMetrics.density).toFloat()
    val minWidth = context.getThemeAttributeValue(
        if(config.orientation == ORIENTATION_PORTRAIT) {
            android.R.attr.windowMinWidthMinor
        } else android.R.attr.windowMinWidthMajor
    )!!
    val min = when (minWidth.type) {
        TypedValue.TYPE_DIMENSION -> minWidth.getDimension(displayMetrics).toInt()
        TypedValue.TYPE_FRACTION -> minWidth.getFraction(screenWidth, screenWidth).toInt()
        else -> 0
    }

    return min
}




