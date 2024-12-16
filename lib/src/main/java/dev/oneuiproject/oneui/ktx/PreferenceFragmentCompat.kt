@file:Suppress("NOTHING_TO_INLINE")

package dev.oneuiproject.oneui.ktx

import android.util.Log
import androidx.appcompat.util.SeslRoundedCorner.ROUNDED_CORNER_BOTTOM_LEFT
import androidx.appcompat.util.SeslRoundedCorner.ROUNDED_CORNER_BOTTOM_RIGHT
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceGroup
import dev.oneuiproject.oneui.design.R
import dev.oneuiproject.oneui.preference.InsetPreferenceCategory
import dev.oneuiproject.oneui.preference.LayoutPreference
import dev.oneuiproject.oneui.widget.RelativeLink
import dev.oneuiproject.oneui.widget.RelativeLinksCard

/**
 * Create and add a [RelativeLinksCard] to this preference screen.
 *
 * **Important: This should be called in [PreferenceFragmentCompat.onViewCreated] or later.**
 *
 * @param relativeLinks Optional [RelativeLink]s to be added initially to the card.
 * @return the [RelativeLinksCard] created.
 */
fun PreferenceFragmentCompat.addRelativeLinksCard(
    vararg relativeLinks: RelativeLink
): RelativeLinksCard {
    preferenceScreen!!.apply{
        getRelativeLinksCard()?.apply {
            Log.w(this::class.simpleName,
                "addRelativeLinksCard,  RelativeLinksCard already exist. Reusing it instead.")
            clearLinks()
            relativeLinks.forEach { addLink(it.title, it.onClick) }
            return@addRelativeLinksCard this
        }

        assert(listView != null){
            "Preference list is not initialized. Ensure to call `initRelativeLinksCard` in" +
                    " PreferenceFragmentCompat.onViewCreated() or later."
        }
        listView!!.seslSetLastRoundedCorner(false)

        val relativeLinksCard = RelativeLinksCard(requireContext()).apply {
            relativeLinks.forEach { addLink(it.title, it.onClick) }
        }

        val addTopInset = if (preferenceCount - 1 > 0){
            getPreference(preferenceCount - 1) !is InsetPreferenceCategory
        } else true

        if (addTopInset) {
            addPreference(
                InsetPreferenceCategory(context).apply {
                    order = Int.MAX_VALUE - 2
                    seslSetSubheaderRoundedBackground(ROUNDED_CORNER_BOTTOM_LEFT or ROUNDED_CORNER_BOTTOM_RIGHT)
                }
            )
        }

        addPreference(
            LayoutPreference(context, relativeLinksCard).apply {
                setViewId(R.id.relative_links_card_preference)
                order = Int.MAX_VALUE - 1
            }
        )
        return relativeLinksCard
    }
}


fun PreferenceFragmentCompat.getRelativeLinksCard(): RelativeLinksCard? {
    preferenceScreen?.apply {
        if (preferenceCount < 1) return null
        for (i in preferenceCount - 1 downTo 0) {
            when (val pref = getPreference(i)) {
                is LayoutPreference -> {
                    (pref.getView() as? RelativeLinksCard)?.let { return it }
                }
                is PreferenceGroup -> {
                    if (pref.preferenceCount < 1) continue
                    for (j in pref.preferenceCount - 1 downTo 0) {
                        when (val p = pref.getPreference(j)) {
                            is LayoutPreference -> {
                                (p.getView() as? RelativeLinksCard)?.let { return it }
                            }
                        }
                    }
                }
            }
        }
    }
    return null
}

@JvmOverloads
fun PreferenceFragmentCompat.removeRelativeLinksCard(removeTopInset: Boolean = false) {
    fun removeRecursively(group: PreferenceGroup) {
        for (i in group.preferenceCount - 1 downTo 0) {
            val pref = group.getPreference(i)
            when {
                pref is LayoutPreference && pref.getView() is RelativeLinksCard -> {
                    group.removePreference(pref)
                    if (removeTopInset && i > 0) {
                        val previousPref = group.getPreference(i - 1)
                        if (previousPref is InsetPreferenceCategory) {
                            group.removePreference(previousPref)
                        }
                    }
                    return
                }
                pref is PreferenceGroup -> {
                    removeRecursively(pref)
                }
            }
        }
    }
    preferenceScreen?.let { removeRecursively(it) }
    listView!!.seslSetLastRoundedCorner(true)
}
