package dev.oneuiproject.oneui.delegates

import android.view.View
import androidx.core.view.isVisible
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.google.android.material.appbar.AppBarLayout
import java.lang.ref.WeakReference

/**
 * An implementation of [ViewYTranslator] designed to center a "No items" view or similar views
 * to show when the RecyclerView is empty. This class translates the Y position
 * of the provided view(s) in the opposite scroll direction of the `AppBarLayout`
 * with a sensitivity of 50%.

 * This behavior ensures that the view remains visible and centered even as the
 * `AppBarLayout` is scrolled.
 *
 * ## Example usage:
 * ```
 * class IconsFragment : Fragment(),
 *                       ViewYTranslator by AppBarAwareYTranslator() {
 *
 *    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
 *        // Configure the translation for single view
 *        binding.noItemView.translateYWithAppBar(appBarLayout, viewLifecycleOwner)
 *
 *        // or for multiple views
 *        translateYWithAppBar(setOf(binding.noItemView, binding.progressBar), appBarLayout, viewLifecycleOwner)
 *    }
 *
 * }
 * ```
 */
class AppBarAwareYTranslator: ViewYTranslator, AppBarLayout.OnOffsetChangedListener  {

    private var appBarLayout: AppBarLayout? = null
    private val translationViews: MutableSet<View> = mutableSetOf()
    private var lifecycleOwnerWR: WeakReference<LifecycleOwner>? = null
    private var isObserverAddedToLifecycle = false

    private val lifecycleObserver = object: DefaultLifecycleObserver{
        override fun onStart(owner: LifecycleOwner) {
            appBarLayout?.addOnOffsetChangedListener(this@AppBarAwareYTranslator)
        }

        override fun onStop(owner: LifecycleOwner) {
            appBarLayout?.removeOnOffsetChangedListener(this@AppBarAwareYTranslator)
        }

        override fun onDestroy(owner: LifecycleOwner) = clearReferences()
    }

    override fun translateYWithAppBar(
        translatingViews: Set<View>,
        appBarLayout: AppBarLayout,
        lifecycleOwner: LifecycleOwner
    ) {
        // If appBarLayout changes, remove listener from the old one
        if (this.appBarLayout != null && this.appBarLayout != appBarLayout) {
            this.appBarLayout?.removeOnOffsetChangedListener(this)
        }
        this.appBarLayout = appBarLayout
        this.translationViews.addAll(translatingViews)

        val currentLifecycleOwner = lifecycleOwnerWR?.get()
        if (currentLifecycleOwner != lifecycleOwner) {
            // Remove observer from previous lifecycle owner
            currentLifecycleOwner?.lifecycle?.removeObserver(lifecycleObserver)
            isObserverAddedToLifecycle = false

            this.lifecycleOwnerWR = WeakReference(lifecycleOwner)
            // Add observer only once to the new lifecycle owner
            if (!isObserverAddedToLifecycle) {
                lifecycleOwner.lifecycle.addObserver(lifecycleObserver)
                isObserverAddedToLifecycle = true
            }
        } else if (!isObserverAddedToLifecycle && lifecycleOwner.lifecycle.currentState.isAtLeast(
                Lifecycle.State.STARTED)) {
            // If same owner but observer wasn't added (e.g., config change and re-init)
            lifecycleOwner.lifecycle.addObserver(lifecycleObserver)
            isObserverAddedToLifecycle = true
        }
    }

    override fun View.translateYWithAppBar(appBarLayout: AppBarLayout, lifecycleOwner: LifecycleOwner) {
        this@AppBarAwareYTranslator.translateYWithAppBar(
            setOf(this), appBarLayout = appBarLayout, lifecycleOwner = lifecycleOwner)
    }

    override fun onOffsetChanged(appBarLayout: AppBarLayout, verticalOffset: Int) {
        for (v in translationViews) {
            if (v.isVisible) {
                v.translationY = (verticalOffset + appBarLayout.totalScrollRange) * SENSITIVITY * -1f
            }
        }
    }

    private fun clearReferences() {
        appBarLayout?.removeOnOffsetChangedListener(this)
        appBarLayout = null
        translationViews.clear()
        lifecycleOwnerWR?.get()?.lifecycle?.removeObserver(lifecycleObserver)
        lifecycleOwnerWR = null
        isObserverAddedToLifecycle = false
    }

    private companion object{
        private const val SENSITIVITY = 0.5f
    }
}

/**
 * Interface to facilitate translating a view's Y position with an AppBarLayout.
 */
interface ViewYTranslator {
    /**
     * Translates the Y position of a view with the given AppBarLayout and LifecycleOwner.
     *
     * @param appBarLayout The [AppBarLayout] to translate the view with.
     * @param lifecycleOwner The [LifecycleOwner] to be the basis of adding and removing
     * an offset listener to the provided AppBarLayout.
     */
    fun View.translateYWithAppBar(appBarLayout: AppBarLayout, lifecycleOwner: LifecycleOwner)

    fun translateYWithAppBar(translatingViews: Set<View>, appBarLayout: AppBarLayout, lifecycleOwner: LifecycleOwner)
}
