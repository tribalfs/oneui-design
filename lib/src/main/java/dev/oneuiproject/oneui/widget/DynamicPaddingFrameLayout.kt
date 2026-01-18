package dev.oneuiproject.oneui.widget

import android.content.Context
import android.content.res.Configuration
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.core.view.marginEnd
import androidx.core.view.marginStart
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding

/**
 * A [android.widget.FrameLayout] that allows for dynamic horizontal padding.
 *
 * This layout uses a [PaddingProvider] to dynamically calculate and apply horizontal padding.
 * The padding is automatically updated when the view is attached to a window, when the
 * device configuration changes, or when the view's size changes.
 *
 * @param context The Context the view is running in, through which it can
 * access the current theme, resources, etc.
 * @param attrs The attributes of the XML tag that is inflating the view.
 * @param defStyleAttr An attribute in the current theme that contains a
 * reference to a style resource that supplies default values for
 * the view. Can be 0 to not look for defaults.
 * @param defStyleRes A resource identifier of a style resource that
 * supplies default values for the view, used only if
 * defStyleAttr is 0 or can not be found in the theme. Can be 0
 * to not look for defaults.
 */
open class DynamicPaddingFrameLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : FrameLayout(context, attrs, defStyleAttr, defStyleRes) {

    /**
     * A functional interface for providing side margin values.
     */
    fun interface PaddingProvider {
        /**
         * Returns the horizontal padding value.
         *
         * @param context The context to use for calculating the padding.
         * @return The horizontal padding in pixels.
         */
        fun getSidePadding(context: Context): Int
    }

    private var paddingProviderImpl: PaddingProvider? = null

    /**
     * Sets the [PaddingProvider] for this layout.
     *
     * The provider will be used to calculate the side margins. If the view
     * is already attached to a window, the margins will be updated immediately.
     *
     * @param provider The [PaddingProvider] to use, or null to clear it.
     */
    fun setPaddingProvider(provider: PaddingProvider?) {
        paddingProviderImpl = provider
        if (!isAttachedToWindow) return
        updateSidePadding()
    }


    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        updateSidePadding()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        updateSidePadding()
        super.onConfigurationChanged(newConfig)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        if (w != oldw) updateSidePadding()
        super.onSizeChanged(w, h, oldw, oldh)
    }

    /**
     * Updates the horizontal padding of the layout using the [PaddingProvider].
     *
     * If the provider is null, the padding is set to 0.
     */
    private fun updateSidePadding() {
        (paddingProviderImpl?.getSidePadding(context) ?: 0).let { sp ->
            if (sp != paddingStart || sp != paddingEnd) {
                updatePadding(left = sp, right = sp)
            }
        }
    }
}