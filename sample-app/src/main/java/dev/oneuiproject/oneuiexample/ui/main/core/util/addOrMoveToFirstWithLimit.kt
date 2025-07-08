package dev.oneuiproject.oneuiexample.ui.main.core.util

fun <T> MutableList<T>.addOrMoveToFirstWithLimit(element: T, limit: Int) {
    remove(element)
    add(0, element)
    if (size > limit) {
        subList(limit, size).clear()
    }
}

