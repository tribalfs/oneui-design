package dev.oneuiproject.oneui.recyclerview.util

import android.graphics.Rect
import android.util.Log
import androidx.collection.mutableScatterSetOf
import androidx.core.view.WindowInsetsCompat.Type.navigationBars
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.NO_POSITION
import dev.oneuiproject.oneui.layout.ToolbarLayout
import dev.oneuiproject.oneui.layout.ToolbarLayout.AllSelectorState
import dev.oneuiproject.oneui.layout.internal.util.ToolbarLayoutUtils.getLayoutLocationInfo
import dev.oneuiproject.oneui.utils.internal.CachedInterpolatorFactory
import dev.oneuiproject.oneui.utils.internal.CachedInterpolatorFactory.Type
import dev.oneuiproject.oneui.recyclerview.ktx.doOnBlockMultiSelection
import dev.oneuiproject.oneui.recyclerview.ktx.doOnLongPressMultiSelection
import dev.oneuiproject.oneui.recyclerview.model.AdapterItem

/**
 * Delegate for multi-selection for use with the [Adapter].
 *
 * ## Example usage:
 *```
 * class IconsAdapter (
 *    onAllSelectorStateChanged: ((AllSelectorState) -> Unit),
 *    onBlockActionMode: (() -> Unit),
 * ) : RecyclerView.Adapter<IconsAdapter.ViewHolder>(),
 *     MultiSelector<Long/*stable id*/> by MultiSelectorDelegate(
 *       onAllSelectorStateChanged = onAllSelectorStateChanged,
 *       onBlockActionMode = onBlockActionMode,
 *       isSelectable = { rv, pos ->
 *          (rv.adapter as MyAdapter).getItem(pos) is SelectableItem
 *       },
 *       // We're using stable Ids
 *       selectionId = null,
 *       selectionChangePayload = Payload.SELECTION_MODE
 *    ) {
 *
 *   init {
 *       // We're using stable Ids
 *       setHasStableIds(true)
 *   }
 *
 *   override fun getItemId(position: Int): Long {
 *       // Implement when using stable Ids
 *       return currentList[position].id.toLong()
 *   }
 *
 *   fun submitList(list: List<Icon>) {
 *      asyncListDiffer.submitList(list)
 *      // submit selectable ids to the delegate everytime a
 *      // new list is submitted to the adapter.
 *      // Must be the same ids return in getItemId
 *      // when setHasStableIds is true
 *      updateSelectableIds( list.map {it.id.toLong()})
 *   }
 *
 *   // rest of the adapter's implementations
 * }
 *
 * class IconsFragment : Fragment(){
 *
 *    private lateinit var iconsAdapter: IconsAdapter
 *
 *    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
 *
 *       iconsAdapter = IconsAdapter(
 *          onAllSelectorStateChanged = { toolbarLayout.updateAllSelector(it) },
 *          onBlockActionMode = { toolbarLayout.startActionMode(...))
 *
 *       // configure the selection delegate with the recyclerview
 *       iconsAdapter.configureWith(binding.recyclerView)
 *    }
 *
 *   // rest of the fragment's implementations
 * }
 * ```
 * @param SID Type of selection Id to be used. This should be set to `Long`
 * when using [Adapter.getItemId]. See [configureWith].
 *
 * @param onAllSelectorStateChanged A function to be invoked to [AllSelectorState] changes.
 *
 * @param onBlockActionMode A function to be invoked if action mode is activated internally by
 * block selection using the S-Pen or mouse input. Use this callback to synchronize the
 * action mode state of [ToolbarLayout].
 *
 * @param isSelectable (Optional) A function to be checked if not all items are selectable
 * that includes the [RecyclerView] and [AdapterItem] params.
 *  - **Important note**: The implementation for this should account for null [item views][AdapterItem.itemView]
 *  and NO_ID [item id][AdapterItem.id] as this function may be invoked for items which are off-screen
 *  and not bounded to a view/viewholder. This is normally the case during [block selection][doOnBlockMultiSelection].
 *
 * @param getSelectionId (Optional) A function to get the item's selection id.
 * If not provided, the adapter's [stable ids][Adapter.setHasStableIds] will be used and the [SID]  must be `Long`.
 *
 * @param selectionChangePayload (Optional) Change payload for more efficient binding on changes of selected items.
 *
 */
class MultiSelectorDelegate<SID>(
    private val onAllSelectorStateChanged: (state: AllSelectorState) -> Unit,
    private val onBlockActionMode: (() -> Unit)?,
    private val isSelectable: (RecyclerView, AdapterItem) -> Boolean = { _, _ -> true },
    private val getSelectionId: ((RecyclerView, AdapterItem) -> SID)? = null,
    private val selectionChangePayload: Any? = null,
) : MultiSelector<SID> {

    // ScatterSet ftw => materially low O(1) constant for lookup and add/remove, no per-entry alloc
    private val selectedIds = mutableScatterSetOf<SID>()
    private val mutableSelectableIds = mutableScatterSetOf<SID>()
    private val currentListSet: Set<SID> = mutableSelectableIds.asSet()
    private lateinit var adapter: Adapter<*>
    private var bottomSelected = NO_POSITION
    private var currentAllSelectionState = AllSelectorState()
    private var restoreItemAnimator: RecyclerView.ItemAnimator? = null
    private var isLongPressMultiSelectionActive = false

    override var isActionMode: Boolean = false
        private set

    @Throws(IllegalStateException::class)
    override fun configureWith(
        recyclerView: RecyclerView
    ) {
        val adapter = recyclerView.adapter
            ?: throw IllegalStateException("RecyclerView must have an attached adapter")

        if (getSelectionId == null && !adapter.hasStableIds()) {
            throw IllegalStateException(
                "Either `selectionId` parameter must be set or" +
                        " the adapter must implement stable Ids."
            )
        }
        if (adapter !is MultiSelector<*>) {
            throw IllegalStateException("Adapter must extend MultiSelector")
        }

        this.adapter = adapter

        recyclerView.apply {
            doOnLongPressMultiSelection(
                onItemSelected = { item ->
                    @Suppress("UNCHECKED_CAST")
                    toggleItem(getSelectionId?.invoke(this, item) ?: item.id as SID, item.position)
                },
                onStarted = {
                    isLongPressMultiSelectionActive = true

                    //Temporarily disable item animator if any
                    itemAnimator?.let {
                        restoreItemAnimator = it
                        itemAnimator = null
                    }
                },
                onEnded = {
                    isLongPressMultiSelectionActive = false
                    selectedIds.trim()
                    scrollToBottomSelected()
                    restoreItemAnimator?.let {
                        itemAnimator = it
                        restoreItemAnimator = null
                    }
                }
            )

            if (onBlockActionMode != null) {
                doOnBlockMultiSelection(
                    onStarted = {},
                    onCompleted = { s ->
                        if (s.isEmpty()) return@doOnBlockMultiSelection

                        val blockSelectedIds = s.map {
                            @Suppress("UNCHECKED_CAST")
                            getSelectionId?.invoke(recyclerView, it)
                                ?: adapter.getItemId(it.position) as SID
                        }.toSet()

                        if (isActionMode) {
                            blockSelectedIds.forEach { sid ->
                                if (!selectedIds.remove(sid)) {
                                    selectedIds.add(sid)
                                }
                            }
                            adapter.notifyItemRangeChanged(
                                0,
                                adapter.itemCount,
                                selectionChangePayload
                            )
                        } else {
                            toggleActionMode(true, blockSelectedIds)
                            onBlockActionMode()
                        }

                        bottomSelected = s.maxOf { it.position }

                        updateAllSelectorState()
                        scrollToBottomSelected()
                    },
                    isSelectable = isSelectable
                )
            }
        }
    }

    override fun updateSelectableIds(selectableIds: List<SID>) {
        mutableSelectableIds.clear()
        mutableSelectableIds.addAll(selectableIds)
        if (isActionMode) {
            updateAllSelectorState()
        }
    }

    override fun toggleActionMode(isActionMode: Boolean, initialSelectedIds: Set<SID>?) {
        if (this.isActionMode == isActionMode) return
        this.isActionMode = isActionMode
        if (isActionMode) {
            initialSelectedIds?.let {
                selectedIds.addAll(it)
            }
            updateAllSelectorState()
        } else {
            selectedIds.clear()
        }
        adapter.notifyItemRangeChanged(0, adapter.itemCount, selectionChangePayload)
    }

    override fun getSelectedIds(): Set<SID> = selectedIds.asSet()

    override fun toggleItem(selectionId: SID, position: Int?) {
        val isAdded = !(selectedIds.remove(selectionId) || !selectedIds.add(selectionId))

        if (position != null && isLongPressMultiSelectionActive) {
            when {
                isAdded && bottomSelected < position ->
                    bottomSelected = position

                !isAdded && bottomSelected >= position ->
                    bottomSelected = (position - 1).coerceAtLeast(0)
            }
        }

        if (position == null) {
            adapter.notifyItemRangeChanged(0, adapter.itemCount, selectionChangePayload)
        } else {
            adapter.notifyItemChanged(position, selectionChangePayload)
        }
        updateAllSelectorState()
    }

    override fun isSelected(selectionId: SID): Boolean {
        return selectedIds.contains(selectionId)
    }

    override fun onToggleSelectAll(isSelectAll: Boolean) {
        if (isSelectAll) {
            selectedIds.addAll(currentListSet)
        } else {
            selectedIds.clear()
        }
        adapter.notifyItemRangeChanged(0, adapter.itemCount, selectionChangePayload)
        updateAllSelectorState()
    }


    private fun updateAllSelectorState() {
        getActionModeAllSelectorState().let {
            if (it != currentAllSelectionState) {
                currentAllSelectionState = it
                onAllSelectorStateChanged.invoke(it)
            }
        }
    }

    private fun getActionModeAllSelectorState(): ToolbarLayout.AllSelectorState {
        if (currentListSet.isEmpty()) {
            Log.w(
                TAG, "Current list of selectable ids is empty. " +
                        "Ensure that `updateSelectableIds()` is called on " +
                        "each update to the adapter data."
            )
        }

        val currentDataSetCount = currentListSet.size
        val isEnabled = currentDataSetCount > 0
        val selectedIds = selectedIds
        val allSelected = when {
            !isEnabled -> null
            selectedIds.size < currentDataSetCount -> false
            else -> currentListSet.none { id -> !selectedIds.contains(id) }
        }
        return AllSelectorState(
            selectedIds.size,
            isChecked = allSelected,
            isEnabled = isEnabled
        )
    }

    private fun RecyclerView.scrollToBottomSelected() {
        if (bottomSelected == NO_POSITION) return
        findViewHolderForAdapterPosition(bottomSelected)?.let { viewHolder ->
            val itemBottom = viewHolder.itemView.run { y + height }
            postOnAnimationDelayed({
                val rvBottom = Rect().apply { getLocalVisibleRect(this) }.bottom
                val bottomOffset = calculateBottomOffset()
                val scrollDistance = (itemBottom - (rvBottom - bottomOffset)).toInt()
                if (scrollDistance > 0) {
                    val restoreNestScrolling = isNestedScrollingEnabled
                    isNestedScrollingEnabled = false
                    smoothScrollBy(
                        0,
                        scrollDistance,
                        CachedInterpolatorFactory.getOrCreate(Type.SINE_IN_OUT_60),
                        300
                    )
                    if (restoreNestScrolling) {
                        postDelayed({ isNestedScrollingEnabled = true }, 350)
                    }
                }
            }, 700)
        }
        bottomSelected = NO_POSITION
    }

    private fun RecyclerView.calculateBottomOffset(): Int {
        val locInfo = getLayoutLocationInfo()
        return if (locInfo.isInsideTBLMainContainer) {
            locInfo.tblParent?.let {
                if (it.isImmersiveScroll) {
                    Rect().apply { it.footerParent.getLocalVisibleRect(this) }.let { rect ->
                        (rect.bottom - rect.top) + rootWindowInsets.getInsets(navigationBars()).bottom
                    }
                } else 0
            } ?: 0
        } else 0
    }

    private companion object {
        const val TAG = "MultiSelectorDelegate"
    }
}

/**
 * Interface for implementing multi-selection in a [RecyclerView.Adapter].
 *
 * This interface provides methods for configuring multi-selection, managing the selection state,
 * and interacting with the selection mode.
 *
 * @param SID The type of the selection ID. This should typically be a stable identifier for your items,
 *          such as `Long` when using [RecyclerView.Adapter.getItemId].
 */
interface MultiSelector<SID> {
    /**
     * Configure the multi selector.
     *
     * @param recyclerView The `RecyclerView` to configure. A [RecyclerView.Adapter] must already be attached.
     */
    fun configureWith(recyclerView: RecyclerView)

    /**
     * Set the current list of selectable Ids for items submitted to the adapter.
     * Call this everytime a new list is submitted to the adapter.
     */
    fun updateSelectableIds(selectableIds: List<SID>)

    /**
     * Use to notify the adapter of the action/selection mode state.
     * This will trigger the adapter to rebind its views, using the `selectionPayload`, if provided.
     *
     * This also updates this delegate's `isActionMode` property value.
     *
     * Typically, this should be invoked on [ToolbarLayout.startActionMode][dev.oneuiproject.oneui.layout.ToolbarLayout.startActionMode] and
     * [ToolbarLayout.endActionMode][dev.oneuiproject.oneui.layout.ToolbarLayout.endActionMode].
     *
     * @param isActionMode Whether action mode is turned on `true` or not `false`.
     * @param initialSelectedIds (Optional) Array of selection ids of initially selected items if [isActionMode] is true
     */
    fun toggleActionMode(isActionMode: Boolean, initialSelectedIds: Set<SID>? = null)

    /**
     * Invoke this to toggle an item's selected state - selects if unselected, unselects if selected.
     *
     * @param selectionId Selection Id of the item selected/unselected
     * @param position (Optional) Adapter position of the item selected/unselected.
     * Provide when possible for better efficiency.
     */
    fun toggleItem(selectionId: SID, position: Int? = null)

    /**
     * Invoke this to toggle all items' selected state - selects if unselected, unselects if selected.
     *
     * @param isSelectAll Whether to select all items `true` or not `false`.
     */
    fun onToggleSelectAll(isSelectAll: Boolean)

    /**
     * Check if an item is selected.
     *
     * @param selectionId Selection Id of the item to check
     */
    fun isSelected(selectionId: SID): Boolean

    /** Get the current list of selected Ids. */
    fun getSelectedIds(): Set<SID>

    /** This property is set by [toggleActionMode] */
    val isActionMode: Boolean
}



