package dev.oneuiproject.oneui.recyclerview.util

import android.icu.text.AlphabeticIndex
import android.os.Build
import android.widget.SectionIndexer
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatDelegate
import androidx.collection.MutableScatterMap
import androidx.collection.mutableScatterMapOf
import java.util.Locale


/**
 * Delegate class for implementing [SemSectionIndexer] in RecyclerView.Adapter instance
 * that accepts generic type of list items.
 *
 * ## Example usage:
 *```
 * class IconsAdapter (
 *    private val context: Context
 * ) : RecyclerView.Adapter<IconsAdapter.ViewHolder>(),
 *     SemSectionIndexer<Int> by SectionIndexerDelegate(
 *           context,
 *           labelExtractor = {iconId -> getLabel(mContext, iconId)}){
 *
 *
 *   fun submitList(list: List<Icon>) {
 *       asyncListDiffer.submitList(list)
 *       //submit the same list to the delegate everytime a
 *       //new list is submitted to the adapter.
 *       updateSections(list, false)
 *   }
 *
 *   //rest of the adapter's implementations
 * }
 * ```
 * @param labelExtractor lambda function to be invoked to get the item's label.
 * This should directly return index chars for api level <24.
 */
class SectionIndexerDelegate<T>(
    private val labelExtractor: (T) -> CharSequence
) : SemSectionIndexer<T> {

    private val sectionMap: MutableScatterMap<String, Int> = mutableScatterMapOf()
    private var _sections: Array<String> = emptyArray()
    private var positionToSectionIndex: IntArray = IntArray(0)
    private var cachedAlphabeticIndex: AlphabeticIndex.ImmutableIndex<Int>? = null

    private companion object {
        const val TAG = "SectionIndexerDelegate"
    }

    override fun updateSections(list: List<T>, useAlphabeticIndex: Boolean) {
        val localSectionMap = mutableScatterMapOf<String, Int>()
        val localSectionsList = mutableListOf<String>()
        val localPositionToSectionIndex = IntArray(list.size)

        val indexFunction: (Int) -> CharSequence =
            if (!useAlphabeticIndex || Build.VERSION.SDK_INT < 24) {
                { i -> labelExtractor(list[i]) }
            } else {
                { i ->
                    val label = labelExtractor(list[i])
                    with(getAlphabeticIndex()) { getBucket(getBucketIndex(label)).label }
                }
            }

        for (i in list.indices) {
            val currentLabel = indexFunction(i).toString()
            if (!localSectionMap.containsKey(currentLabel)) {
                localSectionsList.add(currentLabel)
                localSectionMap[currentLabel] = i
            }
            localPositionToSectionIndex[i] = localSectionsList.size - 1
        }

        synchronized(this) {
            sectionMap.clear()
            sectionMap.putAll(localSectionMap)
            _sections = localSectionsList.toTypedArray()
            positionToSectionIndex = localPositionToSectionIndex
        }
        sectionMap.trim()
    }


    @RequiresApi(Build.VERSION_CODES.N)
    private fun getAlphabeticIndex(): AlphabeticIndex.ImmutableIndex<Int> {
        cachedAlphabeticIndex?.let { return it }
        synchronized(this) {
            cachedAlphabeticIndex?.let { return it }
            val locales = AppCompatDelegate.getApplicationLocales()
            val primaryLocale = if (!locales.isEmpty) locales[0] else Locale.getDefault()
            val alphabeticIndexBuilder = AlphabeticIndex<Int>(primaryLocale)

            for (i in 0 until locales.size()) {
                locales[i]?.let { alphabeticIndexBuilder.addLabels(it) }
            }
            alphabeticIndexBuilder.addLabels(Locale.ENGLISH)
            val newIndex = alphabeticIndexBuilder.buildImmutableIndex()
            cachedAlphabeticIndex = newIndex
            return newIndex
        }
    }

    override fun getPositionForSection(sectionIndex: Int): Int {
        val currentSections = synchronized(this) { _sections }
        return if (sectionIndex < 0 || sectionIndex >= currentSections.size) {
            if (positionToSectionIndex.isNotEmpty()) positionToSectionIndex.size - 1 else 0
        } else {
            sectionMap[currentSections[sectionIndex]] ?: 0
        }
    }

    override fun getSectionForPosition(position: Int): Int {
        val currentPositionToSectionIndex = synchronized(this) { positionToSectionIndex }
        return if (position < 0 || position >= currentPositionToSectionIndex.size) {
            if (currentPositionToSectionIndex.isNotEmpty()) currentPositionToSectionIndex.size - 1 else 0
        } else {
            currentPositionToSectionIndex[position]
        }
    }

    override fun getSections(): Array<String> {
        return synchronized(this) { _sections.copyOf() }
    }
}

/**
 * An interface that extends [SectionIndexer] to provide additional functionality for managing
 * sections in a list, particularly for use with RecyclerView adapters.
 * This is designed to work with generic data types and introduces a method to update the sections dynamically.
 *
 * @param T The type of data items in the list that this indexer will manage.
 */
interface SemSectionIndexer<T> : SectionIndexer {
    /**
     * Updates the section indexer with a new or modified list of data items. This method should
     * be called every time the data set is updated.
     *
     * @param list The updated list of data items for the adapter.
     * @param useAlphabeticIndex Whether to generate index characters automatically using
     * [AlphabeticIndex]. This is applicable only for API level 24 and above. Defaults to `true`
     * for API level >= 24, and `false` for lower API levels.
     */
    fun updateSections(list: List<T>, useAlphabeticIndex: Boolean = true)
}