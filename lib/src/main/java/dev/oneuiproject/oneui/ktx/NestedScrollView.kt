@file:Suppress("NOTHING_TO_INLINE")

package dev.oneuiproject.oneui.ktx

import androidx.core.widget.NestedScrollView

inline fun NestedScrollView.seslSetScrollbarVerticalPadding(verticalPadding: Int){
    seslSetScrollbarVerticalPadding(verticalPadding, verticalPadding)
}