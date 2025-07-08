package dev.oneuiproject.oneuiexample.ui.main.core.util

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.core.graphics.createBitmap

fun Activity.captureScreenBitmap(): Bitmap {
    val rootView = window.decorView.rootView
    val bitmap = createBitmap(rootView.width, rootView.height)
    val canvas = Canvas(bitmap)
    rootView.draw(canvas)
    return bitmap
}