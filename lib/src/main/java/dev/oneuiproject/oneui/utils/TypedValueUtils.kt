package dev.oneuiproject.oneui.utils

import android.content.Context
import android.util.TypedValue
import android.util.TypedValue.COMPLEX_UNIT_MM
import android.util.TypedValue.COMPLEX_UNIT_PX
import androidx.annotation.AnyRes

object TypedValueUtils {
    private val sTypedValue = TypedValue()

    fun getFloat(context: Context,  @AnyRes resId: Int): Float {
        val resources = context.resources
        val typedValue = sTypedValue
        resources.getValue(resId, typedValue, true)
        return typedValue.float
    }

    /**
     * Checks if the given index is a valid dimension index.
     *
     * @param index The index to check.
     * @return True if the index is valid, false otherwise.
     */
    private fun isTypeDimension(index: Int): Boolean {
        return index in COMPLEX_UNIT_PX .. COMPLEX_UNIT_MM
    }

    fun getFloat(context: Context, resId: Int, unit: Int): Float {
        val value = getFloat(context, resId)
        return if (isTypeDimension(unit)) TypedValue.applyDimension(
            unit,
            value,
            context.resources.displayMetrics
        ) else value
    }
}