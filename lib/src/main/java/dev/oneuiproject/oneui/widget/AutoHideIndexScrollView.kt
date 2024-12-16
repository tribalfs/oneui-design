@file:Suppress("unused", "NOTHING_TO_INLINE")

package dev.oneuiproject.oneui.widget


import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.database.MatrixCursor
import android.icu.text.AlphabeticIndex
import android.os.Build
import android.util.AttributeSet
import android.view.MotionEvent
import android.window.OnBackInvokedCallback
import android.window.OnBackInvokedDispatcher
import android.window.OnBackInvokedDispatcher.PRIORITY_OVERLAY
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.res.use
import androidx.indexscroll.widget.SeslCursorIndexer
import androidx.indexscroll.widget.SeslIndexScrollView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_IDLE
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.AppBarLayout.OnOffsetChangedListener
import dev.oneuiproject.oneui.design.R
import dev.oneuiproject.oneui.ktx.doOnEnd
import dev.oneuiproject.oneui.ktx.doOnStart
import dev.oneuiproject.oneui.ktx.dpToPx
import dev.oneuiproject.oneui.utils.internal.CachedInterpolatorFactory
import dev.oneuiproject.oneui.utils.internal.CachedInterpolatorFactory.Type.SINE_IN_OUT_80
import dev.oneuiproject.oneui.widget.AutoHideIndexScrollView.AppBarState.COLLAPSED
import dev.oneuiproject.oneui.widget.AutoHideIndexScrollView.AppBarState.LITTLE_EXPANDED
import dev.oneuiproject.oneui.widget.AutoHideIndexScrollView.AppBarState.SUBSTANTIALLY_EXPANDED
import java.util.Locale

/**
 * Extension of [SeslIndexScrollView] with auto-hide feature
 * and supports RTL
 */
class AutoHideIndexScrollView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : SeslIndexScrollView(context, attrs) {

    @Volatile
    private var mIsIndexBarPressed = false
    @Volatile
    private var mIsRVScrolling = false
    private var mRecyclerView: RecyclerView? = null
    private var mAppBarLayout: AppBarLayout? = null
    private var mHideWhenExpandedListener: AppBarOffsetListener? = null
    private var mAutoHide: Boolean = true
    private var indexEventListeners: MutableSet<OnIndexBarEventListener>? = null

    private var mOnBackInvokedDispatcher: OnBackInvokedDispatcher? = null
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private var mDummyOnBackInvokedCallback: OnBackInvokedCallback? = null

    init{
        sSetVisibility = visibility
        context.obtainStyledAttributes(attrs, R.styleable.AutoHideIndexScrollView).use {
            setIndexer(
                SeslCursorIndexer(
                    MatrixCursor(arrayOf("")),
                    0,
                    getIndexes(),
                    0
                ).apply {
                    setGroupItemsCount(it.getInt(R.styleable.AutoHideIndexScrollView_groupItems, 0))
                    setMiscItemsCount(it.getInt(R.styleable.AutoHideIndexScrollView_otherItems, 0))
                }
            )
            setIndexBarTextMode(it.getBoolean(R.styleable.AutoHideIndexScrollView_textMode, true))
            setAutoHideInternal(it.getBoolean(R.styleable.AutoHideIndexScrollView_autoHide, true))
        }
    }

