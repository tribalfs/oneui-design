package dev.oneuiproject.oneui.ktx

import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner

/**
 * Sets a listener to handle item selection events on this [Spinner].
 *
 * @param onSelect Lambda function to be invoked when an item is selected or nothing is selected.
 *                 Receives the `parent` AdapterView, the `view` selected, `position` of the selected item,
 *                 and the row id of the selected item (or null if nothing is selected).
 *                 If no item is selected, `position` and `id` parameters will be null.
 * @return The Spinner instance to allow for chaining calls.
 *
 * Example usage:
 * ```
 * spinner.onSpinnerItemSelected { parent, view, position, id ->
 *     if (position != null) {
 *         // Handle item selection
 *     } else {
 *         // Handle no item selected
 *     }
 * }
 * ```
 */
@JvmName("onSpinnerItemSelected")
inline fun <T : Spinner> T.onItemSelected(
    crossinline onSelect: (position: Int?, id: Long?) -> Unit
): T {
    onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
            onSelect(position, id)
        }

        override fun onNothingSelected(parent: AdapterView<*>?) {
            onSelect(null, null)
        }
    }
    return this
}

@JvmName("setupSpinner")
inline fun <T : Spinner> T.setEntries(
    entries: List<Any>,
    crossinline onSelect: (position: Int?, id: Long?) -> Unit
){
    adapter = ArrayAdapter(
        context,
        android.R.layout.simple_spinner_item,
        entries
    ).apply {
        setDropDownViewResource(androidx.appcompat.R.layout.support_simple_spinner_dropdown_item)
    }
    onItemSelected {position, id ->
        onSelect.invoke(position, id)
    }
}