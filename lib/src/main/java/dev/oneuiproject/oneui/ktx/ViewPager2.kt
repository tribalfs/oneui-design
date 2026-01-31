package dev.oneuiproject.oneui.ktx

import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2

fun ViewPager2.disablePenSelection() {
    val childAt = getChildAt(0)
    (childAt as? RecyclerView)?.seslSetPenSelectionEnabled(false)
}

fun ViewPager2.blockHoverScroll() {
    val childAt = getChildAt(0)
    (childAt as? RecyclerView)?.seslSetHoverScrollEnabled(false)
}

fun ViewPager2.disableSeslRecoil() {
    val childAt = getChildAt(0)
    (childAt as? RecyclerView)?.seslSetRecoilEnabled(false)
}

