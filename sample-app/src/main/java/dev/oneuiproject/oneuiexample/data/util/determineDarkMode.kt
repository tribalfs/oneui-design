package dev.oneuiproject.oneuiexample.data.util

fun determineDarkMode(darkModeValue: String, darkModeAutoValue: Boolean): DarkMode {
    return if (darkModeAutoValue) {
        DarkMode.AUTO
    } else {
        when (darkModeValue) {
            "0" -> DarkMode.DISABLED
            else -> DarkMode.ENABLED
        }
    }
}

enum class DarkMode {
    AUTO,
    DISABLED,
    ENABLED
}