package dev.oneuiproject.oneui.ktx

inline fun <T> List<T>.findWithIndex(predicate: (T) -> Boolean, onFound: (index: Int, item: T) -> Unit): Boolean {
    for (index in indices) {
        val item = this[index]
        if (predicate(item)) {
            onFound(index, item)
            return true
        }
    }
    return false
}
