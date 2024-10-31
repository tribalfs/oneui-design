package dev.oneuiproject.oneui.delegates

import android.util.Log
import androidx.collection.ScatterSet
import androidx.collection.mutableScatterSetOf
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemAnimator
import androidx.recyclerview.widget.RecyclerView.NO_POSITION
import dev.oneuiproject.oneui.ktx.MultiSelectionState
import dev.oneuiproject.oneui.ktx.MultiSelectionState.ENDED
import dev.oneuiproject.oneui.ktx.MultiSelectionState.STARTED
import dev.oneuiproject.oneui.ktx.doOnLongPressMultiSelection

/**
 * Delegate for multi-selection in OneUI style.
 *
 * @param T Type of selectionId to be used. This should be set to `Long` when using [RecyclerView.Adapter.getItemId] as the selection id.  See [configure].
 *
 * @param isSelectable (Optional) lambda to be checked if not all [RecyclerView.ViewHolder] view types are selectable.
 * Required for adapter with multiple view types where not all view types are selectable. This returns `true` to all by default.
 *
 *
 * Example usage:
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
 *        iconsAdapter.configure(binding.recyclerView, Payload.SELECTION_MODE)
 *
 *    }
 *
 *   //rest of the fragment's implementations
 * }
 * ```
 */
class MultiSelectorDelegate<T>(
    private val isSelectable: (viewType: Int) -> Boolean = { true },
) : MultiSelector<T>{

    //https://developer.android.com/reference/kotlin/androidx/collection/ScatterSet
    private val selectedIds = mutableScatterSetOf<T>()
    private var currentList = emptyList<T>()
    private lateinit var adapter: RecyclerView.Adapter<*>
    private var selectionPayload: Any? = null
    private var firstPosition: Int = NO_POSITION
    private var lastPosition: Int = NO_POSITION
    private var sessionMax = NO_POSITION
    private var sessionMin = NO_POSITION
    private var restoreItemAnimator: ItemAnimator? = null
    private var getSelectionId: ((position: Int) -> T?)? = null
    private var currentAllSelectionState: AllSelectorState = AllSelectorState()
    private var onAllSelectedStateChanged: (selectionId: AllSelectorState) -> Unit = {}

    override var isActionMode: Boolean = false
        private set

    @Throws(IllegalStateException::class)
    override fun configure(
        recyclerView: RecyclerView,
        selectionChangePayload: Any?,
        selectionId: ((position: Int) -> T?)?,
        onAllSelectedStateChanged: ((selectionId: AllSelectorState) -> Unit),
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
        this.onAllSelectedStateChanged = onAllSelectedStateChanged

        recyclerView.apply {
            doOnLongPressMultiSelection(
                onItemSelected = {pos, id ->
                    @Suppress("UNCHECKED_CAST")
                    if (isSelectable(adapter.getItemViewType(pos))) {
                        onSelectItem(getSelectionId?.invoke(pos) ?: id as T, pos)
                    }
                },
                onStateChanged = {state, pos ->
                    when(state){
                        STARTED -> if (pos != NO_POSITION) onStateChanged(STARTED, pos)
                        ENDED -> onStateChanged(ENDED, pos)
                    }
                }
            )
        }
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
        }else{
            selectedIds.clear()
        }
        adapter.notifyItemRangeChanged(0, adapter.itemCount, selectionPayload)
        updateAllSelectorState()
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
                onAllSelectedStateChanged.invoke(it)
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

    companion object{
        private const val TAG = "MultiSelectorDelegate"
    }
}

interface MultiSelector<T> {
    /**
     * Configure the multi selector.
     *
     * @param recyclerView The `RecyclerView` to configure. A [RecyclerView.Adapter] must already be attached.
     * @param selectionChangePayload (Optional) Change payload for more efficient updating of selected items.
     * @param selectionId (Optional) Lambda to be invoked to get the item id used for selection.
     * If not set (or `null`), adapter must set [RecyclerView.Adapter.hasStableIds] to true and implement [RecyclerView.Adapter.getItemId]
     */
    fun configure(
        recyclerView: RecyclerView,
        selectionChangePayload: Any? = null,
        selectionId: ((position: Int) -> T?)? = null,
        onAllSelectedStateChanged: ((selectionId: AllSelectorState) -> Unit)
    )

    /**
     * Set the current  list of selectable stable Ids for items submitted to the adapter.
     * Call this everytime a new list is submitted to the adapter.
     */
    fun updateSelectableIds(selectableIds: List<T>)

    /**
     * Invoke this everytime [ToolbarLayout.startActionMode][dev.oneuiproject.oneui.layout.ToolbarLayout.startActionMode] or
     * [ToolbarLayout.endActionMode][dev.oneuiproject.oneui.layout.ToolbarLayout.endActionMode] is invoked.
     *
     * This automatically notifies the adapter, using the `selectionPayload` if provided, for it to rebind its views.
     * This also updates `isActionMode` property value.
     *
     * @param isActionMode Whether action mode is turned on `true` or not `false`.
     * @param initialSelectedIds (Optional) Array of stable ids of initially selected items if [isActionMode] is true
     */
    fun onToggleActionMode(isActionMode: Boolean, initialSelectedIds: Array<T>? = null)

    /**
     * Invoke this to toggles item selected state - selects if unselected, unselects if selected.
     * @param selectionId Selection Id of the item selected/unselected
     * @param position Adapter position of the item selected/unselected
     */
    fun onToggleItem(selectionId: T, position: Int)

    /**
     *
     */
    fun onToggleSelectAll(isSelectAll: Boolean)

    fun isSelected(selectionId: T): Boolean

    fun getSelectedIds(): ScatterSet<T>

    /**
     * This property is set by [onToggleActionMode]
     */
    val isActionMode: Boolean
}

/**
 * @param totalSelected Number of selected items.
 * @param isChecked Set to true if all visible items are selected. Otherwise, false. Set to `null` to keep the existing state.
 * @param isEnabled (Optional) Set to enable/disable all selector button. Is set to `true` by default.
 */
data class AllSelectorState(
    @JvmField
    val totalSelected: Int = 0,
    @JvmField
    val isChecked: Boolean? = null,
    @JvmField
    val isEnabled: Boolean = true
)


