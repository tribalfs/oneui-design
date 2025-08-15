@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package dev.oneuiproject.oneui.dialog

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.Window.FEATURE_NO_TITLE
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.IdRes
import androidx.annotation.MenuRes
import androidx.annotation.RestrictTo
import androidx.annotation.StyleRes
import androidx.appcompat.app.AppCompatDialog
import androidx.appcompat.view.SupportMenuInflater
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.view.menu.MenuItemImpl
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.GridLayoutManager.SpanSizeLookup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.google.android.material.tabs.TabLayout.LAYOUT_DIRECTION_RTL
import dev.oneuiproject.oneui.design.R
import dev.oneuiproject.oneui.dialog.internal.toGridDialogItem
import dev.oneuiproject.oneui.ktx.activity
import dev.oneuiproject.oneui.ktx.findWithIndex
import dev.oneuiproject.oneui.ktx.semSetToolTipText
import dev.oneuiproject.oneui.ktx.windowWidthNetOfInsets
import dev.oneuiproject.oneui.layout.Badge
import dev.oneuiproject.oneui.layout.internal.util.DrawerLayoutUtils.updateBadgeView
import dev.oneuiproject.oneui.utils.DeviceLayoutUtil
import dev.oneuiproject.oneui.utils.DeviceLayoutUtil.getWindowHeight
import dev.oneuiproject.oneui.utils.DeviceLayoutUtil.isDeskTopMode
import dev.oneuiproject.oneui.utils.DeviceLayoutUtil.isInMultiWindowModeCompat
import dev.oneuiproject.oneui.utils.DeviceLayoutUtil.isLandscapeView
import dev.oneuiproject.oneui.utils.DeviceLayoutUtil.isPhoneLandscape
import dev.oneuiproject.oneui.utils.DeviceLayoutUtil.isPhoneLandscapeOrTablet
import dev.oneuiproject.oneui.utils.DeviceLayoutUtil.isPortrait
import dev.oneuiproject.oneui.utils.DeviceLayoutUtil.isTabletStyle
import dev.oneuiproject.oneui.utils.TypedValueUtils
import kotlin.collections.find
import androidx.appcompat.R as appcompatR


/**
 * A dialog that displays a grid of menu items.
 * It can be used to display a list of actions or options to the user in a visually appealing way.
 * Each action item can have an icon, a title, and a tooltip and supports showing a badge.
 *
 * ## Example usage:
 * ```
 * val dialog = GridMenuDialog(context)
 * dialog.inflateMenu(R.menu.my_menu)
 * dialog.setOnItemClickListener { item ->
 *     // Handle item click
 *     true // Return true to dismiss the dialog
 * }
 * dialog.show()
 * ```
 *
 * @param context The context in which the dialog should be displayed.
 * @param theme (Optional) The custom theme to use for the dialog.
 */
