@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package dev.oneuiproject.oneui.dialog

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Build
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


class GridMenuDialog @JvmOverloads constructor(
    context: Context,
    @StyleRes theme: Int = R.style.MoreMenuDialogStyle
) : AppCompatDialog(context, theme) {

    private var mMessage: CharSequence? = null
    private var mSpanCount = SPAN_COUNT
    private val mGridItems: ArrayList<GridItem> = ArrayList()
    private lateinit var mContentView: LinearLayout
    private lateinit var mGridListView: RecyclerView
    private val mAdapter: GridListAdapter = GridListAdapter()
    private var horizontalCenter: Float? = null

    fun interface OnItemClickListener {
        fun onClick(item: GridItem): Boolean
    }

    private var onClickMenuItem: OnItemClickListener? = null

    fun setOnItemClickListener(listener: OnItemClickListener?) {
        onClickMenuItem = listener
    }

    @SuppressLint("InflateParams")
    override fun onCreate(savedInstanceState: Bundle?) {
        supportRequestWindowFeature(FEATURE_NO_TITLE)
        val context = context
        val inflater = LayoutInflater.from(context)
        mContentView = inflater.inflate(R.layout.oui_dialog_grid_menu, null) as LinearLayout
        setContentView(mContentView)
        super.onCreate(savedInstanceState)

        resetContentPadding()
        setOnShowListener {
            val layoutManager = mGridListView.layoutManager!!
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

    fun setHorizontalCenter(center: Float?) {
        horizontalCenter = center
    }

    fun updateDialog(){
        mSpanCount = calculateColumnCount()
        mGridListView = mContentView.findViewById<RecyclerView>(R.id.grid_menu_view).apply {
            layoutManager = GridLayoutManager(context, mSpanCount).apply {
                spanSizeLookup = object : SpanSizeLookup() {
                    override fun getSpanSize(position: Int) = 1
                }
            }
            adapter = mAdapter
            (itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations = false
        }

        updateDialogMaxHeight()
        updateDialogWidthAndPosition()
    }

    private fun updateDialogWidthAndPosition() {
        if (Build.VERSION.SDK_INT >= 22) {
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
                        R.dimen.more_menu_dialog_width_ratio_mw
                    } else R.dimen.more_menu_dialog_width_ratio)).toInt()

                if (DeviceLayoutUtil.isTabletLayoutOrDesktop(context)) {
                    dialogWidth = dialogWidth.coerceAtMost(resources.getDimensionPixelOffset(R.dimen.more_menu_dialog_max_width))
                    if (DeviceLayoutUtil.isLandscape(config) && !isInMultiWindowModeCompat(context)) {
                        dialogWidth = dialogWidth.coerceAtLeast(resources.getDimensionPixelOffset(R.dimen.more_menu_dialog_min_width))
                    }
                }

                val isRTL = config.layoutDirection == LAYOUT_DIRECTION_RTL
                attributes = windowLp.apply wlp@ {
                    this@wlp.width = dialogWidth
                    this@wlp.y = resources.getDimensionPixelOffset(R.dimen.more_menu_dialog_y_offset)
                    horizontalCenter?.let {
                        this@wlp.x = (it - dialogWidth / 2).toInt()
                    }
                    this@wlp.windowAnimations = if (isRTL) R.style.MoreMenuDialogSlideRight else
                        R.style.MoreMenuDialogSlideLeft
                }
                setGravity(Gravity.BOTTOM or Gravity.START)
            }
        }else{
            window!!.apply {
                attributes = windowLp.apply wlp@{
                    this@wlp.y = 0
                    this@wlp.width = context.windowWidthNetOfInsets
                }
                setGravity(Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL)
            }
        }
    }

    private fun updateDialogMaxHeight() {
        val gridViewLP = mGridListView.layoutParams
        val isOverMaxRow = isOverMaxRow(mSpanCount)

        val plus = if (isOverMaxRow) {
            if (!isTabletStyle(context) && isLandscapeView(context)) 3 else 4
        } else {
            ((mAdapter.itemCount - 1) / mSpanCount) + 1
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
        mGridListView.setLayoutParams(gridViewLP)
    }

    private fun getMoreMenuItemHeight(): Int {
        val resources = context.resources
        return (resources.getDimensionPixelSize(R.dimen.more_menu_griditem_padding_vertical) * 2) +
                resources.getDimensionPixelSize(R.dimen.more_menu_grid_item_height)
    }

    private fun getAvailableMoreMenuHeight(): Int {
        val resources = context.resources
        return (getWindowHeight(context) - window!!.attributes.y -
                resources.getDimensionPixelSize(R.dimen.more_menu_dialog_padding_top)) -
                resources.getDimensionPixelSize(R.dimen.more_menu_dialog_padding_bottom) -
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
            mAdapter.itemCount > rows * 4
        } else mAdapter.itemCount > rows * 3
    }

    private fun calculateColumnCount(): Int {
        val context = context
        val isPhoneLandscape = isPhoneLandscape(context)
        val resources = context.resources

        // Calculate the available width for grid items
        val horizontalPadding = resources.getDimensionPixelOffset(R.dimen.more_menu_dialog_padding_horizontal)
        val gridItemHorizontalPadding = resources.getDimensionPixelSize(R.dimen.more_menu_griditem_padding_horizontal)
        val minGridItemWidth = resources.getDimensionPixelOffset(R.dimen.more_menu_grid_item_min_width)

        val maxColumns = ((context.windowWidthNetOfInsets - (horizontalPadding * 2)) /
                ((gridItemHorizontalPadding * 2) + minGridItemWidth)).coerceAtLeast(1)

        // Determine the final column count based on landscape mode
        return (if (isPhoneLandscape) SPAN_COUNT_LANDSCAPE else SPAN_COUNT).coerceAtMost(maxColumns)
    }

    fun addTopCustomView(view: View) = mContentView.addView(view, 0)

    private fun resetContentPadding() {
        val res = context.resources
        val horizontalPadding = res.getDimensionPixelSize(R.dimen.oui_grid_menu_dialog_horizontal_padding)
        val verticalPadding = res.getDimensionPixelSize(R.dimen.oui_grid_menu_dialog_vertical_padding)
        val hasMessage = mMessage != null && mMessage!!.isNotEmpty()
        mContentView.setPaddingRelative(
            horizontalPadding,
            if (hasMessage) 0 else verticalPadding,
            horizontalPadding,
            verticalPadding
        )
    }

    @SuppressLint("RestrictedApi")
    fun inflateMenu(@MenuRes menuRes: Int) {
        val context = context
        val menu = MenuBuilder(context).also { SupportMenuInflater(context).inflate(menuRes, it) }
        mGridItems.clear()
        for (i in 0 until menu.size()) {
            (menu.getItem(i) as? MenuItemImpl)?.let {
                if (it.isVisible) {
                    mGridItems.add(it.toGridDialogItem())
                }
            }
        }
        notifyDataSetChanged()
    }

    fun updateItems(gridItems: List<GridItem>) {
        this.mGridItems.clear()
        this.mGridItems.addAll(gridItems)
        notifyDataSetChanged()
    }

    fun addItem(gridItem: GridItem) {
        mGridItems.add(gridItem)
        notifyDataSetChanged()
    }

    fun addItem(index: Int, gridItem: GridItem) {
        mGridItems.add(index, gridItem)
        notifyDataSetChanged()
    }

    fun findItem(itemId: Int): GridItem? {
        return mGridItems.find { it.itemId == itemId }.also {
            if (it == null) {
                Log.e(TAG, "findItem: couldn't find item with id 0x${Integer.toHexString(itemId)}")
            }
        }
    }

    fun removeItem(index: Int) {
        mGridItems.removeAt(index)
        notifyDataSetChanged()
    }

    fun removeItemWithId(@IdRes id: Int) {
        findItem(id)?.let {
            mGridItems.remove(it)
            notifyDataSetChanged()
        } ?: Log.e(
            TAG, "removeItemWithId: couldn't find item with id 0x"
                    + Integer.toHexString(id)
        )
    }

    fun setEnableItem(itemId: Int, enabled: Boolean) {
        mGridItems.findWithIndex({ it.itemId == itemId }) { index, item ->
            mGridItems[index] = item.copy(isEnabled = enabled)
            mAdapter.notifyItemChanged(index)
        }
    }

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    internal fun isShowingBadge() = mGridItems.count { it.badge != Badge.NONE } > 0

    fun setBadge(itemId: Int, badge: Badge) {
        mGridItems.findWithIndex({ it.itemId == itemId }) { index, item ->
            mGridItems[index] = item.copy(badge = badge)
            mAdapter.notifyItemChanged(index)
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun notifyDataSetChanged() = mAdapter.notifyDataSetChanged()

    private inner class GridListAdapter : RecyclerView.Adapter<GridListViewHolder>() {
        @SuppressLint("InflateParams")
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GridListViewHolder {
            val inflater = LayoutInflater.from(context)
            val view =
                inflater.inflate(R.layout.oui_view_grid_menu_dialog_item, null, false)

            return GridListViewHolder(view).apply {
                itemView.setOnClickListener {
                    val gridItem = mGridItems[bindingAdapterPosition]
                    val result = onClickMenuItem?.onClick(gridItem) ?: false
                    if (result) {
                        parent.postDelayed({ dismiss()}, 240)
                    }
                }
            }
        }

        override fun onBindViewHolder(holder: GridListViewHolder, position: Int) {
            val gridItem = mGridItems[position]
            holder.apply {
                iconView.setImageDrawable(gridItem.icon)
                titleView.text = gridItem.title
                setBadge(gridItem.badge)
                setEnabled(gridItem.isEnabled)
                setTooltipText(gridItem.tooltipText)
            }
        }

        override fun getItemCount(): Int = mGridItems.size
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

    data class GridItem(
        @JvmField
        val itemId: Int,
        @JvmField
        val title: CharSequence?,
        @JvmField
        val icon: Drawable?,
        @JvmField
        val tooltipText: CharSequence? = null,
        val isEnabled: Boolean = true,
        val isVisible: Boolean = true,
        val badge: Badge = Badge.NONE
    )

    companion object {
        private const val TAG = "GridMenuDialog"
        private const val SPAN_COUNT = 4
        private const val SPAN_COUNT_LANDSCAPE = 5
    }
}

