package dev.oneuiproject.oneui.widget

import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.util.Log
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.Runnable
import java.lang.ref.WeakReference


class ScrollAwareFloatingActionButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FloatingActionButton(context, attrs) {

    private val showRunnable = Runnable { show() }

    private var setVisibility = View.VISIBLE
    private var _temporaryHide = false

    //This FAB might have a longer lifecycle than the RecyclerView.
    private var recyclerViewWR: WeakReference<RecyclerView>? = null
    private var lastRVScrollState = RecyclerView.SCROLL_STATE_IDLE

    /**
     * Set to true to temporarily hide the FAB.
     * This will be helpful for cases like when on search mode or action mode
     * where this FAB needs to be temporarily hidden.
     *
     */
    var temporaryHide: Boolean
        get() = _temporaryHide
        set(value) {
            _temporaryHide = value
            if (value) {
                _temporaryHide = true
                removeCallbacks(showRunnable)
                hide()
            } else {
                show()
            }
        }

    /**
     * Shows the button.
     * This method will animate the button show if the view has already been laid out.
     * This has no effect when [temporaryHide] is true or [setVisibility] is not set to [View.VISIBLE].
     */
    override fun show() {
        if (_temporaryHide) {
            Log.w(TAG, "calling show")
            return
        }
        if (setVisibility == VISIBLE) {
            super.show()
        }
    }

    private val scrollListener by lazy {
        object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                lastRVScrollState = newState
                if (_temporaryHide) return
                when (newState) {
                    RecyclerView.SCROLL_STATE_IDLE -> showDelayed()
                    else -> hide()
                }
            }
        }
    }

    private fun showDelayed() {
        removeCallbacks(showRunnable)
        postDelayed(showRunnable, 1200)
    }

    override fun hide() {
        removeCallbacks(showRunnable)
        super.hide()
    }

    /**
     * Hides this FAB if the provided [RecyclerView] is scrolling
     *
     * @param recyclerView
     */
    fun hideOnScroll(recyclerView: RecyclerView) {
        recyclerViewWR = WeakReference(recyclerView)
        if (isAttachedToWindow) {
            onAttachedToWindowInternal()
        }
    }

    private fun onAttachedToWindowInternal() {
        val recyclerView = recyclerViewWR?.get() ?: return

        recyclerView.removeOnScrollListener(scrollListener)
        recyclerView.addOnScrollListener(scrollListener)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            recyclerView.seslSetFastScrollerEventListener(
                object : RecyclerView.SeslFastScrollerEventListener {
                    override fun onPressed(scrollY: Float) {
                        if (!_temporaryHide) {
                            removeCallbacks(showRunnable)
                            hide()
                        }
                    }

                    override fun onReleased(scrollY: Float) {
                        if (!_temporaryHide) {
                            showDelayed()
                        }
                    }

                }
            )
        }
        if (lastRVScrollState == RecyclerView.SCROLL_STATE_IDLE) showDelayed()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        onAttachedToWindowInternal()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        hide()
        recyclerViewWR?.get()?.apply {
            removeOnScrollListener(scrollListener)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                seslSetFastScrollerEventListener(null)
            }
        }
    }


    override fun setVisibility(visibility: Int) {
        this.setVisibility = visibility
        when (visibility) {
            GONE, INVISIBLE -> {
                if (isOrWillBeHidden) return
                removeCallbacks(showRunnable)
                super.setVisibility(visibility)
            }

            VISIBLE -> {
                if (_temporaryHide && !isOrWillBeHidden) {
                    super.setVisibility(GONE)
                    return
                }
            }
        }
    }


    companion object {
        private const val TAG = "ScrollAwareFAB"
    }

}