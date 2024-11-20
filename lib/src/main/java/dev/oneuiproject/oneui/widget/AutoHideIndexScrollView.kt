@file:Suppress("unused", "NOTHING_TO_INLINE")

package dev.oneuiproject.oneui.widget


import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.database.MatrixCursor
import android.icu.text.AlphabeticIndex
import android.os.LocaleList
import android.util.AttributeSet
import androidx.appcompat.animation.SeslAnimationUtils
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.res.use
import androidx.core.view.isGone
import androidx.core.view.isVisible
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
import dev.oneuiproject.oneui.ktx.ifEmpty
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
    private var mFab: ScrollAwareFloatingActionButton? = null
    private var mAppBarLayout: AppBarLayout? = null
    private var mHideWhenExpandedListener: AppBarOffsetListener? = null
    private var mAutoHide: Boolean = true

    private var mSetVisibility: Int? = null

    init{
        mSetVisibility = visibility
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
            if (newState == SCROLL_STATE_IDLE) {
                mIsRVScrolling = false
                if (!mIsIndexBarPressed && mAutoHide) hideMeShowFabDelayed()
            } else {
                mIsRVScrolling = true
                showMeAndHideFabDelayed()
            }
        }
    }

    private fun hideMeAndShowFab(){
        mFab?.apply { if (!mIsRVScrolling && !mIsIndexBarPressed) show() }
        animateVisibility( false)
    }

    private fun showMeAndHideFab(){
        mFab?.hide()
        if (mHideWhenExpandedListener?.isAppBarExpanding == true ||  mSetVisibility != VISIBLE) return
        animateVisibility(true)
    }

    private val hideMeAndShowFabRunnable = Runnable {
        hideMeAndShowFab()
    }

    private val showMeAndHideFabRunnable = Runnable {
        showMeAndHideFab()
    }

    private fun hideMeShowFabDelayed(){
        removeCallbacks(showMeAndHideFabRunnable)
        removeCallbacks(hideMeAndShowFabRunnable)
        postDelayed(hideMeAndShowFabRunnable, 1200)
    }

    private fun showMeAndHideFabDelayed(){
        removeCallbacks(hideMeAndShowFabRunnable)
        removeCallbacks(showMeAndHideFabRunnable)
        postDelayed(showMeAndHideFabRunnable, 200)
    }


    fun attachScrollAwareFAB(fab: ScrollAwareFloatingActionButton){
        this.mFab = fab
        if (!mIsIndexBarPressed && !mIsRVScrolling){
            fab.show()
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
            mAppBarLayout = rootView.findViewById<AppBarLayout?>(R.id.toolbarlayout_app_bar)?.apply {
                addOnOffsetChangedListener(
                    AppBarOffsetListener().also {
                        mHideWhenExpandedListener = it
                    }
                )
            }

            setIndexBarGravityInt(resources.configuration.layoutDirection)

            addOnScrollListener(rvScrollListener)

            val layoutManager = layoutManager as LinearLayoutManager

            setOnIndexBarEventListener(
                object : OnIndexBarEventListener {
                    override fun onIndexChanged(sectionIndex: Int) {
                        if (mRecyclerView!!.scrollState != SCROLL_STATE_IDLE) {
                            stopScroll()
                        }
                        layoutManager.scrollToPositionWithOffset(sectionIndex,0)
                    }

                    override fun onPressed(v: Float) {
                        mIsIndexBarPressed = true
                        if (mAutoHide) removeCallbacks(hideMeAndShowFabRunnable)
                        mFab?.hide()
                    }

                    override fun onReleased(v: Float) {
                        mIsIndexBarPressed = false
                        if (mAutoHide && mRecyclerView!!.scrollState == SCROLL_STATE_IDLE) {
                            hideMeShowFabDelayed()
                        }
                    }
                }
            )
            if (mAutoHide) {
                if (mIsRVScrolling && mHideWhenExpandedListener?.isAppBarCollapsed != false){
                    showMeAndHideFabDelayed()
                } else {
                    removeCallbacks(showMeAndHideFabRunnable)
                    removeCallbacks(hideMeAndShowFabRunnable)
                    this@AutoHideIndexScrollView.isGone = true
                    mFab?.show()
                }
            }else{
                removeCallbacks(showMeAndHideFabRunnable)
                removeCallbacks(hideMeAndShowFabRunnable)
                this@AutoHideIndexScrollView.isVisible = true
                mFab?.show()
            }
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        if (mAutoHide) {
            removeCallbacks(hideMeAndShowFabRunnable)
            setOnIndexBarEventListener(null)
            mRecyclerView?.removeOnScrollListener(rvScrollListener)
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
        animate().apply {
            cancel()
            if (show) {
                alpha(1f)
                duration = 350
                interpolator = SeslAnimationUtils.SINE_IN_OUT_80
                doOnStart {
                    alpha = 0f
                    super.setVisibility(VISIBLE)
                }
            } else {
                alpha(0f)
                duration = 300
                doOnEnd { super.setVisibility(GONE) }
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
            if (mIsIndexBarPressed || mIsRVScrolling) {
                removeCallbacks(hideMeAndShowFabRunnable)
                removeCallbacks(showMeAndHideFabRunnable)
                showMeAndHideFab()
            }else{
                removeCallbacks(hideMeAndShowFabRunnable)
                removeCallbacks(showMeAndHideFabRunnable)
                hideMeAndShowFab()
            }
        } else {
            mRecyclerView?.removeOnScrollListener(rvScrollListener)
            removeCallbacks(hideMeAndShowFabRunnable)
            removeCallbacks(showMeAndHideFabRunnable)
            animateVisibility(true)
        }
    }

    @SuppressLint("NewApi")
    private inline fun getIndexes():  Array<String> {
        if (isInEditMode){
            return "A,B,C,D,E,F,G,H,I,J,K,L,M,N,O,P,Q,R,S,T,U,V,W,X,Y,Z".split(",").toTypedArray()
        }

        val locales = resources.configuration.locales.ifEmpty {
            AppCompatDelegate.getApplicationLocales().unwrap() as LocaleList
        }
        val alphabeticIndex = AlphabeticIndex<Int>(locales[0])
        for (i in 1 until locales.size()) {
            alphabeticIndex.addLabels(locales[i])
        }
        alphabeticIndex.addLabels(Locale.ENGLISH)
        return alphabeticIndex.buildImmutableIndex().toSet().map { it.toString() }.toTypedArray()
    }

    private inner class AppBarOffsetListener : OnOffsetChangedListener {
        @Volatile
        var isAppBarExpanding: Boolean = false
        @Volatile
        var isAppBarCollapsed: Boolean = true

        override fun onOffsetChanged(layout: AppBarLayout, verticalOffset: Int) {
            val totalScrollRange = layout.totalScrollRange

            isAppBarExpanding = (totalScrollRange + verticalOffset) > totalScrollRange / 3
            isAppBarCollapsed =  totalScrollRange + verticalOffset == 0

            if (isAppBarExpanding) {
                removeCallbacks(hideMeAndShowFabRunnable)
                mFab?.show()
                if (this@AutoHideIndexScrollView.isVisible && !mIsIndexBarPressed) {
                    animateVisibility( false)
                }
            }else if (isAppBarCollapsed){
                if (mAutoHide) {
                    if (mIsRVScrolling) {
                        postDelayed(showMeAndHideFabRunnable, 400)
                    }
                }else if (!this@AutoHideIndexScrollView.isVisible){
                    animateVisibility(true)
                }
            }
        }
    }

    override fun setVisibility(visibility: Int) {
        super.setVisibility(visibility)
        mSetVisibility = visibility
    }
}