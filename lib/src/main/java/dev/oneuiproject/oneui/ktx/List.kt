@file:JvmName("ListUtil")
package dev.oneuiproject.oneui.ktx

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@OptIn(ExperimentalContracts::class)
inline fun <T> List<T>.findWithIndex(predicate: (T) -> Boolean, onFound: (index: Int, item: T) -> Unit): Boolean {
    contract {
        callsInPlace(predicate)
        callsInPlace(onFound, InvocationKind.AT_MOST_ONCE)
    }
    for (index in indices) {
        val item = this[index]
        if (predicate(item)) {
            onFound(index, item)
            return true
        }
    }
    return false
}
