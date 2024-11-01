package dev.oneuiproject.oneui.delegates

import android.content.Context
import android.icu.text.AlphabeticIndex
import android.os.Build
import android.os.LocaleList
import android.widget.SectionIndexer
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatDelegate
import androidx.collection.MutableScatterMap
import androidx.collection.mutableScatterMapOf
import dev.oneuiproject.oneui.ktx.ifEmpty
import java.util.Locale


/**
 * Delegate class for implementing [SectionIndexer] in RecyclerView.Adapter instance
 * that accepts generic type of list items.
 *
 * @param context
 * @param labelExtractor lambda function to be invoked to get the item's label.
 * This should directly return index chars for api level <24.
 */
class SectionIndexerDelegate<T>(private val context: Context,
                                private val labelExtractor: (T) -> CharSequence
) : SemSectionIndexer<T> {

    private val mSectionMap: MutableScatterMap<CharSequence?, Int> = mutableScatterMapOf()
    private var mSections = arrayOf<CharSequence?>()
    private lateinit var mPositionToSectionIndex: IntArray
    private var cachedIndexes: AlphabeticIndex.ImmutableIndex<Int>? = null // Cached value

    override fun updateSections(list: List<T>, useAlphabeticIndex: Boolean) {
        synchronized(mSections){
            mSectionMap.clear()
            val sections = mutableListOf<CharSequence>()
            mPositionToSectionIndex = IntArray(list.size)

            val indexFunction: (Int) -> CharSequence =
                if (!useAlphabeticIndex || Build.VERSION.SDK_INT < 24) {
                    { i -> labelExtractor(list[i]) }
                } else {
                    { i ->
                        val label = labelExtractor(list[i])
                        with(getIndexes()) { getBucket(getBucketIndex(label)).label }
                    }
                }

            for (i in list.indices) {
                val label = indexFunction(i)
                if (!mSectionMap.containsKey(label)) {
                    sections.add(label)
                    mSectionMap[label] = i
                }
                mPositionToSectionIndex[i] = sections.size - 1
            }

            mSections = sections.toTypedArray()
        }
        mSectionMap.trim()
    }


    @RequiresApi(Build.VERSION_CODES.N)
    private fun getIndexes():  AlphabeticIndex.ImmutableIndex<Int> {
        if (cachedIndexes == null) {
            val locales = context.resources.configuration.locales.ifEmpty {
                    AppCompatDelegate.getApplicationLocales().unwrap() as LocaleList
            }
            val alphabeticIndex = AlphabeticIndex<Int>(locales[0])
            for (i in 1 until locales.size()) {
                alphabeticIndex.addLabels(locales[i])
            }
            alphabeticIndex.addLabels(Locale.ENGLISH)

            cachedIndexes = alphabeticIndex.buildImmutableIndex()
        }
        return cachedIndexes!!
    }

    override fun getPositionForSection(sectionIndex: Int): Int {
        return if (sectionIndex >= mSections.size) {
            0
        } else mSectionMap[mSections[sectionIndex]]!!
    }

    override fun getSectionForPosition(position: Int): Int {
        return if (position >= mPositionToSectionIndex.size) {
            0
        } else mPositionToSectionIndex[position]
    }

    override fun getSections(): Array<CharSequence?> {
        synchronized(mSections){
            return mSections
        }
    }
}

interface SemSectionIndexer<T>: SectionIndexer{
    /**
     * Updates the section indexer with a new or modified list of data items. This method should
     * be called every time the data set is updated, and it should be invoked before submitting
     * the new data to the RecyclerView adapter or an AsyncListDiffer.
     *
     * @param list The updated list of data items for the adapter.
     * @param useAlphabeticIndex Whether to generate index characters automatically using
     * [AlphabeticIndex]. This is applicable only for API level 24 and above. Defaults to `true`
     * for API level >= 24, and `false` for lower API levels.
     */
    fun updateSections(list: List<T>, useAlphabeticIndex: Boolean = true)
}