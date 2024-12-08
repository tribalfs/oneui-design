@file:Suppress("NOTHING_TO_INLINE")

package dev.oneuiproject.oneui.layout.internal.util

import dev.oneuiproject.oneui.layout.DrawerLayout.DrawerState

inline fun getDrawerStateUpdate(previousOffset: Float, currentOffset: Float) =
    when (currentOffset) {
        1f -> DrawerState.OPEN
        0f -> DrawerState.CLOSE
        else -> {
            when {
                currentOffset > previousOffset -> DrawerState.OPENING
                else -> DrawerState.CLOSING
            }
        }
    }