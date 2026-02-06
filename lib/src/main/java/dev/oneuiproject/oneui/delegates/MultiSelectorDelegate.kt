package dev.oneuiproject.oneui.delegates

import android.graphics.Rect
import android.util.Log
import androidx.collection.ScatterSet
import androidx.collection.mutableScatterSetOf
import androidx.core.view.WindowInsetsCompat.Type.navigationBars
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.NO_POSITION
import dev.oneuiproject.oneui.ktx.MultiSelectionState
import dev.oneuiproject.oneui.ktx.MultiSelectionState.ENDED
import dev.oneuiproject.oneui.ktx.MultiSelectionState.STARTED
import dev.oneuiproject.oneui.ktx.doOnLongPressMultiSelection
import dev.oneuiproject.oneui.layout.ToolbarLayout.AllSelectorState
import dev.oneuiproject.oneui.layout.internal.util.ToolbarLayoutUtils.getLayoutLocationInfo
import dev.oneuiproject.oneui.utils.internal.CachedInterpolatorFactory
import dev.oneuiproject.oneui.utils.internal.CachedInterpolatorFactory.Type

@Deprecated("Use AllSelectorState found inside dev.oneuiproject.oneui.layout.ToolbarLayout",
    ReplaceWith("AllSelectorState", "dev.oneuiproject.oneui.layout.ToolbarLayout.AllSelectorState"))
typealias AllSelectorState = AllSelectorState

/**
 * Delegate for multi-selection for use with the [RecyclerView.Adapter].
 *
 * ## Example usage:
 *```
 * class IconsAdapter (
 *    private val context: Context
 * ) : RecyclerView.Adapter<IconsAdapter.ViewHolder>(),
 *     MultiSelector<Long> by MultiSelectorDelegate(
 *                  isSelectable = {viewType -> viewType == 1 }) {
 *
 *   init {
 *       //we're using stable Ids (Long) as the selection ids
 *       setHasStableIds(true)
 *   }
 *
 *   override fun getItemId(position: Int): Long {
 *        //Implement when using stable Ids
 *         return currentList[position].id.toLong()
 *   }
 *
 *   fun submitList(list: List<Icon>) {
 *       asyncListDiffer.submitList(list)
 *       //submit selectable ids to the delegate everytime a new list is submitted to the adapter
 *       //Must be the same ids return in getItemId when setHasStableIds is true
 *       updateSelectableIds( list.map {it.id.toLong()})
 *   }
 *
 *   //rest of the adapter's implementations
 * }
 *
 * class IconsFragment : Fragment(){
 *
 *    private lateinit var iconsAdapter: IconsAdapter
 *
 *    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
 *
 *        iconsAdapter = IconsAdapter(requireContext())
 *
 *        //configure the selection delegate with recyclerview
 *        iconsAdapter.configure(binding.recyclerView, Payload.SELECTION_MODE){ ass ->
 *              toolbarLayout.updateAllSelector(ass.totalSelected, ass.isEnabled, ass.isChecked)
 *        }
 *
 *    }
 *
 *   //rest of the fragment's implementations
 * }
 * ```
 * @param T Type of selectionId to be used. This should be set to `Long` when using [RecyclerView.Adapter.getItemId] as the selection id.  See [configure].
 *
 * @param isSelectable (Optional) lambda to be checked if not all [RecyclerView.ViewHolder] view types are selectable.
 * Required for adapter with multiple view types where not all view types are selectable. This returns `true` to all by default.
 */
@Deprecated(
    "Use the new (refactored) MultiSelectorDelegate found in dev.oneuiproject.oneui.recyclerview.util instead.")
