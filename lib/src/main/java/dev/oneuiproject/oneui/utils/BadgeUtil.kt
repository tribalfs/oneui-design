@file:Suppress("NOTHING_TO_INLINE")

package dev.oneuiproject.oneui.utils

import androidx.annotation.RestrictTo
import androidx.core.text.isDigitsOnly
import java.text.NumberFormat
import java.util.Locale

/**
 * Formats into localized badge count string. Also takes care of limiting to [BADGE_LIMIT_NUMBER].
 * Value < 0 returns null.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
inline fun Int.badgeCountToText(): String?{
    return if (this <= 0) null else {
        NumberFormat.getInstance(Locale.getDefault()).format(this.coerceAtMost(BADGE_LIMIT_NUMBER))
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY)
inline fun String.badgeTextToCount(): Int{
    return if (this.isDigitsOnly()) {
        this.toInt()
    }else 0
}

const val BADGE_LIMIT_NUMBER = 99

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
internal const val N_BADGE = -1