@file:Suppress("NOTHING_TO_INLINE")
@file:JvmName("BadgeUtil")

package dev.oneuiproject.oneui.utils

import androidx.annotation.RestrictTo
import dev.oneuiproject.oneui.layout.Badge
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
inline fun String.badgeTextToCount() = this.toIntOrNull() ?: 0

@RestrictTo(RestrictTo.Scope.LIBRARY)
inline fun String?.toBadge(): Badge {
    return this?.badgeTextToCount()?.let {
        if (it > 0) Badge.NUMERIC(it) else Badge.DOT
    } ?: Badge.NONE
}

const val BADGE_LIMIT_NUMBER = 99

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
internal const val N_BADGE = -1