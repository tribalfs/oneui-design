@file:Suppress("NOTHING_TO_INLINE")

package dev.oneuiproject.oneui.utils

import android.graphics.Color
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned.SPAN_MARK_MARK
import android.text.style.ForegroundColorSpan
import androidx.annotation.ColorInt
import androidx.core.text.clearSpans
import java.util.StringTokenizer

class SearchHighlighter {

    @JvmField
    @ColorInt
    var highlightColor = Color.parseColor("#2196F3")

    inline operator fun invoke(
        spannableStringBuilder: SpannableStringBuilder,
        query: String,
    ): SpannableStringBuilder {
        if (query.isEmpty()) return spannableStringBuilder
        return invoke(spannableStringBuilder, StringTokenizer(query))
    }

    inline operator fun invoke(
        spannableStringBuilder: SpannableStringBuilder,
        searchTokens: StringTokenizer,
    ): SpannableStringBuilder {
        spannableStringBuilder.clearSpans()
        while (searchTokens.hasMoreTokens()) {
            val nextToken = searchTokens.nextToken()
            var remainingString = spannableStringBuilder.toString()
            var offsetEnd = 0
            do {
                val index = remainingString.indexOf(nextToken, ignoreCase = true)
                if (index < 0) break
                val length = index + nextToken.length
                val offsetStart = index + offsetEnd
                offsetEnd += length
                spannableStringBuilder.setSpan(ForegroundColorSpan(highlightColor), offsetStart, offsetEnd, SPAN_MARK_MARK)
                remainingString = remainingString.substring(length)
                if (remainingString.indexOf(nextToken, ignoreCase = true) != -1) {
                    break
                }
            } while (offsetEnd < MAX_OFFSET)
        }
        return spannableStringBuilder
    }

    inline operator fun invoke(
        spannableString: SpannableString,
        query: String,
    ): SpannableString {
        if (query.isEmpty()) return spannableString
        return invoke(spannableString, StringTokenizer(query))
    }

    inline operator fun invoke(
        spannableString: SpannableString,
        searchTokens: StringTokenizer,
    ): SpannableString {
        spannableString.clearSpans()
        while (searchTokens.hasMoreTokens()) {
            val nextToken = searchTokens.nextToken()
            var remainingString = spannableString.toString()
            var offsetEnd = 0
            do {
                val index = remainingString.indexOf(nextToken, ignoreCase = true)
                if (index < 0) break
                val length = index + nextToken.length
                val offsetStart = index + offsetEnd
                offsetEnd += length
                spannableString.setSpan(ForegroundColorSpan(highlightColor), offsetStart, offsetEnd, SPAN_MARK_MARK)
                remainingString = remainingString.substring(length)
                if (remainingString.indexOf(nextToken, ignoreCase = true) != -1) {
                    break
                }
            } while (offsetEnd < MAX_OFFSET)
        }
        return spannableString
    }

    inline operator fun invoke(
        textToSearch: CharSequence,
        query:  String,
    ): SpannableString {
        return invoke(SpannableString(textToSearch), StringTokenizer(query))
    }

    companion object{
         const val MAX_OFFSET = 200
    }

}