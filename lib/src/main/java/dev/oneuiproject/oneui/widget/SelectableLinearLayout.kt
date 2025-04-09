package dev.oneuiproject.oneui.widget

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
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
import androidx.core.content.res.use
import androidx.core.view.isGone
import androidx.core.view.isVisible
import dev.oneuiproject.oneui.design.R
import dev.oneuiproject.oneui.widget.internal.SelectableAnimatedDrawable

/**
 * A custom [LinearLayout] designed to be used as a selectable item within a
 * [RecyclerView][androidx.recyclerview.widget.RecyclerView]. This layout provides
 * functionality to display an action mode and a selected indicator, which can be
 * either a [CheckBox] or an animated check overlay. It also supports highlighting
 * to visually distinguish selected items.
 */
@SuppressLint("ResourceType")
class SelectableLinearLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr:Int = 0,
    defStyleRes: Int = 0
) : LinearLayout(context, attrs, defStyleAttr, defStyleRes) {

    private lateinit var selectedHighlightColor: ColorDrawable
    private var checkMode: Int = CHECK_MODE_CHECKBOX

    //For CHECK_MODE_CHECKBOX
    private var mCheckBox: CheckBox? = null

    //For CHECK_MODE_OVERLAY
    private var checkDrawable: SelectableAnimatedDrawable? = null
    private var imageTargetId: Int? = null
    private var imageTarget: ImageView? = null

    init {
        context.obtainStyledAttributes(attrs, R.styleable.SelectableLinearLayout).use {
            val color = it.getColor(R.styleable.SelectableLinearLayout_selectedHighlightColor,
                Color.parseColor("#08000000"))
            selectedHighlightColor = ColorDrawable(color)

            checkMode = it.getInt(R.styleable.SelectableLinearLayout_checkMode, CHECK_MODE_CHECKBOX)

            when (checkMode){
                0 ->{
                    val spacing = it.getDimensionPixelSize(
                        R.styleable.SelectableLinearLayout_checkableButtonSpacing, 14)
                    mCheckBox = AppCompatCheckBox(context).apply {
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
                    addView(mCheckBox, 0)
                }
                1 -> {
                    checkDrawable = SelectableAnimatedDrawable.create(context, R.drawable.oui_des_list_item_selection_anim_selector, context.theme)
                    if (it.hasValue(R.styleable.SelectableLinearLayout_cornerRadius)){
                        checkDrawable!!.setCornerRadius(
                            it.getDimension(R.styleable.SelectableLinearLayout_cornerRadius, 0f)
                        )
                    }
                    imageTargetId = it.getResourceId(R.styleable.SelectableLinearLayout_targetImage, 0)
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
                0 -> mCheckBox!!.isVisible = value
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
            0 -> mCheckBox!!.isChecked = isSelected
            1 -> {
                if (Build.VERSION.SDK_INT < 23) {
                    imageTarget!!.imageAlpha = if (!isSelected) 255 else 0
                }
                imageTarget!!.isSelected = isSelected
            }
        }
        background = if (isSelected) selectedHighlightColor else null
    }

    fun setOverlayCornerRadius(@Px radius: Float){
        when (checkMode){
            1 -> checkDrawable!!.setCornerRadius(radius)
            else -> Log.w(TAG, "setOverlayCornerRadius only applies to overlayCircle mode.")
        }
    }

    companion object{
        private const val TAG = "SelectableLinearLayout"
        private const val CHECK_MODE_CHECKBOX = 0
        private const val CHECK_MODE_OVERLAY = 1
    }
}