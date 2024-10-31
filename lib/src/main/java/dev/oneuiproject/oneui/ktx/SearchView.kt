package dev.oneuiproject.oneui.ktx

import android.app.Activity
import android.app.SearchManager
import android.content.Context
import androidx.appcompat.widget.SearchView

/**
 * Sets the searchable information for this [SearchView] from the provided [Activity].
 *
 * This method retrieves the [SearchableInfo][android.app.SearchableInfo] associated with the activity and applies it to the [SearchView],
 * enabling the search functionality as defined in the searchable configuration.
 *
 * @param activity The [Activity] from which to obtain the searchable information.
 *
 * @see SearchView.setSearchableInfo
 *
 * Example usage:
 * ```
 * searchView.setSearchableInfoFrom(this)
 * ```
 */
fun SearchView.setSearchableInfoFrom(activity: Activity){
    val searchableInfo = (activity.getSystemService(Context.SEARCH_SERVICE) as SearchManager)
        .getSearchableInfo(activity.componentName)
    setSearchableInfo(searchableInfo)
}