package dev.oneuiproject.oneuiexample.ui.main.core.util

import android.app.Activity
import android.view.View
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar


inline fun Activity.suggestiveSnackBar(
    msg: String,
    view: View? = null,
    duration: Int = Snackbar.LENGTH_INDEFINITE,
    onCreate: (Snackbar.() -> Unit) = {}
) =
    Snackbar.make(view ?: findViewById(android.R.id.content),msg, duration,
        Snackbar.SESL_SNACKBAR_TYPE_SUGGESTION).apply { onCreate(); show() }


inline fun Fragment.suggestiveSnackBar(
    msg: String,
    view: View? = null,
    duration: Int = Snackbar.LENGTH_INDEFINITE,
    onCreate: (Snackbar.() -> Unit) = {}) =
    requireActivity().suggestiveSnackBar(msg, view, duration, onCreate)