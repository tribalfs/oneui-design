package dev.oneuiproject.oneui.ktx

import android.os.Build
import android.view.View
import android.widget.SectionIndexer
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


/**
 * Syntactic sugar equivalent to calling:
 *
 * ```
 * RecyclerView.apply{
 *     seslSetFillBottomEnabled(true)
 *     seslSetLastRoundedCorner(true)
 *     seslSetFastScrollerEnabled(true)
 *     seslSetGoToTopEnabled(true)
 *     seslSetSmoothScrollEnabled(true)
 *     seslSetIndexTipEnabled(true)
 * }
 * ```
 */
inline fun RecyclerView.enableCoreSeslFeatures(
    fillBottom:Boolean = true,
    lastRoundedCorner:Boolean = true,
    fastScrollerEnabled:Boolean = true,
    goToTopEnabled:Boolean = true,
    smoothScrollEnabled:Boolean = true,
    indexTipEnabled: Boolean = adapter is SectionIndexer
){
    if (fillBottom) seslSetFillBottomEnabled(true)
    if (lastRoundedCorner) seslSetLastRoundedCorner(true)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && fastScrollerEnabled) {
        seslSetFastScrollerEnabled(true)
    }
    if (goToTopEnabled) seslSetGoToTopEnabled(true)
    if (smoothScrollEnabled) seslSetSmoothScrollEnabled(true)
    if (indexTipEnabled) seslSetIndexTipEnabled(true)
}

