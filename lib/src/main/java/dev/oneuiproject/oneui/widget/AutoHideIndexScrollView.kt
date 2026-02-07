@file:Suppress("unused", "NOTHING_TO_INLINE")

package dev.oneuiproject.oneui.widget


import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.database.MatrixCursor
import android.graphics.Rect
import android.icu.text.AlphabeticIndex
import android.os.Build
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.window.OnBackInvokedCallback
import android.window.OnBackInvokedDispatcher
import android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.withStyledAttributes
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.indexscroll.widget.SeslCursorIndexer
import androidx.indexscroll.widget.SeslIndexScrollView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_IDLE
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.AppBarLayout.OnOffsetChangedListener
import dev.oneuiproject.oneui.design.R
import dev.oneuiproject.oneui.layout.internal.util.ToolbarLayoutUtils.InternalLayoutInfo
import dev.oneuiproject.oneui.layout.internal.util.ToolbarLayoutUtils.getLayoutLocationInfo
import dev.oneuiproject.oneui.utils.internal.CachedInterpolatorFactory
import dev.oneuiproject.oneui.utils.internal.CachedInterpolatorFactory.Type.SINE_IN_OUT_80
import dev.oneuiproject.oneui.widget.AutoHideIndexScrollView.AppBarState.COLLAPSED
import dev.oneuiproject.oneui.widget.AutoHideIndexScrollView.AppBarState.LITTLE_EXPANDED
import dev.oneuiproject.oneui.widget.AutoHideIndexScrollView.AppBarState.SUBSTANTIALLY_EXPANDED
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.LazyThreadSafetyMode.NONE
import kotlin.math.abs

/**
 * An extension of [SeslIndexScrollView] that adds an auto-hide feature for the index bar,
 * supports multiple [OnIndexBarEventListener]s, and provides full RTL (right-to-left) layout support.
 *
 * This view is designed to be used alongside a [RecyclerView] to display an index bar
 * (typically for fast scrolling through large lists) at the side of the [RecyclerView].
 *
 * This view adds the following features:
 * - Automatically hides and shows the index bar based on user interaction and scroll state.
 * - Allows registration of multiple [OnIndexBarEventListener]s for handling index bar events.
 * - Fully supports RTL layouts for languages that require it.
 *
 * ## Example usage:
 * ```xml
 *  <dev.oneuiproject.oneui.widget.RoundedFrameLayout
 *      android:layout_width="match_parent"
 *      android:layout_height="match_parent"
 *      tools:viewBindingIgnore="true">
 *
 *      <dev.oneuiproject.oneui.widget.AutoHideIndexScrollView
 *          android:layout_width="match_parent"
 *          android:layout_height="match_parent"
 *          app:autoHide="true"
 *          app:textMode="true"
 *          android:layout_marginHorizontal="10dp"/>
 *
 *      <androidx.recyclerview.widget.RecyclerView
 *          android:layout_width="match_parent"
 *          android:layout_height="match_parent"
 *          android:paddingHorizontal="10dp"
 *          android:background="?android:colorBackground" />
 *  </dev.oneuiproject.oneui.widget.RoundedFrameLayout>
 * ```
 * @param context The Context the view is running in, through which it can access the
 * current theme, resources, etc.
 * @param attrs (Optional) The attributes of the XML tag that is inflating the view.
 *
 * @see setAutoHide
 * @see addOnIndexEventListener
 * @see removeOnIndexEventListener
 */
class AutoHideIndexScrollView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : SeslIndexScrollView(context, attrs) {

    private var lastSetVisibility: Int = -1
    private var isIndexBarPressed = false
    private var isRVScrolling = false
    private var recyclerView: RecyclerView? = null
    private var appBarLayout: AppBarLayout? = null
    private var hideWhenExpandedListener: AppBarOffsetListener? = null
    private var autoHide: Boolean = true
    private var indexEventListeners: MutableSet<OnIndexBarEventListener>? = null
    private var layoutLocationInfo: InternalLayoutInfo? = null

    private var onBackInvokedDispatcher: OnBackInvokedDispatcher? = null
    @RequiresApi(33)
    private var dummyOnBackInvokedCallback: OnBackInvokedCallback? = null

    private var isImmersive = false

    private var mainJob: Job? = null
    private var scope: CoroutineScope? = null
    private var showMeJob: Job? = null
    private var hideMeJob: Job? = null

