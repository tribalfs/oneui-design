package dev.oneuiproject.oneuiexample.ui.main.core.util

import android.content.Context
import android.widget.Toast
import dev.oneuiproject.oneui.widget.Toast as SemToast

@JvmOverloads
inline fun Context.semToast(msg: String, length: Int = Toast.LENGTH_SHORT, onCreate: Toast.() -> Unit = {})  =
    SemToast.makeText(this, msg, length).apply { onCreate(); show() }