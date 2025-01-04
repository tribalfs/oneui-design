package dev.oneuiproject.oneui.ktx

import java.util.StringTokenizer

inline fun <T: CharSequence>T.containsTokenOf(query: String): Boolean{
    val tokenizer = StringTokenizer(query)
    while (tokenizer.hasMoreTokens()) {
        if (!this.contains(tokenizer.nextToken(),true)) {
            return false
        }
    }
    return true
}


inline fun <T: CharSequence>T.isNumericValue(): Boolean {
    try {
        toString().toInt()
        return true
    } catch (e: NumberFormatException) {
        return false
    }
}