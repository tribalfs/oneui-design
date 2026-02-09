package dev.oneuiproject.oneui.recyclerview.adapter

import androidx.annotation.CallSuper
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import dev.oneuiproject.oneui.layout.ToolbarLayout
import dev.oneuiproject.oneui.layout.ToolbarLayout.AllSelectorState
import dev.oneuiproject.oneui.recyclerview.ktx.doOnBlockMultiSelection
import dev.oneuiproject.oneui.recyclerview.model.AdapterItem
import dev.oneuiproject.oneui.recyclerview.util.MultiSelector
import dev.oneuiproject.oneui.recyclerview.util.MultiSelectorDelegate
import dev.oneuiproject.oneui.recyclerview.util.SectionIndexerDelegate
import dev.oneuiproject.oneui.recyclerview.util.SemSectionIndexer

/**
 * An abstract [ListAdapter] that integrates **section indexing** and **multi-selection**
 * using function-based providers.
 *
 * This adapter provides support for the following `RecyclerView` features:
 *
 * * Index-based fast scrolling. See [RecyclerView.seslSetFastScrollerEnabled].
 * * Showing index tip labels. See [RecyclerView.seslSetIndexTipEnabled].
 * * Multi-selection. See [RecyclerView.seslStartLongPressMultiSelection].
 *
 * ---
 *
 * ### Features
 *
 * #### 1. Multi-Selection
 * Selection state is managed via a [MultiSelectorDelegate].
 *
 * #### 2. Section Indexing
 * The adapter implements [SemSectionIndexer] by delegation to [SectionIndexerDelegate].
 * Section labels are resolved lazily for each item using [indexLabelExtractor], enabling
 * fast scroller navigation without coupling the adapter to a specific data model.
 *
 * ---
 * @param T  Type of the Lists this Adapter will receive
 *
 * @param VH A class that extends [RecyclerView.ViewHolder] that will be used by this adapter.
 *
 * @param SID  The type of the selectable identifier (e.g. `Long`, `String`).
 *
 * @param indexLabelExtractor A function that returns the section index label
 * for a given [item][T]. This value is used by the [fast scroller][RecyclerView.seslSetFastScrollerEnabled]
 * and the [index tip][RecyclerView.seslSetIndexTipEnabled].
 *
 * @param onAllSelectorStateChanged A function to be invoked to [AllSelectorState] changes.
 *
 * @param onBlockActionMode A function to be invoked if action mode is activated internally by
 * block selection using the S-Pen or mouse input. Use this callback to synchronize the
 * action mode state of [ToolbarLayout].
 *
 * @param selectableIdsProvider A function that extracts the list of selectable
 * identifiers from the submitted list. This is invoked every time [submitList] is called.
 *
 * @param selectionIdProvider (Optional) A function to get the item's selection id.
 * If not provided, the adapter's [stable ids][setHasStableIds] will be used and the [SID]  must be `Long`.
 *
 * @param isSelectable (Optional) A function to be checked if not all items are selectable
 * that includes the [RecyclerView] and [AdapterItem] params.
 *  - **Important note**: The implementation for this should account for null [item views][AdapterItem.itemView]
 *  and NO_ID [item id][AdapterItem.id] as this function may be invoked for items which are off-screen
 *  and not bounded to a view/viewholder. This is normally the case during [block selection][doOnBlockMultiSelection].
 *
 * @param selectionChangePayload (Optional) Change payload for more efficient updating of selected items.
 *
 * @param diffCallback A [DiffUtil.ItemCallback] used to compute list differences.
 */
abstract class IndexedSelectableListAdapter<T, VH : RecyclerView.ViewHolder, SID>
    @JvmOverloads constructor(
        indexLabelExtractor: (T) -> CharSequence,
        private val onAllSelectorStateChanged: ((AllSelectorState) -> Unit),
        private val onBlockActionMode: (() -> Unit)? = null,
        private val selectableIdsProvider: (currentList: List<T>) -> List<SID>,
        selectionIdProvider: ((rv: RecyclerView, item: AdapterItem) -> SID)? = null,
        isSelectable: ((rv: RecyclerView, item: AdapterItem) -> Boolean)? = null,
        selectionChangePayload: Any? = null,
        diffCallback: DiffUtil.ItemCallback<T>
) : ListAdapter<T, VH>(diffCallback),
    MultiSelector<SID> by MultiSelectorDelegate(
        onAllSelectorStateChanged = onAllSelectorStateChanged,
        onBlockActionMode = onBlockActionMode,
        isSelectable = isSelectable ?: { _, _ -> true },
        getSelectionId = selectionIdProvider,
        selectionChangePayload = selectionChangePayload
    ),
    SemSectionIndexer<T> by SectionIndexerDelegate(
        labelExtractor = { item -> indexLabelExtractor(item) }) {

    @CallSuper
    override fun submitList(list: List<T>?, commitCallback: Runnable?) {
        super.submitList(list, commitCallback)
        updateSections(list ?: emptyList(), false)
        updateSelectableIds(list?.let { selectableIdsProvider(it) } ?: emptyList())
    }

    @CallSuper
    override fun submitList(list: List<T>?) {
        super.submitList(list)
        updateSections(list ?: emptyList(), false)
        updateSelectableIds(list?.let { selectableIdsProvider(it) } ?: emptyList())
    }
}