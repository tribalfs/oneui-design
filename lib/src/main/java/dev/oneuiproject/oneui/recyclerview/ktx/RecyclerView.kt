@file:Suppress("NOTHING_TO_INLINE")

package dev.oneuiproject.oneui.recyclerview.ktx
import android.view.View
import androidx.collection.MutableScatterSet
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.NO_ID
import androidx.recyclerview.widget.RecyclerView.NO_POSITION
import dev.oneuiproject.oneui.recyclerview.model.AdapterItem
import kotlin.math.max
import kotlin.math.min

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


/**
 * Registers a multi-selection listener for block selection started by an S-Pen or mouse input.
 *
 * @param onStarted Lambda to be invoked when block selection has started.
 *
 * @param onCompleted Lambda to be invoked when block selection has completed.
 * This includes the resulting `selectedItems` parameter - the set of [items][AdapterItem] selected
 * after filtering out those which are not allowed to be selected.
 *
 * **Note**: The start and end positions are inclusive and can be the same
 * or different (which one is larger depends on the direction of the selection).
 *
 * @param isSelectable Lambda to be invoked to check if an [item][AdapterItem] is
 * allowed to be selected. Default always returns `true`.
 *
 * - **Important note**: during block selection, some items may be off-screen so their child view can be null.
 * In such cases, AdapterItem.itemView is null and the computed id will be RecyclerView.NO_ID.
 * If your isSelectable logic relies on a stable id or a non-null view, account for these cases accordingly.
 */
inline fun RecyclerView.doOnBlockMultiSelection(
    crossinline onStarted: () -> Unit,
    crossinline onCompleted: (selectedItems: Set<AdapterItem>) -> Unit,
    crossinline isSelectable: (RecyclerView, AdapterItem) -> Boolean = { _, _ -> true },
) {
    seslSetOnMultiSelectedListener(
        object : RecyclerView.SeslOnMultiSelectedListener {

            private var startPosition: Int = NO_POSITION
            private var endPosition: Int = NO_POSITION

            override fun onMultiSelectStart(startX: Int, startY: Int) {
                val fStartX = startX.toFloat()
                val fStartY = startY.toFloat()
                val child = findChildViewUnder(fStartX, fStartY)
                    ?: seslFindNearChildViewUnder(fStartX, fStartY) ?: return
                startPosition = getChildLayoutPosition(child)
                onStarted()
            }

            override fun onMultiSelectStop(endX: Int, endY: Int) {
                val fEndX = endX.toFloat()
                val fEndY = endY.toFloat()
                val child = findChildViewUnder(fEndX, fEndY)
                    ?: seslFindNearChildViewUnder(fEndX, fEndY) ?: return
                endPosition = getChildLayoutPosition(child)
                onCompleted.invoke(getSelectedItems())
            }

            override fun onMultiSelected(
                view: RecyclerView,
                child: View?,
                position: Int,
                id: Long
            ) {} //not implemented in sesl recyclerview

            private fun getSelectedItems(): Set<AdapterItem> {
                val top = min(startPosition, endPosition).coerceAtLeast(0)
                val bottom = max(startPosition, endPosition).coerceAtMost(adapter!!.itemCount - 1)
                val selectedItems = MutableScatterSet<AdapterItem>(bottom - top + 1)

                if (getLayoutManager() is GridLayoutManager) {
                    val glm = getLayoutManager() as GridLayoutManager?
                    val spanCount = glm!!.getSpanCount()
                    val adapterCount = adapter?.getItemCount() ?: 0

                    if (spanCount <= 0 || adapterCount <= 0 ) {
                        // Nothing to do
                    } else {
                        // Compute logical (row, col) for both corners.
                        val startRow: Int = top / spanCount
                        val startCol: Int = top % spanCount
                        val endRow: Int = bottom / spanCount
                        val endCol: Int = bottom % spanCount

                        val minRow = min(startRow, endRow)
                        val maxRow = max(startRow, endRow)
                        val minCol = min(startCol, endCol)
                        val maxCol = max(startCol, endCol)

                        // Build the full rectangle in row/col space, including off-screen positions.
                        // Support custom span sizes via SpanSizeLookup by iterating logical rows
                        // and mapping to adapter positions whose (row, col) fall within bounds.
                        val spanLookup = glm.spanSizeLookup
                        var currentPos = 0
                        var currentRow = 0
                        while (currentPos < adapterCount && currentRow <= maxRow) {
                            // compute columns for the current row by simulating span accumulation
                            var usedSpan = 0
                            var col = 0
                            while (currentPos < adapterCount && usedSpan < spanCount) {
                                val itemSpan = spanLookup.getSpanSize(currentPos).coerceAtLeast(1)
                                val startsNewRow = usedSpan + itemSpan > spanCount
                                if (startsNewRow) {
                                    // move to next row
                                    break
                                }

                                if (currentRow in minRow..maxRow && col in minCol..maxCol) {
                                    val pos = currentPos
                                    val child = findViewHolderForLayoutPosition(pos)?.itemView
                                    val item = AdapterItem(child, pos, child?.let { getChildItemId(it) } ?: NO_ID)
                                    if (isSelectable(this@doOnBlockMultiSelection, item)) {
                                        selectedItems.add(item)
                                    }
                                }

                                usedSpan += itemSpan
                                col++
                                currentPos++
                            }
                            // if row not fully filled but next item doesn't fit, keep same position but advance row
                            if (currentPos < adapterCount) {
                                // if we broke due to overflow, keep position; else we're at end of row already
                                // advance to next row boundary if not already at boundary
                                while (currentPos < adapterCount && spanLookup.getSpanSize(currentPos) > spanCount) {
                                    // guard against invalid span sizes; skip
                                    currentPos++
                                }
                            }
                            currentRow++
                        }
                    }
                } else {
                    for (pos in top until  bottom + 1) {
                        val child = findViewHolderForLayoutPosition(pos)?.itemView
                        val item = AdapterItem(child, pos, child?.let {getChildItemId(it)} ?: NO_ID)
                        if (isSelectable(this@doOnBlockMultiSelection, item)) {
                            selectedItems.add(item)
                        }
                    }
                }
                return selectedItems.asSet()
            }
        }
    )
}
