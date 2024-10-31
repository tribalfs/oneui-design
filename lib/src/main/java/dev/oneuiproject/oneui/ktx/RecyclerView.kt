package dev.oneuiproject.oneui.ktx

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.NO_POSITION

/**
 * Registers a long-press multi-selection listener.
 *
 * @param onItemSelected Lambda to be invoked when an item is selected that includes the following parameters:
 * `position` - the layout position of the selected item.
 * `id`- the stable ID of the selected item. Returns [RecyclerView.NO_ID] when a stable ID is not implemented.
 *
 * @param onStateChanged Lambda to be invoked when the selection state changes that includes the following parameters:
 * `state` - either [MultiSelectionState.STARTED] or [MultiSelectionState.ENDED].
 *`position` - the layout position of the selected item when [MultiSelectionState.STARTED].
 * This is always set to [RecyclerView.NO_POSITION] when [MultiSelectionState.ENDED].
 *
 */
inline fun RecyclerView.doOnLongPressMultiSelection (
    crossinline onItemSelected: (position: Int,
                                 id: Long) -> Unit,
    crossinline onStateChanged: (state: MultiSelectionState,
                                 position: Int) -> Unit = { _, _ -> },
){
    seslSetLongPressMultiSelectionListener(
        object : RecyclerView.SeslLongPressMultiSelectionListener {
            override fun onItemSelected(
                view: RecyclerView,
                child: View,
                position: Int,
                id: Long
            ) {
                onItemSelected.invoke(position, id)
            }

            override fun onLongPressMultiSelectionStarted(x: Int, y: Int) {
                val child = findChildViewUnder(x.toFloat(), y.toFloat())
                onStateChanged.invoke(
                    MultiSelectionState.STARTED,
                    child?.let { getChildLayoutPosition(it) } ?: NO_POSITION
                )
            }
            override fun onLongPressMultiSelectionEnded(x: Int, y: Int) {
                onStateChanged.invoke(
                    MultiSelectionState.ENDED,
                    NO_POSITION
                )
            }
        })
}

enum class MultiSelectionState{
    STARTED,
    ENDED
}

