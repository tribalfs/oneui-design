package dev.oneuiproject.oneui.utils

import android.graphics.Typeface
import android.os.Build

fun getBoldFont(): Typeface {
    if (Build.VERSION.SDK_INT >= 34){
        val family = Typeface.create("sec", Typeface.NORMAL)
        return Typeface.create(family, 600, false);
    }else {
        return Typeface.create("sec-roboto-light", Typeface.BOLD)
    }
}

fun getNormalFont(): Typeface {
    if (Build.VERSION.SDK_INT >= 34){
        val family = Typeface.create("sec", Typeface.NORMAL);
        return Typeface.create(family, 400, false)
    }else {
        return Typeface.create("sec-roboto-light", Typeface.NORMAL)
    }
}