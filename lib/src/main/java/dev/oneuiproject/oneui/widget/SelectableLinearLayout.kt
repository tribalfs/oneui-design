package dev.oneuiproject.oneui.widget

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.AnimatedVectorDrawable
import android.graphics.drawable.ColorDrawable
import android.util.AttributeSet
import android.view.Gravity
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.res.use
import androidx.core.view.isGone
import androidx.core.view.isVisible
import dev.oneuiproject.oneui.design.R

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

    private var mDisabledAlpha: Float = 0.4f
    private var mCheckBox: CheckBox? = null
    private lateinit var selectedHighlightColor: ColorDrawable
    private var mCheckMode: Int = 0
    private lateinit var mColorBackground: ColorDrawable

    private var checkDrawable: AnimatedVectorDrawable? = null
    private var uncheckDrawable: AnimatedVectorDrawable? = null
    private var imageTargetId: Int? = null
    private var imageTarget: ImageView? = null

    init {
        context.obtainStyledAttributes(
            intArrayOf(
                android.R.attr.colorBackground,
                android.R.attr.disabledAlpha
            ))
            .use{
                mColorBackground = ColorDrawable(it.getColor(0, 0))
                mDisabledAlpha = it.getFloat(1, .4f)
            }

        context.obtainStyledAttributes(attrs, R.styleable.SelectableLinearLayout).use {
            val color = it.getColor(R.styleable.SelectableLinearLayout_selectedHighlightColor,
                Color.parseColor("#08000000"))
            selectedHighlightColor = ColorDrawable(color)

            mCheckMode = it.getInt(R.styleable.SelectableLinearLayout_checkMode, 0)

            when (mCheckMode){
                0 ->{
                    val spacing = it.getDimensionPixelSize(
                        R.styleable.SelectableLinearLayout_checkableButtonSpacing, 14)
                    mCheckBox = CheckBox(context).apply {
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
                else -> {
                    if (mCheckMode == 1) {
                        checkDrawable = AppCompatResources.getDrawable(context,
                            R.drawable.ic_oui_check_selected) as AnimatedVectorDrawable
                        uncheckDrawable = AppCompatResources.getDrawable(context,
                            R.drawable.ic_oui_check_unselected) as AnimatedVectorDrawable
                    }
                    imageTargetId = it.getResourceId(R.styleable.SelectableLinearLayout_targetImage, 0)
                }
            }
        }
    }

    @SuppressLint("NewApi")
    override fun onFinishInflate() {
        super.onFinishInflate()
        if (mCheckMode == 1) {
            imageTarget = findViewById(imageTargetId!!)!!
            imageTargetId = null
            if (isInEditMode) {
                imageTarget!!.foreground = checkDrawable
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
            when (mCheckMode){
                0 -> {
                    mCheckBox!!.isVisible = value
                }
                else -> Unit
            }

        }


    /**
     * Use to toggle selected selected indicator and highlight on this view.
     */
    @SuppressLint("NewApi")
    override fun setSelected(isSelected: Boolean) {
        when (mCheckMode){
            0 ->  mCheckBox!!.isChecked = isSelected
            1 -> imageTarget!!.foreground = if (isSelected) checkDrawable else null

        }
        background = if (isSelected) selectedHighlightColor else null
    }

    /**
     * Use to toggle selected selected indicator and highlight on this view
     * with animation.
     */
    @SuppressLint("NewApi")
    fun setSelectedAnimate(isSelected: Boolean){
        when (mCheckMode){
            0 -> {
                mCheckBox!!.isChecked = isSelected
            }
            1 -> {
                if (isSelected) {
                    imageTarget!!.apply icon@ {
                        if (this@icon.foreground != checkDrawable) {
                            this@icon.foreground = checkDrawable
                            checkDrawable!!.start()
                        }
                    }
                }else{
                    imageTarget!!.apply icon@ {
                        if (this@icon.foreground == checkDrawable) {
                            this@icon.foreground = uncheckDrawable
                            uncheckDrawable!!.start()
                        }
                    }
                }
            }
        }
        background = if (isSelected) selectedHighlightColor else null

    }
}