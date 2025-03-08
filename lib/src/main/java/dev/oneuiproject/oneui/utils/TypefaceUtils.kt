@file:Suppress("NOTHING_TO_INLINE")

package dev.oneuiproject.oneui.utils

import android.graphics.Typeface
import android.os.Build

fun getBoldFont(): Typeface {
    if (Build.VERSION.SDK_INT >= 34){
        val family = Typeface.create("sec", Typeface.NORMAL)
        return Typeface.create(family, 700, false)
    }else {
        return Typeface.create("roboto", Typeface.BOLD)
    }
}

fun getSemiBoldFont(): Typeface {
    if (Build.VERSION.SDK_INT >= 34){
        val family = Typeface.create("sec", Typeface.NORMAL)
        return Typeface.create(family, 600, false)
    }else {
        //Medium
        return Typeface.create("sec-roboto-light", Typeface.BOLD)
    }
}

@Deprecated("Use getRegularFont instead", ReplaceWith("getRegularFont()"))
inline fun getNormalFont() = getRegularFont()

fun getRegularFont(): Typeface {
    if (Build.VERSION.SDK_INT >= 34){
        val family = Typeface.create("sec", Typeface.NORMAL)
        return Typeface.create(family, 400, false)
    }else {
        return Typeface.create("sec-roboto-light", Typeface.NORMAL)
    }
}

fun getLightFont(): Typeface {
    if (Build.VERSION.SDK_INT >= 34){
        val family = Typeface.create("sec", Typeface.NORMAL)
        return Typeface.create(family, 300, false)
    }else {
        return Typeface.create("sans-serif-light", Typeface.NORMAL)
    }
}