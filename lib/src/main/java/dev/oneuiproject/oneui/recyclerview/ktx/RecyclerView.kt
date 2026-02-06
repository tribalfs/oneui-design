@file:Suppress("NOTHING_TO_INLINE")

package dev.oneuiproject.oneui.recyclerview.ktx

import android.app.Activity
import android.view.View
import android.widget.SectionIndexer
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.annotation.Px
import androidx.annotation.RequiresApi
import androidx.appcompat.content.res.AppCompatResources
import androidx.collection.MutableScatterSet
import androidx.core.graphics.toColorInt
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.NO_ID
import androidx.recyclerview.widget.RecyclerView.NO_POSITION
import dev.oneuiproject.oneui.recyclerview.model.AdapterItem
import androidx.recyclerview.widget.RecyclerView.OnScrollListener
import androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_DRAGGING
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.recyclerview.widget.SeslSwipeListAnimator
import androidx.recyclerview.widget.SeslSwipeListAnimator.SwipeConfiguration
import dev.oneuiproject.oneui.delegates.SwipeActionListener
import dev.oneuiproject.oneui.delegates.SwipeItemCallbackDelegate
import dev.oneuiproject.oneui.design.R
import dev.oneuiproject.oneui.ktx.activity
import dev.oneuiproject.oneui.ktx.doOnAttachedStateChanged
import dev.oneuiproject.oneui.ktx.dpToPx
import dev.oneuiproject.oneui.ktx.hideSoftInput
import dev.oneuiproject.oneui.layout.ToolbarLayout
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
 *     seslSetIndexTipEnabled(adapter is SectionIndexer)
 *     seslSetFillHorizontalPaddingEnabled(true)
 * }
 * ```
 */
@JvmOverloads
inline fun RecyclerView.enableCoreSeslFeatures(
    fillBottom: Boolean = true,
    lastRoundedCorner: Boolean = true,
    fastScrollerEnabled: Boolean = true,
    goToTopEnabled: Boolean = true,
    smoothScrollEnabled: Boolean = true,
    indexTipEnabled: Boolean = adapter is SectionIndexer,
    fillHorizontalPadding: Boolean = true
) {
    if (fillBottom) seslSetFillBottomEnabled(true)
    // This is already enabled by default
    if (!lastRoundedCorner) seslSetLastRoundedCorner(false)
    if (fastScrollerEnabled) seslSetFastScrollerEnabled(true)
    if (goToTopEnabled) seslSetGoToTopEnabled(true)
    if (smoothScrollEnabled) seslSetSmoothScrollEnabled(true)
    if (indexTipEnabled) seslSetIndexTipEnabled(true)
    if (fillHorizontalPadding) seslSetFillHorizontalPaddingEnabled(true)
}

/**
 * Configures swipe animation functionality for a [RecyclerView] using Samsung's SESL Swipe List Animator.
 *
 * @param leftToRightLabel Text label to display when swiping items from left to right. Default is an empty string.
 * @param rightToLeftLabel Text label to display when swiping items from right to left. Default is an empty string.
 * @param leftToRightColor Color of the swipe indicator for left-to-right swipe. Default is unset (-1).
 * @param rightToLeftColor Color of the swipe indicator for right-to-left swipe. Default is unset (-1).
 * @param leftToRightDrawableRes Drawable resource ID for the swipe indicator icon when swiping from left to right. Default is null.
 * @param rightToLeftDrawableRes Drawable resource ID for the swipe indicator icon when swiping from right to left. Default is null.
 * @param drawablePadding Padding between the swipe indicator icon and the swipe label text. Default is 20dp converted to pixels.
 * @param textColor Color of the swipe label text. Default is unset (-1).
 * @param onSwiped Lambda function to be invoked when an item is swiped. Return true to dismiss the item, false to cancel. Receives position, direction, and action state.
 * @param isLeftSwipeEnabled Lambda function to determine if left swipe is enabled for a specific view holder. Default always returns true.
 * @param isRightSwipeEnabled Lambda function to determine if right swipe is enabled for a specific view holder. Default always returns true.
 */
inline fun RecyclerView.configureItemSwipeAnimator(
    leftToRightLabel: String = "",
    rightToLeftLabel: String = "",
    @ColorInt
    leftToRightColor: Int = -1,//UNSET_VALUE
    @ColorInt
    rightToLeftColor: Int = -1,
    @DrawableRes
    leftToRightDrawableRes: Int? = null,
    @DrawableRes
    rightToLeftDrawableRes: Int? = null,
    @Px
    drawablePadding: Int = 20.dpToPx(resources),
    @ColorInt
    textColor: Int = -1,
    crossinline onSwiped: (position: Int, direction: Int, actionState: Int) -> Boolean,
    crossinline onCleared: () -> Unit = {},
    crossinline isLeftSwipeEnabled: (viewHolder: ViewHolder) -> Boolean = { true },
    crossinline isRightSwipeEnabled: (viewHolder: ViewHolder) -> Boolean = { true }
): ItemTouchHelper {

    val context = context
    val swipeConfiguration = SwipeConfiguration().apply sc@{
        textLeftToRight = leftToRightLabel
        textRightToLeft = rightToLeftLabel
        this@sc.drawablePadding = drawablePadding
        this@sc.textColor = textColor
        colorLeftToRight = leftToRightColor
        colorRightToLeft = rightToLeftColor
        drawableLeftToRight = leftToRightDrawableRes?.let {
            AppCompatResources.getDrawable(context, it)?.apply {
                mutate()
                setTint("#CCFAFAFF".toColorInt())
                setBounds(0, 0, intrinsicWidth, intrinsicHeight)
            }
        }
        drawableRightToLeft = rightToLeftDrawableRes?.let {
            AppCompatResources.getDrawable(context, it)?.apply {
                mutate()
                setTint("#CCFAFAFF".toColorInt())
                setBounds(0, 0, intrinsicWidth, intrinsicHeight)
            }
        }
    }

    val seslSwipeListAnimator = SeslSwipeListAnimator(this, context).apply {
        setSwipeConfiguration(swipeConfiguration)
    }

    val swipeActionListener = object : SwipeActionListener {
        override fun isLeftSwipeEnabled(viewHolder: ViewHolder): Boolean {
            return isLeftSwipeEnabled.invoke(viewHolder)
        }

        override fun isRightSwipeEnabled(viewHolder: ViewHolder): Boolean {
            return isRightSwipeEnabled.invoke(viewHolder)
        }

        override fun onCleared() {
            onCleared()
        }

        override fun onSwiped(
            position: Int,
            swipeDirection: Int,
            actionState: Int
        ): Boolean {
            return onSwiped.invoke(position, swipeDirection, actionState)
        }
    }

    return ItemTouchHelper(
        SwipeItemCallbackDelegate(
            seslSwipeListAnimator,
            swipeActionListener
        )
    ).apply {
        attachToRecyclerView(this@configureItemSwipeAnimator)
    }
}

/**
 * Hides soft input if showing when this [RecyclerView] is scrolled
 * and optionally clear  focus on the text input view.
 */
@JvmOverloads
fun RecyclerView.hideSoftInputOnScroll(clearFocus: Boolean = true) {
    class HideOnScrollListener(
        private val activity: Activity,
        val clearFocus: Boolean
    ) : OnScrollListener() {
        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
            if (newState == SCROLL_STATE_DRAGGING) {
                activity.hideSoftInput(clearFocus)
            }
        }
    }

    (getTag(R.id.tag_rv_hide_soft_input) as? HideOnScrollListener)?.let {
        if (it.clearFocus == clearFocus) return@hideSoftInputOnScroll
        removeOnScrollListener(it)
    }

    val scrollListener = HideOnScrollListener(context.activity!!, clearFocus)
    setTag(R.id.tag_rv_hide_soft_input, scrollListener)

    if (isAttachedToWindow) addOnScrollListener(scrollListener)

    doOnAttachedStateChanged { _, isAttached ->
        if (isAttached) addOnScrollListener(scrollListener)
        else removeOnScrollListener(scrollListener)
    }
}

/**
 * Automatically adjust bottom padding of GoToTop button
 * and fast scroller when immersive scroll is active.
 */
@RequiresApi(30)
fun RecyclerView.configureImmBottomPadding(toolbarLayout: ToolbarLayout, extraPadding: Int = 0) {
    val tagKey = R.id.tag_rv_imm_bottom_padding_listener

    @Suppress("UNCHECKED_CAST")
    (getTag(tagKey) as? ((Float) -> Unit))?.let { oldListener ->
        toolbarLayout.removeOnBottomOffsetChangedListener(oldListener)
    }

    val bottomOffsetChangedListener: (bottomOffset: Float) -> Unit = {
        seslSetImmersiveScrollBottomPadding(extraPadding + it.toInt())
    }
    setTag(tagKey, bottomOffsetChangedListener)

    if (isAttachedToWindow) {
        toolbarLayout.addOnBottomOffsetChangedListener(bottomOffsetChangedListener)
    }

    doOnAttachedStateChanged { _, isViewAttached ->
        if (isViewAttached) {
            toolbarLayout.addOnBottomOffsetChangedListener(bottomOffsetChangedListener)
        } else {
            toolbarLayout.removeOnBottomOffsetChangedListener(bottomOffsetChangedListener)
        }
    }
}

inline fun RecyclerView.seslSetFastScrollerAdditionalPadding(verticalPadding: Int) {
    seslSetFastScrollerAdditionalPadding(verticalPadding, verticalPadding)
}