class MultiSelectorDelegate<T>(
    private val isSelectable: (viewType: Int) -> Boolean = { true },
) : MultiSelector<T> {

    //https://developer.android.com/reference/kotlin/androidx/collection/ScatterSet
    private val selectedIds = mutableScatterSetOf<T>()
    private var currentList = emptyList<T>()
    private lateinit var adapter: RecyclerView.Adapter<*>
    private var selectionPayload: Any? = null
    private var firstPosition: Int = NO_POSITION
    private var lastPosition: Int = NO_POSITION
    private var sessionMax = NO_POSITION
    private var sessionMin = NO_POSITION
    private var getSelectionId: ((position: Int) -> T?)? = null
    private var currentAllSelectionState: AllSelectorState = AllSelectorState()
    private var onAllSelectorStateChanged: (selectionId: AllSelectorState) -> Unit = {}
    private var restoreItemAnimator: RecyclerView.ItemAnimator? = null

    override var isActionMode: Boolean = false
        private set

    @Throws(IllegalStateException::class)
    override fun configure(
        recyclerView: RecyclerView,
        selectionChangePayload: Any?,
        selectionId: ((position: Int) -> T?)?,
        onAllSelectorStateChanged: ((allSelectorState: AllSelectorState) -> Unit),
    ) {
        val adapter = recyclerView.adapter
            ?: throw IllegalStateException("RecyclerView must have an attached adapter")

        if (selectionId == null && !adapter.hasStableIds()){
            throw IllegalStateException("Either `selectionId` parameter must be set or" +
                    " the adapter must implement stable Ids.")
        }
        if (adapter !is MultiSelector<*>){
            throw IllegalStateException("Adapter must extend MultiSelector")
        }

        this.adapter = adapter
        this.getSelectionId = selectionId
        this.selectionPayload = selectionChangePayload
        this.onAllSelectorStateChanged = onAllSelectorStateChanged

        recyclerView.apply {
            doOnLongPressMultiSelection(
                onItemSelected = {pos, id ->
                    @Suppress("UNCHECKED_CAST")
                    if (isSelectable(adapter.getItemViewType(pos))) {
                        onSelectItem(getSelectionId?.invoke(pos) ?: id as T, pos)
                    }
                },
                onStateChanged = { state, pos ->
                    when(state){
                        STARTED -> {
                            //Temporarily disable item animator if any
                            itemAnimator?.let {
                                restoreItemAnimator = it
                                itemAnimator = null
                            }
                            if (pos != NO_POSITION) onStateChanged(STARTED, pos)
                        }
                        ENDED -> {
                            scrollToBottomSelected()
                            onStateChanged(ENDED, pos)
                            restoreItemAnimator?.let {
                                itemAnimator = it
                                restoreItemAnimator = null
                            }
                        }
                    }
                }
            )
        }
    }

    private fun RecyclerView.scrollToBottomSelected() {
        val bottomPosition = maxOf(firstPosition, lastPosition)

        findViewHolderForAdapterPosition(bottomPosition)?.let { viewHolder ->
            val itemBottom = viewHolder.itemView.run { y + height }
            postOnAnimationDelayed({
                val rvBottom = Rect().apply { getLocalVisibleRect(this) }.bottom
                val bottomOffset = calculateBottomOffset()
                val scrollDistance = (itemBottom - (rvBottom - bottomOffset)).toInt()
                if (scrollDistance > 0) {
                    isNestedScrollingEnabled = false
                    smoothScrollBy(0, scrollDistance, CachedInterpolatorFactory.getOrCreate(Type.SINE_IN_OUT_60), 300)
                    postDelayed({ isNestedScrollingEnabled = true }, 350)
                }
            }, 750)
        }
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

    override fun updateSelectableIds(selectableIds: List<T>) {
        this.currentList = selectableIds
        if (isActionMode) {
            updateAllSelectorState()
        }
    }

    override fun onToggleActionMode(isActionMode: Boolean, initialSelectedIds: Array<T>?) {
        if (this.isActionMode == isActionMode) return
        this.isActionMode = isActionMode
        if (isActionMode) {
            initialSelectedIds?.let {
                selectedIds.addAll(it)
            }
            updateAllSelectorState()
        }else{
            selectedIds.clear()
        }
        adapter.notifyItemRangeChanged(0, adapter.itemCount, selectionPayload)
    }

    private fun onStateChanged(state: MultiSelectionState, position: Int) {
        when (state) {
            STARTED -> {
                firstPosition = position
                lastPosition = position
                sessionMax = position
                sessionMin = position
                @Suppress("UNCHECKED_CAST")
                if (selectedIds.add(getSelectionId?.invoke(position) ?: adapter.getItemId(position) as T)) {
                    adapter.notifyItemChanged(position, selectionPayload)
                    updateAllSelectorState()
                }
            }

            ENDED -> {
                selectedIds.trim()
                firstPosition = NO_POSITION
                lastPosition = NO_POSITION
                sessionMax = NO_POSITION
                sessionMin = NO_POSITION
            }
        }
    }

    override fun getSelectedIds(): ScatterSet<T> = selectedIds

    private fun onSelectItem(selectionId: T, position: Int) {
        //Note: Avoid using minOf()/maxOf() with 4 or more params -
        //https://www.romainguy.dev/posts/2024/micro-optimizations-in-kotlin-3/
        sessionMax = maxOf(lastPosition, firstPosition, sessionMax)
        sessionMin = minOf(firstPosition, lastPosition, sessionMin)

        lastPosition = position

        if (position in sessionMin..sessionMax) {
            if (!selectedIds.remove(selectionId)) {
                selectedIds.add(selectionId)
            }
            adapter.notifyItemChanged(position, selectionPayload)
            updateAllSelectorState()
        } else {
            if (selectedIds.add(selectionId)) {
                adapter.notifyItemChanged(position, selectionPayload)
                updateAllSelectorState()
            }
        }
    }

    override fun onToggleItem(selectionId: T, position: Int) {
        if (!selectedIds.remove(selectionId)) {
            selectedIds.add(selectionId)
        }
        adapter.notifyItemChanged(position, selectionPayload)
        updateAllSelectorState()
    }

    override fun isSelected(selectionId: T): Boolean {
        return selectedIds.contains(selectionId)
    }


    override fun onToggleSelectAll(isSelectAll: Boolean) {
        if (isSelectAll) {
            selectedIds.apply{
                addAll(currentList)
            }
        } else {
            selectedIds.clear()
        }
        adapter.notifyItemRangeChanged(0, adapter.itemCount, selectionPayload)
        updateAllSelectorState()
    }


    private fun updateAllSelectorState(){
        getActionModeAllSelectorState().let {
            if (it != currentAllSelectionState){
                currentAllSelectionState = it
                onAllSelectorStateChanged.invoke(it)
            }
        }
    }

    private fun getActionModeAllSelectorState(): AllSelectorState {
        if (currentList.isEmpty()){
            Log.d(TAG, "getSelectedIds() returns empty. " +
                    "Ensure that `updateSelectableIds()` is called on each update to the adapter data.")
        }
        val currentDataSetCount = currentList.size
        val isEnabled = currentDataSetCount > 0
        val selectedIds = selectedIds
        val allSelected = if (!isEnabled) null else {
            selectedIds.count { it in currentList } >= currentDataSetCount
        }
        return AllSelectorState(
            selectedIds.size,
            isChecked =  allSelected,
            isEnabled =  isEnabled
        )
    }

    private companion object{
        const val TAG = "MultiSelectorDelegate"
    }
}

/**
 * Interface for implementing multi-selection in a [RecyclerView.Adapter].
 *
 * This interface provides methods for configuring multi-selection, managing the selection state,
 * and interacting with the selection mode.
 *
 * @param T The type of the selection ID. This should typically be a stable identifier for your items,
 *          such as `Long` when using [RecyclerView.Adapter.getItemId].
 */
@Deprecated("Use MultiSelector found in dev.oneuiproject.oneui.recyclerview.util")
interface MultiSelector<T> {
    /**
     * Configure the multi selector.
     *
     * @param recyclerView The `RecyclerView` to configure. A [RecyclerView.Adapter] must already be attached.
     * @param selectionChangePayload (Optional) Change payload for more efficient updating of selected items.
     * @param selectionId (Optional) Lambda to be invoked to get the item id used for selection.
     * If not set (or `null`), adapter must set [RecyclerView.Adapter.hasStableIds] to true and implement [RecyclerView.Adapter.getItemId]
     * @param onAllSelectorStateChanged  (Optional) Lambda to be invoked to [AllSelectorState] changes.
     */
    fun configure(
        recyclerView: RecyclerView,
        selectionChangePayload: Any? = null,
        selectionId: ((position: Int) -> T?)? = null,
        onAllSelectorStateChanged: ((allSelectorState: AllSelectorState) -> Unit)
    )

    /**
     * Set the current list of selectable Ids for items submitted to the adapter.
     * Call this everytime a new list is submitted to the adapter.
     */
    fun updateSelectableIds(selectableIds: List<T>)

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
    fun onToggleActionMode(isActionMode: Boolean, initialSelectedIds: Array<T>? = null)

    /**
     * Invoke this to toggle an item's selected state - selects if unselected, unselects if selected.
     *
     * @param selectionId Selection Id of the item selected/unselected
     * @param position Adapter position of the item selected/unselected
     */
    fun onToggleItem(selectionId: T, position: Int)

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
    fun isSelected(selectionId: T): Boolean

    /** Get the current list of selected Ids. */
    fun getSelectedIds(): ScatterSet<T>

    /** This property is set by [onToggleActionMode] */
    val isActionMode: Boolean
}


