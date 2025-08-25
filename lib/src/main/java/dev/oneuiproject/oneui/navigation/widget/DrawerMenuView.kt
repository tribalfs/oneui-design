package dev.oneuiproject.oneui.navigation.widget

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.view.menu.MenuView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

/** @hide
 */
@SuppressLint("RestrictedApi")
internal class DrawerMenuView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int =  androidx.recyclerview.R.attr.recyclerViewStyle
) : RecyclerView(context, attrs, defStyleAttr), MenuView {

    init {
        layoutManager = LinearLayoutManager(context, VERTICAL, false)
        seslSetLastRoundedCorner(false)
        seslSetGoToTopEnabled(false)
        setWillNotDraw(true)
    }

    override fun initialize(menu: MenuBuilder) = Unit

    override fun getWindowAnimations(): Int = 0
}
