@file:Suppress("NOTHING_TO_INLINE")

package dev.oneuiproject.oneui.recyclerview.ktx
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import dev.oneuiproject.oneui.recyclerview.model.AdapterItem

/**
 * Registers a long-press multi-selection listener.
 *
 * @param onStarted Function called when a long multi-selection session has [started][RecyclerView.seslStartLongPressMultiSelection].
 *
 * @param onItemSelected Function called when a `selectable` [item][AdapterItem] is selected.
 *
 * @param onEnded Function called when the current long-press multi-selection has ended.
 *
 * @param isSelectable Function use to check if an [item][AdapterItem] is allowed to be selected.
 * [onItemSelected] will only be  invoked for an item when this returns true.
 * This by default always returns `true`.
 */
inline fun RecyclerView.doOnLongPressMultiSelection(
    crossinline onStarted: () -> Unit,
    crossinline onItemSelected: (selectedItem: AdapterItem) -> Unit,
    crossinline onEnded: () -> Unit,
    crossinline isSelectable: (RecyclerView, item: AdapterItem) -> Boolean =
        {_, _ -> true }
) {
    seslSetLongPressMultiSelectionListener(
        object : RecyclerView.SeslLongPressMultiSelectionListener {

            override fun onItemSelected(
                view: RecyclerView,
                child: View,
                position: Int,
                id: Long
            ) {
                val item = AdapterItem(child, position, id)
                if (!isSelectable(view, item)) return
                onItemSelected.invoke(item)
            }

            override fun onLongPressMultiSelectionStarted(x: Int, y: Int) {
                onStarted.invoke()
                val child = findChildViewUnder(x.toFloat(), y.toFloat())
                if (child != null) {
                    val position = getChildAdapterPosition(child)
                    val itemId = getChildItemId(child)
                    val item = AdapterItem(child, position, itemId)
                    if (!isSelectable(this@doOnLongPressMultiSelection, item)) return
                    onItemSelected.invoke(item)
                }
            }

            override fun onLongPressMultiSelectionEnded(x: Int, y: Int) {
                onEnded.invoke()
            }
        })
}