    private val immersiveStateListener: (Float) -> Unit by lazy(NONE) {
        { setIndexScrollMarginInternal(topMargin, bottomMargin + it.toInt()) }
    }

    init{
        lastSetVisibility = visibility
        context.withStyledAttributes(attrs, R.styleable.AutoHideIndexScrollView) {
            setIndexer(
                SeslCursorIndexer(
                    MatrixCursor(arrayOf("")),
                    0,
                    getIndexes(),
                    0
                ).apply {
                    setGroupItemsCount(getInt(R.styleable.AutoHideIndexScrollView_groupItems, 0))
                    setMiscItemsCount(getInt(R.styleable.AutoHideIndexScrollView_otherItems, 0))
                }
            )
            setIndexBarTextMode(getBoolean(R.styleable.AutoHideIndexScrollView_textMode, true))
            setAutoHideInternal(getBoolean(R.styleable.AutoHideIndexScrollView_autoHide, true))
        }
        setIndexBarGravityInt(resources.configuration.layoutDirection)
    }

    private val rvScrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
            super.onScrollStateChanged(recyclerView, newState)
            (newState != SCROLL_STATE_IDLE).let {
                if (isRVScrolling != it){
                    isRVScrolling = it
                    updateVisibility()
                }
            }
        }
    }

    private fun startShowMe() {
        if (showMeJob?.isActive == true) return
        showMeJob = scope?.launch {
            delay(100)
            if (hideWhenExpandedListener?.appBarState == SUBSTANTIALLY_EXPANDED
                || lastSetVisibility != VISIBLE) return@launch
            animateVisibility(show = true)
        }
    }


    private fun startHideMe(delay: Long = 0) {
        if (hideMeJob?.isActive == true) return
        hideMeJob = scope?.launch {
            delay(delay)
            animateVisibility(show = false)
        }
    }

    private fun updateVisibility(delayedHide: Boolean = true){
        val show = if (autoHide) {
            isRVScrolling && hideWhenExpandedListener?.appBarState != SUBSTANTIALLY_EXPANDED
        }else{
            lastSetVisibility == VISIBLE && hideWhenExpandedListener?.appBarState != SUBSTANTIALLY_EXPANDED
        }
        if (show) {
            hideMeJob?.cancel()
            startShowMe()
        }else{
            showMeJob?.cancel()
            startHideMe(if (delayedHide) 1_500 else 0)
        }
    }

    override fun attachToRecyclerView(recyclerView: RecyclerView) {
        super.attachToRecyclerView(recyclerView)
        if (this.recyclerView === recyclerView) return
        this.recyclerView?.removeOnScrollListener(rvScrollListener)
        this.recyclerView = recyclerView
        if (isAttachedToWindow){
            onAttachedToWindowInternal()
        }
    }


    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        scope = CoroutineScope(Dispatchers.Main + SupervisorJob().also { mainJob = it })
        onAttachedToWindowInternal()
        layoutLocationInfo = layoutLocationInfo ?: getLayoutLocationInfo()
        layoutLocationInfo?.takeIf { it.isInsideTBLMainContainer }?.tblParent?.apply {
            isImmersive = isImmersiveScroll
            addOnBottomOffsetChangedListener(immersiveStateListener)
        }
    }


    private fun onAttachedToWindowInternal(){
        recyclerView?.apply {
            configureDummyCallback(true)

            if (hideWhenExpandedListener == null || appBarLayout == null){
                hideWhenExpandedListener = AppBarOffsetListener()
                appBarLayout = rootView.findViewById<AppBarLayout?>(R.id.toolbarlayout_app_bar)
            }

            // Note: AppBarLayout internally guards duplicate listeners
            appBarLayout?.addOnOffsetChangedListener(hideWhenExpandedListener)

            addOnScrollListener(rvScrollListener)
            setupIndexEventListener(layoutManager as LinearLayoutManager)
            updateVisibility()
        }
    }


    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        super.setOnIndexBarEventListener(null)
        if (autoHide) recyclerView?.removeOnScrollListener(rvScrollListener)
        configureDummyCallback(false)

        layoutLocationInfo?.takeIf { it.isInsideTBLMainContainer }?.tblParent?.apply {
            this@AutoHideIndexScrollView.appBarLayout?.removeOnOffsetChangedListener(hideWhenExpandedListener)
            removeOnBottomOffsetChangedListener (immersiveStateListener)
        }
        mainJob?.cancel()
        scope = null
    }

    private fun setupIndexEventListener(layoutManager: LinearLayoutManager) {
        super.setOnIndexBarEventListener(
            object : OnIndexBarEventListener {
                override fun onIndexChanged(sectionIndex: Int) {
                    if (recyclerView!!.scrollState != SCROLL_STATE_IDLE) {
                        recyclerView!!.stopScroll()
                    }
                    layoutManager.scrollToPositionWithOffset(sectionIndex, 0)
                    indexEventListeners?.forEach { it.onIndexChanged(sectionIndex) }
                }

                override fun onPressed(v: Float) {
                    isIndexBarPressed = true
                    hideMeJob?.cancel()
                    indexEventListeners?.forEach { it.onPressed(v) }
                }

                override fun onReleased(v: Float) {
                    isIndexBarPressed = false
                    updateVisibility()
                    indexEventListeners?.forEach { it.onReleased(v) }
                }
            }
        )
    }

    @Deprecated("Use addOnIndexEventListener() and removeOnIndexEventListener() instead",
        ReplaceWith("addOnIndexEventListener(listener)"))
    override fun setOnIndexBarEventListener(onIndexBarEventListener: OnIndexBarEventListener?) {
        if (onIndexBarEventListener != null) {
            addOnIndexEventListener(onIndexBarEventListener)
        }
    }

    /**
     * Add a listener that will be notified of any changes in "section index", "pressed", or "released" events for the index bar.
     * @param listener The listener that will be notified.
     * @see OnIndexBarEventListener
     * @see removeOnIndexEventListener
     */
    fun addOnIndexEventListener(listener: OnIndexBarEventListener){
        if (indexEventListeners == null) {
            indexEventListeners = mutableSetOf(listener)
            return
        }
        indexEventListeners?.add(listener)
    }

    /**
     * Unregisters the callback to be invoked when the index has been changed or pressed/released.
     * Multiple listeners can be added and will be called in the order they are added
     * @param listener The listener to remove
     * @see addOnIndexEventListener
     */
    fun removeOnIndexEventListener(listener: OnIndexBarEventListener) {
        indexEventListeners?.forEach {
            if (it === listener) {
                indexEventListeners?.remove(it)
            }
        }
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
        @SuppressLint("UseKtx")
        if ((super.visibility == View.VISIBLE) == show) return

        if (show) {
            alpha = 0f
            animate().apply {
                super.setVisibility(VISIBLE)
                alpha(1f)
                duration = 200
                interpolator = CachedInterpolatorFactory.getOrCreate(SINE_IN_OUT_80)
            }
        } else {
            alpha = 1f
            animate().apply {
                super.setVisibility(VISIBLE)
                alpha(0f)
                duration = 150
                withEndAction { super.setVisibility(GONE) }
            }
        }
    }

    /**
     * Whether the index scrollbar autohides or not.
     * Default is true.
     * @attr ref dev.oneuiproject.oneui.design.R.styleable#AutoHideIndexScrollView_autoHide
     */
    fun setAutoHide(autoHide: Boolean){
        if (this.autoHide == autoHide) return
        setAutoHideInternal(autoHide)
    }

    private fun setAutoHideInternal(autoHide: Boolean) {
        if (isInEditMode) return
        this.autoHide = autoHide
        if (autoHide) {
            recyclerView?.addOnScrollListener(rvScrollListener)
        } else {
            recyclerView?.removeOnScrollListener(rvScrollListener)
        }
        updateVisibility(delayedHide = false)
    }

    @SuppressLint("NewApi")
    private inline fun getIndexes(): Array<String> {
        if (Build.VERSION.SDK_INT < 24 || isInEditMode){
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

    private enum class AppBarState{ COLLAPSED, LITTLE_EXPANDED, SUBSTANTIALLY_EXPANDED }

    private var topMargin = 0
    private var bottomMargin = 0

    override fun setIndexScrollMargin(topMargin: Int, bottomMargin: Int) {
        this.topMargin = topMargin
        this.bottomMargin = bottomMargin
        setIndexScrollMarginInternal(topMargin, bottomMargin)
    }

    private fun setIndexScrollMarginInternal(topMargin: Int, bottomMargin: Int) {
        super.setIndexScrollMargin(topMargin, bottomMargin)
    }

    private inner class AppBarOffsetListener : OnOffsetChangedListener {
        @Volatile
        var appBarState: AppBarState = COLLAPSED

        override fun onOffsetChanged(appBarLayout: AppBarLayout, verticalOffset: Int) {
            val immScrollOffset = if (isImmersive) appBarLayout.seslGetCollapsedHeight().toInt() else 0
            val immScrollRange = appBarLayout.totalScrollRange - immScrollOffset

            val scrollDelta = appBarLayout.totalScrollRange + verticalOffset
            val scrollDeltaBottom = scrollDelta - immScrollOffset

            val newAppBarState = when{
                scrollDeltaBottom > immScrollRange / 3 -> SUBSTANTIALLY_EXPANDED
                scrollDeltaBottom == 0 -> COLLAPSED
                else -> LITTLE_EXPANDED
            }

            if (appBarState == newAppBarState) return
            appBarState = newAppBarState
            updateVisibility(delayedHide = false)
        }
    }

    override fun setVisibility(visibility: Int) {
        lastSetVisibility = visibility
        hideMeJob?.cancel()
        showMeJob?.cancel()
        updateVisibility()
    }

    private var dumbCallbackRegistered = false
    private var downX = 0f
    private var downY = 0f

    @SuppressLint("ClickableViewAccessibility")
    @Suppress("NewApi")
    override fun onTouchEvent(ev: MotionEvent): Boolean {
        when (ev.action){
            MotionEvent.ACTION_DOWN -> {
                downX = ev.x
                downY = ev.y
                onBackInvokedDispatcher?.apply {
                    if (dumbCallbackRegistered || !shouldRegisterDummy(ev.x)) return@apply
                    dumbCallbackRegistered = true
                    registerOnBackInvokedCallback(PRIORITY_DEFAULT, dummyOnBackInvokedCallback!!)
                }
            }
            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> {
                if (dumbCallbackRegistered) {
                    dumbCallbackRegistered = false
                    onBackInvokedDispatcher?.unregisterOnBackInvokedCallback(dummyOnBackInvokedCallback!!)
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (dumbCallbackRegistered && !isIndexBarPressed && abs(ev.y - downY) < abs(downX - ev.x)) {
                    dumbCallbackRegistered = false
                    onBackInvokedDispatcher?.unregisterOnBackInvokedCallback(dummyOnBackInvokedCallback!!)
                }
            }
        }
        return super.onTouchEvent(ev)
    }

    override fun setIndexBarTextMode(textMode: Boolean) {
        super.setIndexBarTextMode(textMode)
        invalidate()
    }

    private fun configureDummyCallback(isAttached: Boolean) {
        if (Build.VERSION.SDK_INT in 33..<35) {
            if (isAttached) {
                if (onBackInvokedDispatcher == null) {
                    onBackInvokedDispatcher = findOnBackInvokedDispatcher()
                }
                if (dummyOnBackInvokedCallback == null) {
                    dummyOnBackInvokedCallback = OnBackInvokedCallback { }
                }
            } else {
                onBackInvokedDispatcher = null
                dummyOnBackInvokedCallback = null
            }
        }
    }

    @RequiresApi(33)
    private fun shouldRegisterDummy(x: Float): Boolean {
        if (Build.VERSION.SDK_INT > 34) return false

        @SuppressLint("PrivateResource")
        val scrollViewWidth = resources.getDimensionPixelSize(androidx.indexscroll.R.dimen.sesl_indexbar_textmode_width)
        val (startX, endX) = if (layoutDirection == LAYOUT_DIRECTION_LTR) {
            val end = width - computeGestureAllowance(true)
            end - scrollViewWidth to end
        } else {
            val start = computeGestureAllowance(false)
            start to start + scrollViewWidth
        }
        return (x.toInt() in startX..endX)
    }

    @RequiresApi(30)
    private fun computeGestureAllowance(isLTR: Boolean): Int {
        val insets = ViewCompat.getRootWindowInsets(this) ?: return 0
        val gestureInsets = insets.getInsets(WindowInsetsCompat.Type.systemGestures())

        val viewRect = Rect()
        if (getGlobalVisibleRect(viewRect)) {
            val bounds =
                (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).currentWindowMetrics.bounds
            return if (isLTR){
                (gestureInsets.right - (bounds.right - viewRect.right)).coerceIn(0, 25)
            }else{
                (gestureInsets.left - viewRect.left).coerceIn(0, 25)
            }
        }
        return 0
    }

    private companion object{
        const val TAG = "AutoHideIndexScrollView"
    }
}