package dev.oneuiproject.oneui.delegates

import android.view.View
import androidx.core.view.isVisible
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.android.material.appbar.AppBarLayout
import dev.oneuiproject.oneui.ktx.doOnAttachedStateChanged
import java.lang.ref.WeakReference

/**
 * An implementation of [ViewYTranslator] designed to center a "No items" view or similar views
 * when the RecyclerView adapter is empty. This class translates the Y position
 * of the provided view(s) in the opposite scroll direction of the `AppBarLayout`
 * with a sensitivity of 50%.

 * This behavior ensures that the view remains visible and centered even as the
 * `AppBarLayout` is scrolled.
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
class AppBarAwareYTranslator: ViewYTranslator, DefaultLifecycleObserver {

    private lateinit var appBarLayout: AppBarLayout
    private var translationViews: MutableSet<View>? = null
    private var lifecycleOwnerWR: WeakReference<LifecycleOwner>? = null

    override fun translateYWithAppBar(
        translatingViews: Set<View>,
        appBarLayout: AppBarLayout,
        lifecycleOwner: LifecycleOwner
    ) {
        this@AppBarAwareYTranslator.appBarLayout = appBarLayout
        if (translationViews == null){
            translationViews = mutableSetOf()
        }
        translationViews!!.addAll(translatingViews)


        val currentLifeCycleOwner = lifecycleOwnerWR?.get()
        if (currentLifeCycleOwner == lifecycleOwner) return

        currentLifeCycleOwner?.lifecycle?.removeObserver(this@AppBarAwareYTranslator)
        lifecycleOwnerWR = WeakReference(lifecycleOwner)

        for (v in translationViews!!) {
            if (v.isAttachedToWindow) {
                lifecycleOwner.lifecycle.addObserver(this@AppBarAwareYTranslator)
            }
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
        for (v in translationViews!!) {
            if (v.isVisible) {
                v.translationY = (verticalOffset + appBarLayout.totalScrollRange) * SENSITIVITY * -1f
            }
        }
    }

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        appBarLayout.addOnOffsetChangedListener(this)
    }

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        appBarLayout.removeOnOffsetChangedListener(this)
    }

    companion object{
        private const val SENSITIVITY = 0.5f
    }
}

/**
 * Interface to facilitate translating a view's Y position with an AppBarLayout.
 */
interface ViewYTranslator: AppBarLayout.OnOffsetChangedListener{
    /**
     * Translates the Y position of a view with the given AppBarLayout and LifecycleOwner.
     *
     * @param appBarLayout The AppBarLayout to translate the view with.
     * @param lifecycleOwner The LifecycleOwner that controls the observer lifecycle.
     */
    fun View.translateYWithAppBar(appBarLayout: AppBarLayout, lifecycleOwner: LifecycleOwner)

    fun translateYWithAppBar(translatingViews: Set<View>, appBarLayout: AppBarLayout, lifecycleOwner: LifecycleOwner)
}
