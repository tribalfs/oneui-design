package dev.oneuiproject.oneui.utils

import android.content.Context
import android.graphics.Color
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned.SPAN_MARK_MARK
import android.text.style.ForegroundColorSpan
import android.text.style.TextAppearanceSpan
import androidx.annotation.ColorInt
import androidx.core.text.clearSpans
import dev.oneuiproject.oneui.design.R
import java.util.StringTokenizer

/**
 * A Util class intended for highlighting text based on a search string.
 *
 * @param context Provides the ability to set highlight color to context theme's colorPrimaryDark.
 * @param highlightColor Set a custom color to override default highlight color
 *
 * if neither context nor custom highlightColor is provided, the color will be set to #2196F3.
 *
 */
class SearchHighlighter @JvmOverloads constructor(@JvmField val context: Context? = null,
                        @JvmField @ColorInt var highlightColor: Int = -1) {

    init {
        if (context == null && highlightColor == -1) {
            highlightColor =  Color.parseColor("#2196F3")
        }
    }

    /**
     * Highlights the occurrences of the query tokens in the provided [SpannableString].
     *
     * @param spannable The [SpannableString] to apply highlighting to.
     * @param searchString The search string containing terms to highlight.
     * @return The [SpannableString] with highlights applied.
     */
    operator fun invoke(
        spannable: SpannableString,
        searchString: String
    ): SpannableString {
        if (searchString.isEmpty()) return spannable
        applyHighlighting(spannable, StringTokenizer(searchString))
        return spannable
    }

    /**
     * Highlights the occurrences of the query tokens in the provided [SpannableString].
     *
     * @param spannable The [SpannableStringBuilder] to apply highlighting to.
     * @param searchString The search string containing terms to highlight.
     * @return The [SpannableString] with highlights applied.
     */
    operator fun invoke(
        spannable: SpannableStringBuilder,
        searchString: String
    ): SpannableStringBuilder {
        if (searchString.isEmpty()) return spannable
        applyHighlighting(spannable, StringTokenizer(searchString))
        return spannable
    }

    /**
     * Highlights the occurrences of the query tokens in the provided text.
     *
     * @param text The [CharSequence] to search and highlight.
     * @param searchString The search string containing terms to highlight.
     * @return A [SpannableString] with highlights applied.
     */
    operator fun invoke(
        text: CharSequence,
        searchString: String
    ): SpannableString {
        val spannable = SpannableString(text)
        if (searchString.isEmpty()) return spannable
        applyHighlighting(spannable, StringTokenizer(searchString))
        return spannable
    }

    /**
     * Highlights the occurrences of the query tokens in the provided text.
     *
     * @param text The [CharSequence] to search and highlight.
     * @param searchTokens The search StringTokenizer containing terms to highlight.
     * @return A [SpannableString] with highlights applied.
     */
    operator fun invoke(
        text: CharSequence,
        searchTokens: StringTokenizer
    ): SpannableString {
        val spannable = SpannableString(text)
        applyHighlighting(spannable, searchTokens)
        return spannable
    }

    /**
     * Applies highlighting to the provided [Spannable] based on the search query.
     *
     * @param spannableString The [Spannable] to apply highlighting to.
     * @param searchTokens The StringTokenizer containing terms to highlight.
     */
    private fun applyHighlighting(
        spannableString: Spannable,
        searchTokens: StringTokenizer
    ) {
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

                context?.let {
                    spannableString.setSpan(
                        TextAppearanceSpan(it, R.style.OneUI_SearchHighlightedTextAppearance), offsetStart,
                        offsetEnd, SPAN_MARK_MARK)
                }

                if (highlightColor != -1) {
                    spannableString.setSpan(ForegroundColorSpan(highlightColor), offsetStart, offsetEnd, SPAN_MARK_MARK)
                }

                remainingString = remainingString.substring(length)
                if (remainingString.indexOf(nextToken, ignoreCase = true) != -1) {
                    break
                }
            } while (offsetEnd < MAX_OFFSET)
        }
    }

    companion object{
        const val MAX_OFFSET = 200
    }

}