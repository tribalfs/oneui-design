@file:Suppress("unused")

package dev.oneuiproject.oneui.widget

import android.content.Context
import android.content.res.TypedArray
import android.graphics.Rect
import android.os.Build
import android.text.TextUtils
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityNodeInfo
import android.view.autofill.AutofillManager
import android.view.autofill.AutofillValue
import android.widget.LinearLayout
import androidx.annotation.IdRes
import androidx.annotation.RequiresApi
import androidx.core.content.withStyledAttributes
import androidx.core.view.isVisible
import dev.oneuiproject.oneui.design.R


/**
 * A [ViewGroup] that displays a set of [RadioItemView]s vertically.
 *
 * This layout arranges its children in a single vertical column.
 * The [RadioItemView]s within this group are mutually exclusive; selecting one
 * [RadioItemView] will automatically deselect any previously selected item.
 *
 * This behavior is similar to a standard `RadioGroup` but is designed specifically
 * for custom [RadioItemView] instances.
 *
 * @param context The Context the view is running in, through which it can
 * access the current theme, resources, etc.
 * @param attrs The attributes of the XML tag that is inflating the view.
 *
 * @see RadioItemView
 */
class RadioItemViewGroup @JvmOverloads constructor(
    private val context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {
    /**
     *
     * Returns the identifier of the selected checked text in this group.
     * Upon empty selection, the returned value is -1.
     *
     * @return the unique id of the selected checked text in this group
     *
     * @see .check
     * @see .clearCheck
     * @attr ref android.R.styleable#RadioGroup_checkedButton
     */
    // holds the checked id; the selection is empty by default
    @get:IdRes
    val checkedRadioButtonId: Int get() = _checkedRadioButtonId
    private var _checkedRadioButtonId: Int = -1

    // tracks children checked texts checked state
    private var childOnCheckedChangeListener = CheckedStateTracker()

    // when true, mOnCheckedChangeListener discards events
    private var protectFromCheckedChange = false

    private var onCheckedChangeListener: OnCheckedChangeListener? = null
    private var passThroughListener = PassThroughHierarchyChangeListener()

    // Indicates whether the child was set from resources or dynamically, so it can be used
    // to sanitize autofill requests.
    private var initialCheckedId = View.NO_ID

    init {
        orientation = VERTICAL
        // RadioGroup is important by default, unless app developer overrode attribute.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (importantForAutofill == View.IMPORTANT_FOR_AUTOFILL_AUTO) {
                setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_YES)
            }
        }
        setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES)

        // retrieve selected checked text as requested by the user in the
        // XML layout file
        context.withStyledAttributes(
            attrs,
            R.styleable.RadioItemViewGroup,
            R.attr.radioItemViewGroupStyle,
            0
        ) {
            getResourceId(R.styleable.RadioItemViewGroup_checkedButton, View.NO_ID).let { checkedButtonId ->
                if (checkedButtonId != View.NO_ID) {
                    _checkedRadioButtonId = checkedButtonId
                    initialCheckedId = checkedButtonId
                }
            }
        }

        super.setOnHierarchyChangeListener(passThroughListener)

    }

    private fun findChildViewUnder(view: View, x: Int, y: Int): View? {
        var foundView: View? = null

        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                val child: View = view.getChildAt(i)

                val childRect = Rect()
                val parentRect = Rect()
                child.getGlobalVisibleRect(childRect)
                getGlobalVisibleRect(parentRect)

                if (childRect.contains(parentRect.left + x, parentRect.top + y)) {
                    foundView = findChildViewUnder(child, x, y)
                    if (foundView != null) {
                        break
                    }
                }
            }
        }

        if (foundView == null && view.isClickable && view.isVisible && view.isEnabled) {
            return view
        }

        return foundView
    }

    private fun findClickableChildUnder(event: MotionEvent): View? {
        var clickableChild: View? = null

        for (i in 0 until childCount) {
            val child = getChildAt(i)
            val childRect = Rect()
            val parentRect = Rect()
            child.getGlobalVisibleRect(childRect)
            getGlobalVisibleRect(parentRect)

            if (childRect.contains(
                    parentRect.left + event.x.toInt(),
                    parentRect.top + event.y.toInt()
                )
            ) {
                clickableChild = child
                break
            }
        }

        if (clickableChild == null) {
            return null
        }

        val childUnder = findChildViewUnder(clickableChild, event.x.toInt(), event.y.toInt())
        if (childUnder != null && childUnder !== clickableChild) {
            if (childUnder.height * childUnder.width < clickableChild.height * clickableChild.width * 0.5) {
                return null
            }
        }

        return childUnder
    }

    override fun setOnHierarchyChangeListener(listener: OnHierarchyChangeListener) {
        // the user listener is delegated to our pass-through listener
        passThroughListener.mOnHierarchyChangeListener = listener
    }

    override fun onFinishInflate() {
        super.onFinishInflate()

        // checks the appropriate checked text as requested in the XML file
        if (_checkedRadioButtonId != -1) {
            protectFromCheckedChange = true
            setCheckedStateForView(_checkedRadioButtonId, true)
            protectFromCheckedChange = false
            setCheckedId(_checkedRadioButtonId)
        }
    }

    override fun addView(child: View, index: Int, params: ViewGroup.LayoutParams) {
        if (child is RadioItemView) {
            if (child.isChecked) {
                protectFromCheckedChange = true
                if (_checkedRadioButtonId != -1) {
                    setCheckedStateForView(_checkedRadioButtonId, false)
                }
                protectFromCheckedChange = false
                setCheckedId(child.getId())
            }
            child.apply {
                showTopDivider = false
                setOnClickListener {
                    child.isChecked = true
                }
            }
        }

        super.addView(child, index, params)
    }

    /**
     *
     * Sets the selection to the radio button text whose identifier is passed in
     * parameter. Using -1 as the selection identifier clears the selection;
     * such an operation is equivalent to invoking [clearCheck].
     *
     * @param id the unique id of the checked text to select in this group
     *
     * @see .getCheckedRadioButtonId
     * @see .clearCheck
     */
    fun check(@IdRes id: Int) {
        // don't even bother
        if (id != -1 && id == _checkedRadioButtonId) {
            return
        }

        if (_checkedRadioButtonId != -1) {
            setCheckedStateForView(_checkedRadioButtonId, false)
        }

        if (id != -1) {
            setCheckedStateForView(id, true)
        }

        setCheckedId(id)
    }

    private fun setCheckedId(@IdRes id: Int) {
        val changed = id != _checkedRadioButtonId
        _checkedRadioButtonId = id

        if (onCheckedChangeListener != null) {
            onCheckedChangeListener!!.onCheckedChanged(this, _checkedRadioButtonId)
        }
        if (changed && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.getSystemService(AutofillManager::class.java).notifyValueChanged(this)
        }
    }

    private fun setCheckedStateForView(viewId: Int, checked: Boolean) {
        val checkedView = findViewById<View>(viewId)
        if (checkedView != null && checkedView is RadioItemView) {
            checkedView.isChecked = checked
        }
    }

    /**
     *
     * Clears the selection. When the selection is cleared, no checked text
     * in this group is selected and [.getCheckedRadioButtonId] returns
     * null.
     *
     * @see .check
     * @see .getCheckedRadioButtonId
     */
    fun clearCheck() {
        check(-1)
    }

    /**
     *
     * Register a callback to be invoked when the checked checked text
     * changes in this group.
     *
     * @param listener the callback to call on checked state change
     */
    fun setOnCheckedChangeListener(listener: OnCheckedChangeListener?) {
        onCheckedChangeListener = listener
    }

    override fun generateLayoutParams(attrs: AttributeSet): LayoutParams {
        return LayoutParams(context, attrs)
    }

    override fun checkLayoutParams(p: ViewGroup.LayoutParams): Boolean {
        return p is LayoutParams
    }

    override fun generateDefaultLayoutParams(): LinearLayout.LayoutParams {
        return LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    /**
     *
     * This set of layout parameters defaults the width and the height of
     * the children to [.WRAP_CONTENT] when they are not specified in the
     * XML file. Otherwise, this class ussed the value read from the XML file.
     *
     *
     */
    class LayoutParams : LinearLayout.LayoutParams {

        constructor(c: Context?, attrs: AttributeSet?) : super(c, attrs)
        constructor(w: Int, h: Int) : super(w, h)
        constructor(w: Int, h: Int, initWeight: Float) : super(w, h, initWeight)
        constructor(p: ViewGroup.LayoutParams?) : super(p)
        constructor(source: MarginLayoutParams?) : super(source)

        /**
         *
         * Fixes the child's width to
         * [android.view.ViewGroup.LayoutParams.WRAP_CONTENT] and the child's
         * height to  [android.view.ViewGroup.LayoutParams.WRAP_CONTENT]
         * when not specified in the XML file.
         *
         * @param a the styled attributes set
         * @param widthAttr the width attribute to fetch
         * @param heightAttr the height attribute to fetch
         */
        override fun setBaseAttributes(
            a: TypedArray,
            widthAttr: Int, heightAttr: Int
        ) {
            width = if (a.hasValue(widthAttr)) {
                a.getLayoutDimension(widthAttr, "layout_width")
            } else {
                ViewGroup.LayoutParams.WRAP_CONTENT
            }

            height = if (a.hasValue(heightAttr)) {
                a.getLayoutDimension(heightAttr, "layout_height")
            } else {
                ViewGroup.LayoutParams.WRAP_CONTENT
            }
        }
    }

    /**
     *
     * Interface definition for a callback to be invoked when the checked
     * [RadioItemView] changed in this group.
     */
    fun interface OnCheckedChangeListener {
        /**
         *
         * Called when the checked checked text has changed. When the
         * selection is cleared, checkedId is -1.
         *
         * @param group the group in which the checked checked text has changed
         * @param checkedId the unique identifier of the newly checked checked text
         */
        fun onCheckedChanged(group: RadioItemViewGroup?, @IdRes checkedId: Int)
    }

    private inner class CheckedStateTracker : RadioItemView.OnCheckedChangeListener {
        override fun onCheckedChanged(radioItem: RadioItemView, isChecked: Boolean) {
            // prevents from infinite recursion
            if (protectFromCheckedChange) {
                return
            }

            protectFromCheckedChange = true
            if (_checkedRadioButtonId != -1) {
                setCheckedStateForView(_checkedRadioButtonId, false)
            }
            protectFromCheckedChange = false

            val id: Int = radioItem.id
            setCheckedId(id)
        }

    }

    /**
     *
     * A pass-through listener acts upon the events and dispatches them
     * to another listener. This allows the table layout to set its own internal
     * hierarchy change listener without preventing the user to setup this.
     */
    private inner class PassThroughHierarchyChangeListener : OnHierarchyChangeListener {
        var mOnHierarchyChangeListener: OnHierarchyChangeListener? = null

        override fun onChildViewAdded(parent: View, child: View) {
            if (parent === this@RadioItemViewGroup && child is RadioItemView) {
                var id = child.id
                // generates an id if it's missing
                if (id == View.NO_ID) {
                    id = View.generateViewId()
                    child.id = id
                }
                child.setOnCheckedChangeWidgetListener(
                    childOnCheckedChangeListener
                )
            }

            mOnHierarchyChangeListener?.onChildViewAdded(parent, child)
        }

        override fun onChildViewRemoved(parent: View, child: View) {
            mOnHierarchyChangeListener?.onChildViewRemoved(parent, child)
        }
    }


    @RequiresApi(Build.VERSION_CODES.O)
    override fun autofill(value: AutofillValue) {
        if (!isEnabled) return

        if (!value.isList) {
            Log.w(LOG_TAG, "$value could not be autofilled into $this")
            return
        }

        val index: Int = value.getListValue()
        val child: View = getChildAt(index)

        check(child.id)
    }

    override fun onInitializeAccessibilityNodeInfo(info: AccessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfo(info)
        if (this.orientation == HORIZONTAL) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                AccessibilityNodeInfo.CollectionInfo(
                    1,
                    visibleChildWithTextCount,
                    false,
                    AccessibilityNodeInfo.CollectionInfo.SELECTION_MODE_SINGLE
                )
            }else{
                @Suppress("DEPRECATION")
                AccessibilityNodeInfo.CollectionInfo.obtain(
                    1,
                    visibleChildWithTextCount,
                    false,
                    AccessibilityNodeInfo.CollectionInfo.SELECTION_MODE_SINGLE
                )
            }.let {
                info.setCollectionInfo(it)
            }
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                AccessibilityNodeInfo.CollectionInfo(
                    visibleChildWithTextCount,
                    1,
                    false,
                    AccessibilityNodeInfo.CollectionInfo.SELECTION_MODE_SINGLE
                )
            }else{
                @Suppress("DEPRECATION")
                AccessibilityNodeInfo.CollectionInfo.obtain(
                    visibleChildWithTextCount,
                    1,
                    false,
                    AccessibilityNodeInfo.CollectionInfo.SELECTION_MODE_SINGLE
                )
            }.let {
                info.setCollectionInfo(it)
            }
        }
    }

    private val visibleChildWithTextCount: Int
        get() {
            var count = 0
            for (i in 0 until childCount) {
                if (this.getChildAt(i) is RadioItemView) {
                    if (isVisibleWithText(this.getChildAt(i) as RadioItemView)) {
                        count++
                    }
                }
            }
            return count
        }

    fun getIndexWithinVisibleButtons(child: View?): Int {
        if (child !is RadioItemView) {
            return -1
        }
        var index = 0
        for (i in 0 until childCount) {
            if (getChildAt(i) is RadioItemView) {
                val button = this.getChildAt(i) as RadioItemView
                if (button === child) {
                    return index
                }
                if (isVisibleWithText(button)) {
                    index++
                }
            }
        }
        return -1
    }

    private fun isVisibleWithText(button: RadioItemView): Boolean {
        return button.isVisible && !TextUtils.isEmpty(button.title)
    }


    companion object {
        private const val LOG_TAG: String = "RadioItemViewGroup"

        private const val MOTION_EVENT_ACTION_PEN_DOWN: Int = 211
        private const val MOTION_EVENT_ACTION_PEN_UP: Int = 212

    }
}
