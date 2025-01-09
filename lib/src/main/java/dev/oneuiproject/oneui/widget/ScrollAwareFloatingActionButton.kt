package dev.oneuiproject.oneui.widget

import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.view.isVisible
import androidx.indexscroll.widget.SeslIndexScrollView.OnIndexBarEventListener
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_IDLE
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import dev.oneuiproject.oneui.ktx.findAncestorOfType
import dev.oneuiproject.oneui.ktx.isDescendantOf
import dev.oneuiproject.oneui.layout.ToolbarLayout
import kotlinx.coroutines.Runnable
import java.lang.ref.WeakReference


/**
 * An extension of [FloatingActionButton] designed for integration with
 * [ToolbarLayout][dev.oneuiproject.oneui.layout.ToolbarLayout]. This specialized FAB
 * provides functionality to automatically hide itself during scrolling events
 * within a [RecyclerView] and with [AutoHideIndexScrollView].
 *
 * @see hideOnScroll
 */
class ScrollAwareFloatingActionButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null) : FloatingActionButton(context, attrs),
    AppBarLayout.OnOffsetChangedListener, RecyclerView.OnItemTouchListener {

    private enum class ScrollState{
        SCROLLING_UP, SCROLLING_DOWN, IDLE
    }

    private val showRunnable = Runnable { show() }

    private var setVisibility = View.VISIBLE
    private var isTouchingRv = false
    private var fastScrollerPressed = false
    private var scrollState: ScrollState = ScrollState.IDLE

    //This FAB might have a longer lifecycle than the RecyclerView
    // and AutoHideIndexScrollView.
    private var recyclerViewWR: WeakReference<RecyclerView>? = null
    private var indexScrollViewWR: WeakReference<AutoHideIndexScrollView>? = null

    private val indexBarEventListener by lazy {
        object : OnIndexBarEventListener {
            override fun onIndexChanged(sectionIndex: Int) {}

            override fun onPressed(v: Float) {
                fastScrollerPressed = true
                updateState()
            }

            override fun onReleased(v: Float) {
                fastScrollerPressed = false
                updateState()
            }
        }
    }

    /**
     * Shows the button.
     * This method will animate the button show if the view has already been laid out.
     * This has no effect when [setVisibility] is not set to [View.VISIBLE].
     */
    override fun show() {
        if (setVisibility == VISIBLE) {
            super.show()
        }
    }

    override fun hide() {
        removeCallbacks(showRunnable)
        super.hide()
    }

    private fun showDelayed() {
        removeCallbacks(showRunnable)
        postDelayed(showRunnable, 1_500)
    }

    override fun setVisibility(visibility: Int) {
        this.setVisibility = visibility
        when (visibility) {
            GONE, INVISIBLE -> hide()
            VISIBLE -> updateState()
        }
    }

    private fun updateState(){
        if (setVisibility != VISIBLE || fastScrollerPressed) {
            hide()
            return
        }
        when (scrollState){
            ScrollState.SCROLLING_UP -> {
                hide()
            }
            ScrollState.SCROLLING_DOWN -> {
                show()
            }
            ScrollState.IDLE -> {
                if (isTouchingRv) return
                showDelayed()
            }
        }
    }

    private val scrollListener by lazy {
        object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (dy > SCROLL_DELTA) {
                    scrollState = ScrollState.SCROLLING_UP
                    updateState()
                } else if (dy < -SCROLL_DELTA){
                    scrollState = ScrollState.SCROLLING_DOWN
                    updateState()
                }
            }

            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (newState == SCROLL_STATE_IDLE) {
                    scrollState = ScrollState.IDLE
                    updateState()
                }
            }
        }
    }


    /**
     * Hides this FAB if the provided [RecyclerView] is scrolling
     *
     * @param recyclerView
     * @param indexScrollView (Optional)
     */
    @JvmOverloads
    fun hideOnScroll(recyclerView: RecyclerView, indexScrollView: AutoHideIndexScrollView? = null) {
        recyclerViewWR = WeakReference(recyclerView)
        indexScrollViewWR = WeakReference(indexScrollView)
        if (isAttachedToWindow) {
            onAttachedToWindowInternal()
        }
    }

    private fun onAttachedToWindowInternal() {
        recyclerViewWR?.get()?.apply {
            removeOnScrollListener(scrollListener)
            addOnScrollListener(scrollListener)
            removeOnItemTouchListener(this@ScrollAwareFloatingActionButton)
            addOnItemTouchListener(this@ScrollAwareFloatingActionButton)
            if (Build.VERSION.SDK_INT >= 24) {
                seslSetFastScrollerEventListener(
                    object: RecyclerView.SeslFastScrollerEventListener {
                        override fun onPressed(scrollY: Float) {
                            fastScrollerPressed = true
                            updateState()
                        }

                        override fun onReleased(scrollY: Float) {
                            fastScrollerPressed = false
                            updateState()
                        }
                    }
                )
            }
        } ?: return

        indexScrollViewWR?.get()?.addOnIndexEventListener(indexBarEventListener)

        updateState()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        onAttachedToWindowInternal()
        offsetYIfInsideMainContainer(true)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        recyclerViewWR?.get()?.apply {
            removeOnScrollListener(scrollListener)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                seslSetFastScrollerEventListener(null)
            }
            setOnTouchListener(null)
            removeOnItemTouchListener(this@ScrollAwareFloatingActionButton)
        }
        indexScrollViewWR?.get()?.removeOnIndexEventListener(indexBarEventListener)
        offsetYIfInsideMainContainer(false)
    }

    private fun offsetYIfInsideMainContainer(register: Boolean){
        if (!layoutLocationInfo.isInsideTBLMainContainer) return
        layoutLocationInfo.tblParent!!.appBarLayout.apply {
            if (register){
                addOnOffsetChangedListener(this@ScrollAwareFloatingActionButton)
            } else {
                removeOnOffsetChangedListener(this@ScrollAwareFloatingActionButton)
            }
        }
    }

    private val layoutLocationInfo by lazy {
        findAncestorOfType<ToolbarLayout>()?.let {
            InternalLayoutInfo(this.isDescendantOf(it.mainContainer), it)
        } ?: InternalLayoutInfo(false)
    }

    override fun onOffsetChanged(appBarLayout: AppBarLayout, verticalOffset: Int) {
        if (!isVisible) return
        translationY = (verticalOffset + appBarLayout.totalScrollRange).toFloat() * -1f
    }

    private data class InternalLayoutInfo(
        val isInsideTBLMainContainer: Boolean,
        val tblParent: ToolbarLayout? = null
    )

    companion object {
        private const val TAG = "ScrollAwareFAB"
        private const val SCROLL_DELTA = 4f
    }

    override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
        when (e.action) {
            MotionEvent.ACTION_DOWN -> {
                isTouchingRv = true
                updateState()
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isTouchingRv = false
                updateState()
            }
        }
        return false
    }

    override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {}
    override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {}
}
