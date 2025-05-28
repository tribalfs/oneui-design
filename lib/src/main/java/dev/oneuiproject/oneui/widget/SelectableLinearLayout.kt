package dev.oneuiproject.oneui.widget

import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.util.AttributeSet
import android.util.Log
import android.view.Gravity
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.annotation.Px
import androidx.appcompat.widget.AppCompatCheckBox
import androidx.core.content.withStyledAttributes
import androidx.core.graphics.drawable.toDrawable
import androidx.core.graphics.toColorInt
import androidx.core.view.isGone
import androidx.core.view.isVisible
import dev.oneuiproject.oneui.design.R
import dev.oneuiproject.oneui.widget.internal.SelectableAnimatedDrawable

/**
 * A custom [LinearLayout] designed to be used as a selectable item of a
 * [recyclerview][androidx.recyclerview.widget.RecyclerView]. This layout provides
 * functionality to display OneUI-styled action mode and check mode (for selected state) indicators
 * on recyclerview items. The check mode can be either a [CheckBox] or an animated check overlay drawable.
 *
 * The check mode can be set using the `app:checkMode` attribute.
 * - `app:checkMode:"checkBox"`: When this is set, `app:checkableButtonSpacing` attribute
 * can also be set to adjust the spacing (in pixels) between the [CheckBox] and the content.
 * - `app:checkMode:"overlayCircle"`: When this is set, `app:cornerRadius` attribute
 * can also be set to adjust the corner radius of the check drawable overlay. This
 * can also be set dynamically using [setOverlayCornerRadius].
 *
 *  This view also supports highlighting when selected for additional visual distinction.
 *  A custom highlight color can be set using the `app:selectedHighlightColor` attribute.
 *  This color is set to `#08000000` by default.
 *
 * ## Example usage:
 * ```xml
 * <dev.oneuiproject.oneui.widget.SelectableLinearLayout
 *     android:layout_width="match_parent"
 *     android:layout_height="?android:listPreferredItemHeight"
 *     android:animateLayoutChanges="true"
 *     android:background="?listChoiceBackgroundIndicator"
 *     android:gravity="center_vertical"
 *     android:orientation="horizontal"
 *     android:paddingStart="?android:listPreferredItemPaddingStart"
 *     android:paddingEnd="?android:listPreferredItemPaddingEnd"
 *     app:checkMode="checkBox">
 *
 *     <!-- Child views -->
 *
 * </dev.oneuiproject.oneui.widget.SelectableLinearLayout>
 * ```
 * @param context The Context the view is running in, through which it can access the
 * current theme, resources, etc.
 * @param attrs (Optional) The attributes of the XML tag that is inflating the view.
 * @param defStyleAttr (Optional) An attribute in the current theme that contains a
 * reference to a style resource that supplies default values for the view.
 * @param defStyleRes (Optional) A resource identifier of a style resource that
 * supplies default values for the view, used only if defStyleAttr is not provided
 * or cannot be found in the theme.
 */
class SelectableLinearLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr:Int = 0,
    defStyleRes: Int = 0
) : LinearLayout(context, attrs, defStyleAttr, defStyleRes) {

    private lateinit var selectedHighlightColor: ColorDrawable
    private var checkMode: Int = CHECK_MODE_CHECKBOX

    //For CHECK_MODE_CHECKBOX
    private var checkBox: CheckBox? = null

    //For CHECK_MODE_OVERLAY
    private var checkDrawable: SelectableAnimatedDrawable? = null
    private var imageTargetId: Int? = null
    private var imageTarget: ImageView? = null

    init {
        context.withStyledAttributes(attrs, R.styleable.SelectableLinearLayout) {
            val color = getColor(R.styleable.SelectableLinearLayout_selectedHighlightColor, "#08000000".toColorInt())
            selectedHighlightColor = color.toDrawable()

            checkMode = getInt(R.styleable.SelectableLinearLayout_checkMode, CHECK_MODE_CHECKBOX)

            when (checkMode){
                0 ->{
                    val spacing = getDimensionPixelSize(
                        R.styleable.SelectableLinearLayout_checkableButtonSpacing, 14)
                    checkBox = AppCompatCheckBox(context).apply {
                        layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                            gravity = Gravity.CENTER
                            marginEnd = spacing
                            marginStart = -4
                        }
                        isClickable = false
                        isLongClickable = false
                        isGone = true
                        background = null //to remove ripple
                    }
                    addView(checkBox, 0)
                }
                1 -> {
                    checkDrawable = SelectableAnimatedDrawable.create(context, R.drawable.oui_des_list_item_selection_anim_selector, context.theme)
                    if (hasValue(R.styleable.SelectableLinearLayout_cornerRadius)){
                        checkDrawable!!.setCornerRadius(
                            getDimension(R.styleable.SelectableLinearLayout_cornerRadius, 0f)
                        )
                    }
                    imageTargetId = getResourceId(R.styleable.SelectableLinearLayout_targetImage, 0)
                }
            }
        }
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        if (checkMode == CHECK_MODE_OVERLAY) {
            imageTarget = findViewById(imageTargetId!!)!!
            imageTargetId = null
            if (Build.VERSION.SDK_INT >= 23) {
                imageTarget!!.foreground = checkDrawable
            }else{
                imageTarget!!.background = checkDrawable
            }
        }
    }

    /**
     * Activate or deactivate selection mode on this view
     */
    var isSelectionMode: Boolean = false
        set(value) {
            if (field == value) return
            field = value
            when (checkMode){
                0 -> checkBox!!.isVisible = value
                else -> Unit
            }
        }

    /**
     * Use to toggle selected state indicators on this view
     * @see setSelectedAnimate
     */
    override fun setSelected(isSelected: Boolean) {
        setSelectedAnimate(isSelected)
        checkDrawable?.jumpToCurrentState()
     }

    /**
     * Same as [setSelected] but
     * will animate the check drawable when `checkMode` is set to `overlayCircle`.
     */
    fun setSelectedAnimate(isSelected: Boolean){
        when (checkMode){
            0 -> checkBox!!.isChecked = isSelected
            1 -> {
                if (Build.VERSION.SDK_INT < 23) {
                    imageTarget!!.imageAlpha = if (!isSelected) 255 else 0
                }
                imageTarget!!.isSelected = isSelected
            }
        }
        background = if (isSelected) selectedHighlightColor else null
    }

    /**
     * Sets the corner radius of the check drawable overlay
     * when `app:checkMode:overlayCircle` is set
     *
     * By default, this is set to 50% of the height of the target view
     */
    fun setOverlayCornerRadius(@Px radius: Float){
        when (checkMode){
            1 -> checkDrawable!!.setCornerRadius(radius)
            else -> Log.w(TAG, "setOverlayCornerRadius only applies to overlayCircle mode.")
        }
    }

    private companion object{
        const val TAG = "SelectableLinearLayout"
        const val CHECK_MODE_CHECKBOX = 0
        const val CHECK_MODE_OVERLAY = 1
    }
}