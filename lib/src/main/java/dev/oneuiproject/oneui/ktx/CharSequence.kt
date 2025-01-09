@file:Suppress("NOTHING_TO_INLINE")

package dev.oneuiproject.oneui.ktx

import java.util.StringTokenizer

/**
 * Checks if this [CharSequence] contains all tokens of the given [query].
 */
@JvmName("containsAllTokens")
inline fun <T: CharSequence>T.containsAllTokensOf(query: String): Boolean{
    val tokenizer = StringTokenizer(query)
    while (tokenizer.hasMoreTokens()) {
        if (!this.contains(tokenizer.nextToken(),true)) {
            return false
        }
    }
    return true
}

/**
 * Checks if this [CharSequence] contains all tokens of the given [query].
 */
@Deprecated("This is a misnomer of containsAllTokensOf", ReplaceWith("containsAllTokensOf(query)"))
inline fun <T: CharSequence>T.containsTokenOf(query: String) = containsAllTokensOf(query)


inline fun <T: CharSequence>T.isNumericValue(): Boolean {
    try {
        toString().toInt()
        return true
    } catch (e: NumberFormatException) {
        return false
    }
}