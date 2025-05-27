package dev.oneuiproject.oneui.delegates

import android.graphics.Canvas
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.View.LAYOUT_DIRECTION_RTL
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ItemTouchHelper.ACTION_STATE_DRAG
import androidx.recyclerview.widget.ItemTouchHelper.ACTION_STATE_IDLE
import androidx.recyclerview.widget.ItemTouchHelper.ACTION_STATE_SWIPE
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.recyclerview.widget.SeslSwipeListAnimator
import java.util.Locale
import androidx.core.text.layoutDirection


/**
 * A delegate class that extends [ItemTouchHelper.Callback] to handle swipe gestures on RecyclerView items.
 *
 * This class works in conjunction with [SeslSwipeListAnimator] and a [SwipeActionListener]
 * to provide swipe-to-action functionality for RecyclerView items. It manages the swipe states,
 * animations, and communicates swipe events to the [SwipeActionListener].
 *
 * @property seslSwipeListAnimator The animator responsible for swipe animations.
 * @property swipeActionCallback The listener for swipe actions.
 */
class SwipeItemCallbackDelegate(
    private val seslSwipeListAnimator: SeslSwipeListAnimator,
    private val swipeActionCallback: SwipeActionListener
) : ItemTouchHelper.Callback() {

    companion object {
        private const val TAG = "SemSwipeCallback"
    }

    private val isRTL = Locale.getDefault().layoutDirection == LAYOUT_DIRECTION_RTL

    private var swipeItemView: View? = null
    private var isSwipeInProgress = false
    private var isDelayedActionPending = false

    private var startX = 0f
    private var allowAction = false

    override fun onSelectedChanged(viewholder: ViewHolder?, actionState: Int) {
        super.onSelectedChanged(viewholder, actionState)
        Log.d(TAG, "onSelectedChanged : $actionState")

        if (viewholder != null) {
            when (actionState) {
                ACTION_STATE_SWIPE -> {
                    allowAction = true
                    if (isSwipeInProgress && swipeItemView != null) {
                        seslSwipeListAnimator.clearSwipeAnimation(swipeItemView!!)
                        swipeItemView!!.translationX = 0.0f
                        swipeItemView!!.setAlpha(1.0f)
                    }
                    swipeItemView = viewholder.itemView
                    isSwipeInProgress = true

                }
            }
        }
    }

    override fun onSwiped(viewholder: ViewHolder, direction: Int) {
        if (allowAction) {
            val state = if (startX > 0.0f) ACTION_STATE_IDLE else ACTION_STATE_SWIPE
            if (swipeActionCallback.onSwiped(viewholder.layoutPosition, direction, state)) {
                Handler(Looper.getMainLooper()).postDelayed(
                    { isDelayedActionPending = false },
                    1000L
                )
            }

            seslSwipeListAnimator.onSwiped(swipeItemView!!)
            isSwipeInProgress = false
        }
        allowAction = false
    }

    override fun clearView(
        recyclerView: RecyclerView,
        viewholder: ViewHolder
    ) {
        Log.d(TAG, "clearView")
        seslSwipeListAnimator.clearSwipeAnimation(swipeItemView!!)
        isSwipeInProgress = true
        super.clearView(recyclerView, viewholder)
        swipeActionCallback.onCleared()
    }

    override fun getAnimationDuration(
        recyclerView: RecyclerView,
        animationType: Int,
        animateDx: Float,
        animateDy: Float
    ): Long {
        return seslSwipeListAnimator.getAnimationDuration(recyclerView, animationType, animateDx, animateDy)
    }

    override fun getMovementFlags(
        recyclerView: RecyclerView,
        viewholder: ViewHolder
    ): Int {
        return makeFlag(ACTION_STATE_SWIPE,
            getSwipeDir(swipeActionCallback.isLeftSwipeEnabled(viewholder),
                swipeActionCallback.isRightSwipeEnabled(viewholder))
        )
    }

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: ViewHolder,
        target: ViewHolder
    ): Boolean {
        Log.d(TAG, "onMove")
        return true
    }

    override fun onChildDraw(
        canvas: Canvas,
        recyclerView: RecyclerView,
        viewholder: ViewHolder,
        dX: Float,
        dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean
    ) {
        Log.d(TAG, "onChildDrawOver")
        if (allowAction && !isDelayedActionPending) {
            startX = dX
            seslSwipeListAnimator.doMoveAction(canvas, swipeItemView!!, dX, isCurrentlyActive)
        }
    }

    private fun getSwipeDir(isLeftSwipeEnabled: Boolean, isRightSwipeEnabled: Boolean): Int {
        var flags = 0
        if (isRightSwipeEnabled) {
            flags += if (isRTL) ItemTouchHelper.END else ItemTouchHelper.START
        }
        if (isLeftSwipeEnabled) {
            flags += if (isRTL) ItemTouchHelper.START else ItemTouchHelper.END
        }
        return flags
    }
}


interface SwipeActionListener {
    /**
     * Called when a ViewHolder is swiped by the user.
     * If you are returning relative directions (START , END) from the getMovementFlags(RecyclerView, RecyclerView. ViewHolder) method, this method will also use relative directions. Otherwise, it will use absolute directions.
     * If you don't support swiping, this method will never be called.
     *
     * @param position The position returned by [RecyclerView.ViewHolder.getLayoutPosition]
     * @param swipeDirection One of [ItemTouchHelper.START], [ItemTouchHelper.END], [ItemTouchHelper.LEFT] or [ItemTouchHelper.RIGHT]
     * @param actionState One of [ACTION_STATE_IDLE], [ACTION_STATE_SWIPE] or [ACTION_STATE_DRAG].
     */
    fun onSwiped(position: Int, swipeDirection: Int, actionState: Int): Boolean

    /**
     * Specify if swiping left-to-right is enabled for this item viewHolder
     * @param viewHolder The [RecyclerView.ViewHolder] of the recyclerview item
     */
    fun isLeftSwipeEnabled(viewHolder: ViewHolder): Boolean
    /**
     * Specify if swiping right-to-left is enabled for this item viewHolder
     * @param viewHolder The [RecyclerView.ViewHolder] of the recyclerview item
     */
    fun isRightSwipeEnabled(viewHolder: ViewHolder): Boolean

    fun onCleared()
}