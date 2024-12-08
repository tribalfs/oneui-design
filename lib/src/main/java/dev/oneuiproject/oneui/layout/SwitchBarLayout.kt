package dev.oneuiproject.oneui.layout

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.widget.SeslSwitchBar
import dev.oneuiproject.oneui.design.R


/**
 * [ToolbarLayout] with a [SeslSwitchBar].
 *
 * This component is deprecated and will be removed in future releases.
 *
 * @deprecated Use `ToolbarLayout` instead, which now supports `SeslSwitchBar`.
 * The `SeslSwitchBar` can be accessed directly through the `switchBar` field in `ToolbarLayout`.
 */
@Deprecated("Use ToolbarLayout which now supports SeslSwitchBar that can be shown " +
        "by setting app:showSwitchBar=\"true\"")
class SwitchBarLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null) : ToolbarLayout(context, attrs)