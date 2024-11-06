package dev.oneuiproject.oneui.delegates

import android.view.View
import androidx.core.view.isVisible
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.android.material.appbar.AppBarLayout
import dev.oneuiproject.oneui.ktx.doOnAttachedStateChanged
import java.lang.ref.WeakReference

/**
 * Convenience class for centering "No items" view or the likes shown
 * when the recyclerview adapter is empty.
 *
 * Sample usage:
 * ```
 * class IconsFragment : Fragment(),
 *                       ViewYTranslator by AppBarAwareYTranslator() {
 *
 *    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
 *        // Configure the translation for single view
 *        binding.noItemView.translateYWithAppBar(appBarLayout, viewLifecycleOwner)
 *        // or for multiple views
 *        translateYWithAppBar(setOf(binding.noItemView, binding.progressBar), appBarLayout, viewLifecycleOwner)
 *    }
 *
 * }
 * ```
 */
class AppBarAwareYTranslator: ViewYTranslator, AppBarLayout.OnOffsetChangedListener,
    DefaultLifecycleObserver {

    private lateinit var mAppBarLayout: AppBarLayout
    private var mTranslationViews: MutableSet<View>? = null
    private var lifecycleOwnerWR: WeakReference<LifecycleOwner>? = null

    override fun translateYWithAppBar(
        translatingViews: Set<View>,
        appBarLayout: AppBarLayout,
        lifecycleOwner: LifecycleOwner
    ) {
        mAppBarLayout = appBarLayout
        if (mTranslationViews == null){
            mTranslationViews = mutableSetOf()
        }
        mTranslationViews!!.addAll(translatingViews)


        val currentLifeCycleOwner = lifecycleOwnerWR?.get()
        if (currentLifeCycleOwner == lifecycleOwner) return

        currentLifeCycleOwner?.lifecycle?.removeObserver(this@AppBarAwareYTranslator)
        lifecycleOwnerWR = WeakReference(lifecycleOwner)

        for (v in mTranslationViews!!) {
            v.doOnAttachedStateChanged { _, isAttached ->
                lifecycleOwnerWR?.get()?.lifecycle?.apply {
                    if (isAttached) {
                        addObserver(this@AppBarAwareYTranslator)
                    } else {
                        removeObserver(this@AppBarAwareYTranslator)
                    }
                }
            }
        }
    }

    override fun View.translateYWithAppBar(appBarLayout: AppBarLayout, lifecycleOwner: LifecycleOwner) {
        this@AppBarAwareYTranslator.translateYWithAppBar(
            setOf(this), appBarLayout = appBarLayout, lifecycleOwner = lifecycleOwner)
    }

    override fun onOffsetChanged(appBarLayout: AppBarLayout, verticalOffset: Int) {
        for (v in mTranslationViews!!) {
            if (v.isVisible) {
                v.translationY = (verticalOffset + appBarLayout.totalScrollRange) / -2f
            }
        }
    }

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        mAppBarLayout.addOnOffsetChangedListener(this)
    }

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        mAppBarLayout.removeOnOffsetChangedListener(this)
    }
}

/**
 * Interface to facilitate translating a view's Y position with an AppBarLayout.
 */
interface ViewYTranslator{
    /**
     * Translates the Y position of a view with the given AppBarLayout and LifecycleOwner.
     *
     * @param appBarLayout The AppBarLayout to translate the view with.
     * @param lifecycleOwner The LifecycleOwner that controls the observer lifecycle.
     */
    fun View.translateYWithAppBar(appBarLayout: AppBarLayout, lifecycleOwner: LifecycleOwner)

    fun translateYWithAppBar(translatingViews: Set<View>, appBarLayout: AppBarLayout, lifecycleOwner: LifecycleOwner)
}
