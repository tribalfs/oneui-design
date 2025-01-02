package dev.oneuiproject.oneui.widget

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.util.AttributeSet
import android.view.Gravity
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.res.use
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat
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

    private var checkDrawable: AnimatedVectorDrawableCompat? = null
    private var uncheckDrawable: AnimatedVectorDrawableCompat? = null
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

            if (Build.VERSION.SDK_INT >= 23) {
                mCheckMode = it.getInt(R.styleable.SelectableLinearLayout_checkMode, 0)
            }

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
                        checkDrawable = AnimatedVectorDrawableCompat.create(context,
                            R.drawable.ic_oui_check_selected) as AnimatedVectorDrawableCompat
                        uncheckDrawable = AnimatedVectorDrawableCompat.create(context,
                            R.drawable.ic_oui_check_unselected) as AnimatedVectorDrawableCompat
                    }
                    imageTargetId = it.getResourceId(R.styleable.SelectableLinearLayout_targetImage, 0)
                }
            }
        }
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        if (mCheckMode == 1) {
            imageTarget = findViewById(imageTargetId!!)!!
            imageTargetId = null
            if (isInEditMode) {
                @Suppress("NewApi")
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
     * Use to toggle selected state indicators on this view
     * @see setSelectedAnimate
     */
    override fun setSelected(isSelected: Boolean) {
        when (mCheckMode){
            0 ->  mCheckBox!!.isChecked = isSelected
            1 -> {
                @Suppress("NewApi")
                imageTarget!!.foreground = if (isSelected) checkDrawable else null
            }

        }
        background = if (isSelected) selectedHighlightColor else null
    }

    /**
     * Same as [setSelected] but
     * will animate the check drawable when `checkMode` is set to `overlayCircle`.
     */
    fun setSelectedAnimate(isSelected: Boolean){
        when (mCheckMode){
            0 -> {
                mCheckBox!!.isChecked = isSelected
            }
            1 -> {
                @Suppress("NewApi")
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