class GridMenuDialog @JvmOverloads constructor(
    context: Context,
    @StyleRes theme: Int = R.style.MoreMenuDialogStyle
) : AppCompatDialog(context, theme) {

    private var message: CharSequence? = null
    private var spanCount = SPAN_COUNT
    private val gridItems: ArrayList<GridItem> = ArrayList()
    private lateinit var contentView: LinearLayout
    private lateinit var gridListView: RecyclerView
    private val adapter: GridListAdapter = GridListAdapter()
    private var currentAnchorView: View? = null
    private var selectedId: Int = -1

    /**
     * Interface definition for a callback to be invoked when an item in this
     * [GridMenuDialog] has been clicked.
     */
    fun interface OnItemClickListener {
        fun onClick(item: GridItem): Boolean
    }

    private var onClickMenuItem: OnItemClickListener? = null

    /**
     * Sets a listener to be invoked when an item in this dialog is clicked.
     *
     * @param listener The listener that will be invoked.
     */
    fun setOnItemClickListener(listener: OnItemClickListener?) {
        onClickMenuItem = listener
    }

    @SuppressLint("InflateParams")
    override fun onCreate(savedInstanceState: Bundle?) {
        supportRequestWindowFeature(FEATURE_NO_TITLE)
        val context = context
        val inflater = LayoutInflater.from(context)
        contentView = inflater.inflate(R.layout.oui_des_dialog_grid_menu, null) as LinearLayout
        setContentView(contentView)
        super.onCreate(savedInstanceState)

        resetContentPadding()
        setOnShowListener {
            val layoutManager = gridListView.layoutManager!!
            val childCount = layoutManager.childCount
            for (i in 0 until childCount) {
                layoutManager.getChildAt(i)?.apply {
                    requestFocus()
                    return@setOnShowListener
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        updateDialog()
    }

    /**
     * Updates the dialog's layout and appearance.
     *
     * This function recalculates the number of columns in the grid,
     * initializes or updates the RecyclerView for displaying grid items,
     * and adjusts the dialog's maximum height.
     */
    fun updateDialog(){
        spanCount = calculateColumnCount().coerceAtMost(gridItems.size)
        gridListView = contentView.findViewById<RecyclerView>(R.id.grid_menu_view).apply {
            layoutManager = GridLayoutManager(context, spanCount).apply {
                spanSizeLookup = object : SpanSizeLookup() {
                    override fun getSpanSize(position: Int) = 1
                }
            }
            this.adapter = this@GridMenuDialog.adapter
            (itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations = false
        }

        updateDialogMaxHeight()
    }

    private fun updateDialogWidthAndPosition() {
        if (SDK_INT >= 22) {
            window!!.setElevation(0f)
        }

        val windowLp = WindowManager.LayoutParams()
        windowLp.copyFrom(window!!.attributes)

        if (isPhoneLandscapeOrTablet(context)) {
            val context = context
            val resources = context.resources
            val config = resources.configuration

            window!!.apply{
                val decorView = context.activity!!.window.decorView
                val decorViewWidth = decorView.width
                var dialogWidth = (decorViewWidth * TypedValueUtils.getFloat(context,
                    if (isInMultiWindowModeCompat(context)) {
                        R.dimen.oui_des_more_menu_dialog_width_ratio_mw
                    } else R.dimen.oui_des_more_menu_dialog_width_ratio)).toInt()

                if (DeviceLayoutUtil.isTabletLayoutOrDesktop(context)) {
                    dialogWidth = dialogWidth.coerceAtMost(resources.getDimensionPixelOffset(R.dimen.oui_des_more_menu_dialog_max_width))
                    if (DeviceLayoutUtil.isLandscape(config) && !isInMultiWindowModeCompat(context)) {
                        dialogWidth = dialogWidth.coerceAtLeast(resources.getDimensionPixelOffset(
                            R.dimen.oui_des_more_menu_dialog_min_width))
                    }
                }

                val isRTL = config.layoutDirection == LAYOUT_DIRECTION_RTL
                attributes = windowLp.apply wlp@ {
                    this@wlp.width = dialogWidth
                    this@wlp.y = resources.getDimensionPixelOffset(R.dimen.oui_des_more_menu_dialog_y_offset)
                    getAnchorViewHorizontalCenter()?.let {
                        this@wlp.x = (it - dialogWidth / 2).toInt()
                    }
                    this@wlp.windowAnimations = if (isRTL) R.style.MoreMenuDialogSlideRight else R.style.MoreMenuDialogSlideLeft
                    gravity = Gravity.BOTTOM or Gravity.START
                }
            }
        }else{
            window!!.apply {
                attributes = windowLp.apply wlp@{
                    this@wlp.y = 0
                    this@wlp.width = context.windowWidthNetOfInsets
                    gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                    this@wlp.windowAnimations = appcompatR.style.Animation_AppCompat_Dialog
                }
            }
        }
    }

    private fun updateDialogMaxHeight() {
        val gridViewLP = gridListView.layoutParams
        val isOverMaxRow = isOverMaxRow(spanCount)

        val plus = if (isOverMaxRow) {
            if (!isTabletStyle(context) && isLandscapeView(context)) 3 else 4
        } else {
            ((adapter.itemCount - 1) / spanCount) + 1
        }

        val moreMenuItemHeight: Int = getMoreMenuItemHeight() * plus
        val availableMoreMenuHeight: Int = getAvailableMoreMenuHeight()

        if (moreMenuItemHeight > availableMoreMenuHeight) {
            gridViewLP.height = availableMoreMenuHeight
        } else if (isOverMaxRow) {
            gridViewLP.height = moreMenuItemHeight
        } else {
            gridViewLP.height = WRAP_CONTENT
        }
        gridListView.setLayoutParams(gridViewLP)
    }

    private val updateDialogWidthAndPositionRunnable = Runnable {
        updateDialogWidthAndPosition()
        show()
    }

    private val onLayoutChangeListener = View.OnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
        currentAnchorView?.apply {
            removeCallbacks(updateDialogWidthAndPositionRunnable)
            hide()
            postDelayed(updateDialogWidthAndPositionRunnable,500)
        }
    }

    /**
     * Sets the anchor view for the dialog.
     * Typically, the dialog will be positioned at the center and above this view.
     *
     * @param anchor The view to anchor the dialog to.
     */
    fun setAnchor(anchor: View?) {
        currentAnchorView?.removeOnLayoutChangeListener(onLayoutChangeListener)
        currentAnchorView = anchor
        anchor?.apply {
            addOnLayoutChangeListener(onLayoutChangeListener)
            updateDialogWidthAndPosition()
        }
    }

    private fun getAnchorViewHorizontalCenter(): Float? {
        return currentAnchorView?.let { view ->
            val location = IntArray(2)
            if (SDK_INT >= 29) {
                view.getLocationInSurface(location)
            }else{
                view.getLocationInWindow(location)
            }
            location[0] + view.width / 2f
        }
    }

    override fun dismiss() {
        currentAnchorView?.removeOnLayoutChangeListener(onLayoutChangeListener)
        currentAnchorView = null
        super.dismiss()
    }

    private fun getMoreMenuItemHeight(): Int {
        val resources = context.resources
        return (resources.getDimensionPixelSize(R.dimen.oui_des_more_menu_griditem_padding_vertical) * 2) +
                resources.getDimensionPixelSize(R.dimen.oui_des_more_menu_grid_item_height)
    }

    private fun getAvailableMoreMenuHeight(): Int {
        val resources = context.resources
        return (getWindowHeight(context) - window!!.attributes.y -
                resources.getDimensionPixelSize(R.dimen.oui_des_more_menu_dialog_padding_top)) -
                resources.getDimensionPixelSize(R.dimen.oui_des_more_menu_dialog_padding_bottom) -
                getStatusBarHeight()
    }

    private fun getStatusBarHeight(): Int {
        val resources = context.resources
        if (isPortrait(resources.configuration) && isInMultiWindowModeCompat(context)) {
            return 0
        }
        return DeviceLayoutUtil.getStatusBarHeight(resources)
    }

    private fun isOverMaxRow(rows: Int): Boolean {
        return if ((isDeskTopMode(context.resources) || !isLandscapeView(context))) {
            adapter.itemCount > rows * 4
        } else adapter.itemCount > rows * 3
    }

    private fun calculateColumnCount(): Int {
        val context = context
        val isPhoneLandscape = isPhoneLandscape(context)
        val resources = context.resources

        // Calculate the available width for grid items
        val horizontalPadding = resources.getDimensionPixelOffset(R.dimen.oui_des_more_menu_dialog_padding_horizontal)
        val gridItemHorizontalPadding = resources.getDimensionPixelSize(R.dimen.oui_des_more_menu_griditem_padding_horizontal)
        val minGridItemWidth = resources.getDimensionPixelOffset(R.dimen.oui_des_more_menu_grid_item_min_width)

        val maxColumns = ((context.windowWidthNetOfInsets - (horizontalPadding * 2)) /
                ((gridItemHorizontalPadding * 2) + minGridItemWidth)).coerceAtLeast(1)

        // Determine the final column count based on landscape mode
        return (if (isPhoneLandscape) SPAN_COUNT_LANDSCAPE else SPAN_COUNT).coerceAtMost(maxColumns)
    }

    /**
     * Add a custom view to the top of the dialog.
     *
     * @param view The view to add.
     */
    fun addTopCustomView(view: View) = contentView.addView(view, 0)

    private fun resetContentPadding() {
        val res = context.resources
        val horizontalPadding = res.getDimensionPixelSize(R.dimen.oui_des_grid_menu_dialog_horizontal_padding)
        val verticalPadding = res.getDimensionPixelSize(R.dimen.oui_des_grid_menu_dialog_vertical_padding)
        val hasMessage = message != null && message!!.isNotEmpty()
        contentView.setPaddingRelative(
            horizontalPadding,
            if (hasMessage) 0 else verticalPadding,
            horizontalPadding,
            verticalPadding
        )
    }

    /**
     * Inflates a menu resource into this dialog.
     * This will clear the existing items and add all items from the provided menu resource.
     *
     * @param menuRes The menu resource to inflate.
     */
    @SuppressLint("RestrictedApi")
    fun inflateMenu(@MenuRes menuRes: Int) {
        val context = context
        val menu = MenuBuilder(context).also { SupportMenuInflater(context).inflate(menuRes, it) }
        gridItems.clear()
        for (i in 0 until menu.size()) {
            (menu.getItem(i) as? MenuItemImpl)?.let {
                if (it.isVisible) {
                    gridItems.add(it.toGridDialogItem())
                }
            }
        }
        notifyDataSetChanged()
    }

    /**
     * Updates the list of items displayed in the grid menu.
     * This will clear the existing items and add all items from the provided list.
     * The adapter is then notified of the data set change to refresh the UI.
     *
     * @param gridItems The new list of [GridItem]s to display.
     */
    fun updateItems(gridItems: List<GridItem>) {
        this.gridItems.clear()
        this.gridItems.addAll(gridItems)
        notifyDataSetChanged()
    }

    /**
     * Adds a [GridItem] to the end of the list of items in the dialog.
     *
     * @param gridItem The item to add.
     */
    fun addItem(gridItem: GridItem) {
        gridItems.add(gridItem)
        notifyDataSetChanged()
    }

    /**
     * Adds a [GridItem] to the list of items at the specified [index].
     *
     * @param index The index at which to add the item.
     * @param gridItem The item to add.
     */
    fun addItem(index: Int, gridItem: GridItem) {
        gridItems.add(index, gridItem)
        notifyDataSetChanged()
    }

    /**
     * Finds a [GridItem] by its ID.
     *
     * @param itemId The ID of the item to find.
     * @return The [GridItem] if found, or null if no item with the given ID exists.
     */
    fun findItem(itemId: Int): GridItem? {
        return gridItems.find { it.itemId == itemId }.also {
            if (it == null) {
                Log.e(TAG, "findItem: couldn't find item with id 0x${Integer.toHexString(itemId)}")
            }
        }
    }

    /**
     * Removes the item at the specified position in this list.
     * Shifts any subsequent elements to the left (subtracts one from their indices).
     *
     * @param index the index of the element to remove.
     * @throws IndexOutOfBoundsException if the index is out of bounds (index < 0 || index >= size).
     */
    fun removeItem(index: Int) {
        gridItems.removeAt(index)
        notifyDataSetChanged()
    }

    fun removeItemWithId(@IdRes id: Int) {
        findItem(id)?.let {
            gridItems.remove(it)
            notifyDataSetChanged()
        } ?: Log.e(
            TAG, "removeItemWithId: couldn't find item with id 0x"
                    + Integer.toHexString(id)
        )
    }

    /**
     * Sets the enabled state of an item in the grid menu.
     *
     * @param itemId The ID of the item to enable or disable.
     * @param enabled True to enable the item, false to disable it.
     */
    fun setEnableItem(itemId: Int, enabled: Boolean) {
        gridItems.findWithIndex({ it.itemId == itemId }) { index, item ->
            gridItems[index] = item.copy(isEnabled = enabled)
            adapter.notifyItemChanged(index)
        }
    }

    /**
     * Sets the selected item in the grid menu.
     * If an item is already selected, it will be deselected.
     * The new item will be marked as selected, and the UI will be updated accordingly.
     *
     * @param itemId The ID of the item to select. If -1, no item will be selected.
     */
    fun setSelectedItem(itemId: Int) {
        if (selectedId == itemId) return
        val prevIdx =  if (selectedId == -1) -1 else gridItems.indexOfFirst { it.itemId == selectedId }
        val currentIdx =  if (itemId == -1) -1 else gridItems.indexOfFirst { it.itemId == itemId }
        selectedId = itemId
        if (prevIdx != -1) adapter.notifyItemChanged(prevIdx)
        if (currentIdx != -1) adapter.notifyItemChanged(currentIdx)
    }

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    internal fun isShowingBadge() = gridItems.count { it.badge != Badge.NONE } > 0

    /**
     * Sets the badge for a specific grid item.
     *
     * @param itemId The ID of the item to set the badge for.
     * @param badge The badge to set.
     */
    fun setBadge(itemId: Int, badge: Badge) {
        gridItems.findWithIndex({ it.itemId == itemId }) { index, item ->
            gridItems[index] = item.copy(badge = badge)
            adapter.notifyItemChanged(index)
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun notifyDataSetChanged() = adapter.notifyDataSetChanged()

    private inner class GridListAdapter : RecyclerView.Adapter<GridListViewHolder>() {
        @SuppressLint("InflateParams")
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GridListViewHolder {
            val inflater = LayoutInflater.from(context)
            val view =
                inflater.inflate(R.layout.oui_des_view_grid_menu_dialog_item, null, false)

            return GridListViewHolder(view).apply {
                itemView.setOnClickListener {
                    val gridItem = gridItems[bindingAdapterPosition]
                    val result = onClickMenuItem?.onClick(gridItem) ?: false
                    if (result) {
                        dismiss()
                    }
                }
            }
        }

        override fun onBindViewHolder(holder: GridListViewHolder, position: Int) {
            val gridItem = gridItems[position]
            holder.apply {
                iconView.setImageDrawable(gridItem.icon)
                titleView.text = gridItem.title
                setBadge(gridItem.badge)
                setEnabled(gridItem.isEnabled)
                setTooltipText(gridItem.tooltipText)
                holder.itemView.isSelected = selectedId == gridItem.itemId
            }
        }

        override fun getItemCount(): Int = gridItems.size
    }

    private class GridListViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val iconView: ImageView = itemView.findViewById(R.id.grid_menu_item_icon)
        val titleView: TextView = itemView.findViewById(R.id.grid_menu_item_title)
        private val mBadgeView: TextView = itemView.findViewById(R.id.grid_menu_item_badge)

        fun setBadge(badge: Badge) = mBadgeView.updateBadgeView(badge, -12)

        fun setEnabled(enabled: Boolean) {
            itemView.apply {
                if (this.isEnabled == enabled) return
                isEnabled = enabled
                alpha = if (enabled) 1.0f else 0.4f
            }
        }

        fun setTooltipText(tooltipText: CharSequence?) = itemView.semSetToolTipText(tooltipText)
    }

    /**
     * Represents an item in the grid menu.
     *
     * @property itemId The unique identifier for the item.
     * @property title The title of the item.
     * @property icon The icon for the item.
     * @property tooltipText Optional tooltip text for the item.
     * @property isEnabled Whether the item is enabled and interactive. Defaults to `true`.
     * @property isVisible Whether the item is visible in the grid. Defaults to `true`.
     * @property badge The badge to display on the item. Defaults to [Badge.NONE].
     */
    data class GridItem(
        @JvmField
        val itemId: Int,
        @JvmField
        val title: CharSequence?,
        @JvmField
        val icon: Drawable?,
        @JvmField
        val tooltipText: CharSequence? = null,
        @JvmField
        val isEnabled: Boolean = true,
        @JvmField
        val isVisible: Boolean = true,
        @JvmField
        val badge: Badge = Badge.NONE
    )

    private companion object {
        const val TAG = "GridMenuDialog"
        const val SPAN_COUNT = 4
        const val SPAN_COUNT_LANDSCAPE = 5
    }
}

