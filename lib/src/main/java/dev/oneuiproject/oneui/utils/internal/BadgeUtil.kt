package dev.oneuiproject.oneui.utils.internal

/**
 * Formats into localized badge count string. Also takes care of limiting to [BADGE_LIMIT_NUMBER].
 * Value < 0 returns null.
 */
@androidx.annotation.RestrictTo(androidx.annotation.RestrictTo.Scope.LIBRARY)
internal inline fun Int.badgeCountToText(): String?{
    return if (this <= 0) null else {
        java.text.NumberFormat.getInstance(java.util.Locale.getDefault()).format(this.coerceAtMost(BADGE_LIMIT_NUMBER))
    }
}

@androidx.annotation.RestrictTo(androidx.annotation.RestrictTo.Scope.LIBRARY)
internal inline fun String.badgeTextToCount() = this.toIntOrNull() ?: 0

@androidx.annotation.RestrictTo(androidx.annotation.RestrictTo.Scope.LIBRARY)
internal inline fun String?.toBadge(): dev.oneuiproject.oneui.layout.Badge {
    return this?.badgeTextToCount()?.let {
        if (it > 0) dev.oneuiproject.oneui.layout.Badge.NUMERIC(it) else dev.oneuiproject.oneui.layout.Badge.DOT
    } ?: dev.oneuiproject.oneui.layout.Badge.NONE
}

internal const val BADGE_LIMIT_NUMBER = 99

@androidx.annotation.RestrictTo(androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
internal const val N_BADGE = -1