    private val rvScrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
            super.onScrollStateChanged(recyclerView, newState)
            (newState != SCROLL_STATE_IDLE).let {
                if (mIsRVScrolling != it){
                    mIsRVScrolling = it
                    updateVisibility(false, delayHide = true)
                }
            }
        }
    }

    private val showMeRunnable = Runnable {
        if (mHideWhenExpandedListener?.appBarState == SUBSTANTIALLY_EXPANDED
            || sSetVisibility != VISIBLE) return@Runnable
        animateVisibility(true)
    }

    private val hideMeRunnable = Runnable {
        animateVisibility( false)
    }

    private fun updateVisibility(delayShow: Boolean, delayHide: Boolean){
        removeCallbacks(hideMeRunnable)
        removeCallbacks(showMeRunnable)
        val show = if (mAutoHide) {
            mIsRVScrolling && mHideWhenExpandedListener?.appBarState != SUBSTANTIALLY_EXPANDED
        }else{
            sSetVisibility == VISIBLE && mHideWhenExpandedListener?.appBarState != SUBSTANTIALLY_EXPANDED
        }
        if (show) {
            postDelayed(showMeRunnable, if (delayShow) 400 else 0)
        }else{
            postDelayed(hideMeRunnable, if (delayHide) 1_500 else 0)
        }
    }

    override fun attachToRecyclerView(recyclerView: RecyclerView?) {
        super.attachToRecyclerView(recyclerView)
        if (mRecyclerView == recyclerView) return
        mRecyclerView = recyclerView
        if (isAttachedToWindow){
            onAttachedToWindowInternal()
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        onAttachedToWindowInternal()
    }

    private fun onAttachedToWindowInternal(){
        mRecyclerView?.apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                mOnBackInvokedDispatcher = findOnBackInvokedDispatcher()
                mDummyOnBackInvokedCallback = OnBackInvokedCallback { }
            }

            mAppBarLayout = rootView.findViewById<AppBarLayout?>(R.id.toolbarlayout_app_bar)?.apply {
                addOnOffsetChangedListener(
                    AppBarOffsetListener().also {
                        mHideWhenExpandedListener = it
                    }
                )
            }

            setIndexBarGravityInt(resources.configuration.layoutDirection)
            addOnScrollListener(rvScrollListener)
            setupEventListener(layoutManager as LinearLayoutManager)

            updateVisibility(false, delayHide = false)
        }
    }

    private fun setupEventListener(layoutManager: LinearLayoutManager) {
        super.setOnIndexBarEventListener(
            object : OnIndexBarEventListener {
                override fun onIndexChanged(sectionIndex: Int) {
                    if (mRecyclerView!!.scrollState != SCROLL_STATE_IDLE) {
                        mRecyclerView!!.stopScroll()
                    }
                    layoutManager.scrollToPositionWithOffset(sectionIndex, 0)
                    indexEventListeners?.forEach {
                        it.onIndexChanged(sectionIndex)
                    }
                }

                override fun onPressed(v: Float) {
                    mIsIndexBarPressed = true
                    if (mAutoHide) removeCallbacks(hideMeRunnable)
                    indexEventListeners?.forEach {
                        it.onPressed(v)
                    }
                }

                override fun onReleased(v: Float) {
                    mIsIndexBarPressed = false
                    updateVisibility(true, delayHide = true)
                    indexEventListeners?.forEach {
                        it.onReleased(v)
                    }
                }
            }
        )
    }

    @Deprecated("Use addOnIndexEventListener() and removeOnIndexEventListener() instead",
        ReplaceWith("addOnIndexEventListener(listener)"))
    override fun setOnIndexBarEventListener(iOnIndexBarEventListener: OnIndexBarEventListener?) {
        if (iOnIndexBarEventListener != null) {
            addOnIndexEventListener(iOnIndexBarEventListener)
        }
    }

    fun addOnIndexEventListener(listener: OnIndexBarEventListener){
        if (indexEventListeners == null) {
            indexEventListeners = mutableSetOf(listener)
            return
        }
        indexEventListeners?.add(listener)
    }

    fun removeOnIndexEventListener(listener: OnIndexBarEventListener){
        indexEventListeners?.forEach {
            if (it == listener) {
                indexEventListeners?.remove(it)
            }
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        if (mAutoHide) {
            removeCallbacks(hideMeRunnable)
            super.setOnIndexBarEventListener(null)
            mRecyclerView?.removeOnScrollListener(rvScrollListener)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            mOnBackInvokedDispatcher = null
            mDummyOnBackInvokedCallback = null
        }

        mAppBarLayout?.removeOnOffsetChangedListener(mHideWhenExpandedListener)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        setIndexBarGravityInt(newConfig.layoutDirection)
    }

    private fun setIndexBarGravityInt(layoutDirection: Int) {
        setIndexBarGravity(
            if (layoutDirection == LAYOUT_DIRECTION_RTL) {
                GRAVITY_INDEX_BAR_LEFT
            } else {
                GRAVITY_INDEX_BAR_RIGHT
            }
        )
    }

    private fun animateVisibility(show: Boolean) {
        if (isShown && show) return
        animate().apply {
            cancel()
            if (show) {
                alpha(1f)
                duration = 200
                interpolator = CachedInterpolatorFactory.getOrCreate(SINE_IN_OUT_80)
                doOnStart {
                    alpha = 0f
                    super.setVisibility(VISIBLE)
                }
            } else {
                alpha(0f)
                duration = 150
                doOnEnd {
                    super.setVisibility(GONE)
                    alpha = 1f
                }
            }
            start()
        }
    }

    fun setAutoHide(autoHide: Boolean){
        if (this.mAutoHide == autoHide) return
        setAutoHideInternal(autoHide)
    }

    private fun setAutoHideInternal(autoHide: Boolean){
        if (isInEditMode) return
        this.mAutoHide = autoHide
        if (autoHide) {
            mRecyclerView?.addOnScrollListener(rvScrollListener)
        } else {
            mRecyclerView?.removeOnScrollListener(rvScrollListener)
        }
        updateVisibility(false, delayHide = false)
    }

    @SuppressLint("NewApi")
    private inline fun getIndexes():  Array<String> {
        if (isInEditMode){
            return "A,B,C,D,E,F,G,H,I,J,K,L,M,N,O,P,Q,R,S,T,U,V,W,X,Y,Z".split(",").toTypedArray()
        }

        val locales = AppCompatDelegate.getApplicationLocales()
        val alphabeticIndex = AlphabeticIndex<Int>(locales[0])
        for (i in 1 until locales.size()) {
            alphabeticIndex.addLabels(locales[i])
        }
        alphabeticIndex.addLabels(Locale.ENGLISH)
        return alphabeticIndex.buildImmutableIndex().toSet().map { it.toString() }.toTypedArray()
    }

    private enum class AppBarState{
        COLLAPSED,
        LITTLE_EXPANDED,
        SUBSTANTIALLY_EXPANDED
    }

    private inner class AppBarOffsetListener : OnOffsetChangedListener {
        @Volatile
        var appBarState: AppBarState = COLLAPSED

        override fun onOffsetChanged(layout: AppBarLayout, verticalOffset: Int) {
            val totalScrollRange = layout.totalScrollRange
            val newAppBarState = when{
                (totalScrollRange + verticalOffset) > totalScrollRange / 3 -> SUBSTANTIALLY_EXPANDED
                 totalScrollRange + verticalOffset == 0 -> COLLAPSED
                else -> LITTLE_EXPANDED
            }
            if (appBarState == newAppBarState) return
            appBarState = newAppBarState
            updateVisibility(delayShow = false, delayHide = false)
        }
    }

    override fun setVisibility(visibility: Int) {
        sSetVisibility = visibility
        super.setVisibility(visibility)
        updateVisibility(false, delayHide = false)
    }

    private var dumbCallbackRegistered = false

    @Suppress("NewApi")
    override fun onTouchEvent(ev: MotionEvent): Boolean {
        when (ev.action){
            MotionEvent.ACTION_DOWN -> {
                mOnBackInvokedDispatcher?.apply {
                    if (dumbCallbackRegistered || !shouldRegisterDummy(ev.x)) return@apply
                    dumbCallbackRegistered = true
                    registerOnBackInvokedCallback(PRIORITY_OVERLAY, mDummyOnBackInvokedCallback!!)
                }
            }
            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> {
                if (dumbCallbackRegistered) {
                    dumbCallbackRegistered = false
                    mOnBackInvokedDispatcher?.unregisterOnBackInvokedCallback(mDummyOnBackInvokedCallback!!)
                }
            }
        }
        return super.onTouchEvent(ev)
    }

    @RequiresApi(33)
    private fun shouldRegisterDummy(x: Float): Boolean{
        val scrollViewWidth = resources.getDimension(androidx.indexscroll.R.dimen.sesl_indexbar_textmode_width)
        val backGestureAllowance = 22.4f.dpToPx(resources).toFloat()

        val (startX, endX) = if (layoutDirection == LAYOUT_DIRECTION_LTR) {
            val end = width - backGestureAllowance
            end - scrollViewWidth to end
        } else {
            backGestureAllowance to backGestureAllowance + scrollViewWidth
        }
        return (x > startX && x < endX)
    }

    companion object{
        private var sSetVisibility: Int = -1
        private const val TAG = "AutoHideIndexScrollView"
    }
}