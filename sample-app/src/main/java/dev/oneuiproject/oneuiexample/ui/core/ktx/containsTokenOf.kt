package dev.oneuiproject.oneuiexample.ui.core.ktx

import java.util.StringTokenizer

fun String.containsTokenOf(query: String): Boolean{
    val tokenizer = StringTokenizer(query)
    while (tokenizer.hasMoreTokens()) {
        if (!this.contains(tokenizer.nextToken(),true)) {
            return false
        }
    }
    return true
}

