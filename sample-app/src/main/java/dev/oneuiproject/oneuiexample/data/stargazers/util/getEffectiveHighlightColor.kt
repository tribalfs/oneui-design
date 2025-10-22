package dev.oneuiproject.oneuiexample.data.stargazers.util

fun getEffectiveHighlightColor(isCustomHighlightColor: Boolean, customColor: Int): Int =
        if (isCustomHighlightColor) customColor else -1//DEFAULT
