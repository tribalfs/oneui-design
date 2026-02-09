package dev.oneuiproject.oneuiexample.ui.main.fragments.recyclerview.stargazers.adapter

import android.app.ActionBar.LayoutParams
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.MarginLayoutParams
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.sec.sesl.tester.R
import dev.oneuiproject.oneui.layout.ToolbarLayout.AllSelectorState
import dev.oneuiproject.oneui.recyclerview.adapter.IndexedSelectableListAdapter
import dev.oneuiproject.oneui.recyclerview.model.AdapterItem
import dev.oneuiproject.oneui.utils.SearchHighlighter
import dev.oneuiproject.oneui.widget.SelectableLinearLayout
import dev.oneuiproject.oneui.widget.Separator
import dev.oneuiproject.oneuiexample.ui.main.fragments.recyclerview.stargazers.model.StargazersListItemUiModel
import dev.oneuiproject.oneuiexample.ui.main.fragments.recyclerview.stargazers.model.StargazersListItemUiModel.GroupItem
import dev.oneuiproject.oneuiexample.ui.main.fragments.recyclerview.stargazers.model.StargazersListItemUiModel.SeparatorItem
import dev.oneuiproject.oneuiexample.ui.main.fragments.recyclerview.stargazers.model.StargazersListItemUiModel.StargazerItem
import dev.oneuiproject.oneuiexample.ui.main.fragments.recyclerview.stargazers.util.loadImageFromUrl

class StargazersAdapter(
    onAllSelectorStateChanged: ((AllSelectorState) -> Unit),
    onBlockActionMode: (() -> Unit),
) :
    IndexedSelectableListAdapter<StargazersListItemUiModel, StargazersAdapter.ViewHolder, Long>(
        indexLabelExtractor = indexLabelExtractor,
        onAllSelectorStateChanged = onAllSelectorStateChanged,
        onBlockActionMode = onBlockActionMode,
        selectableIdsProvider = selectableIdsProvider,
        isSelectable = isSelectable,
        selectionChangePayload = Payload.SELECTION_MODE,
        diffCallback = diffCallback
) {

    init {
        setHasStableIds(true)
    }

    private lateinit var stringHighlight: SearchHighlighter

    var searchHighlightColor: Int
        @ColorInt
        get() = stringHighlight.highlightColor
        set(@ColorInt color) {
            if (stringHighlight.highlightColor != color) {
                stringHighlight.highlightColor = color
                notifyItemRangeChanged(0, itemCount, Payload.HIGHLIGHT)
            }
        }

    var onClickItem: ((StargazersListItemUiModel, Int, ViewHolder) -> Unit)? = null

    var onLongClickItem: (() -> Unit)? = null


    var highlightWord: String = ""
        set(value) {
            if (value != field) {
                field = value
                notifyItemRangeChanged(0, itemCount, Payload.HIGHLIGHT)
            }
        }


    override fun getItemId(position: Int) = currentList[position].toStableId()

    fun getItemByPosition(position: Int): StargazersListItemUiModel = currentList[position]

    override fun getItemViewType(position: Int): Int {
        return when(currentList[position]){
            is GroupItem -> GroupItem.VIEW_TYPE
            is StargazerItem -> StargazerItem.VIEW_TYPE
            is SeparatorItem -> SeparatorItem.VIEW_TYPE
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val context = parent.context
        stringHighlight = SearchHighlighter(context)

        when(viewType){
            GroupItem.VIEW_TYPE,
            StargazerItem.VIEW_TYPE -> {
                val inflater = LayoutInflater.from(context)
                val view = inflater.inflate(
                    R.layout.view_stargazers_list_item, parent, false
                )
                return ViewHolder(view, false).apply vh@{
                    itemView.apply {
                        setOnClickListener {
                            bindingAdapterPosition.let{
                                onClickItem?.invoke(currentList[it],it,this@vh)
                            }
                        }
                        setOnLongClickListener {
                            onLongClickItem?.invoke()
                            true
                        }
                    }
                }
            }

            SeparatorItem.VIEW_TYPE -> {
                return ViewHolder(Separator(context), true).apply {
                    itemView.layoutParams = MarginLayoutParams(MATCH_PARENT, LayoutParams.WRAP_CONTENT)
                }
            }

            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int, payloads: MutableList<Any>) {

        if (payloads.isEmpty()) {
            onBindViewHolder(holder, position)
        }else{
            val item = currentList[position]
            for (payload in payloads.toSet()) {
                when(payload){
                    Payload.SELECTION_MODE -> holder.bindActionModeAnimate(getItemId(position))
                    Payload.HIGHLIGHT -> {
                        when (item){
                            is StargazerItem -> with(item.stargazer){ holder.bindDetails(getDisplayName(), html_url) }
                            is GroupItem -> holder.bindDetails(item.groupName, null)
                            else -> Unit
                        }
                    }
                }
            }
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        when(val item = currentList[position]){
            is GroupItem -> holder.bind(getItemId(position), item.groupName, null, null)
            is StargazerItem -> {
                with(item.stargazer) {
                    holder.bind(
                        getItemId(position),
                        getDisplayName(),
                        avatar_url,
                        html_url
                    )
                }
                holder.itemView.transitionName = item.stargazer.id.toString()
            }
            is SeparatorItem -> holder.nameView.text = item.indexText
        }
    }

    inner class ViewHolder (itemView: View, var isSeparator: Boolean) :
        RecyclerView.ViewHolder(itemView) {

        var nameView: TextView
        var avatarView: ImageView? = null
        private var githubView: TextView? = null
        private var selectableLayout: SelectableLinearLayout? = null

        init {
            if (isSeparator) {
                nameView = itemView as TextView
            } else {
                selectableLayout = itemView.findViewById(R.id.selectable_layout)!!
                nameView = itemView.findViewById(R.id.stargazer_name)
                avatarView = itemView.findViewById(R.id.stargazer_avatar)
                githubView = itemView.findViewById(R.id.stargazer_github)
            }
        }

        fun bind(itemId: Long, name: String, imageUrl: String?, number: String?){
            if (imageUrl != null) {
                avatarView!!.loadImageFromUrl(imageUrl)
            }
            bindDetails(name, number)
            bindActionMode(itemId)

        }

        fun bindDetails(name: String, number: String?){
            nameView.text =  stringHighlight(name, highlightWord)
            number?.let{
                githubView!!.apply{
                    text = stringHighlight(it, highlightWord)
                    isVisible = true
                }
            } ?: run { githubView?.isVisible = false }
        }

        fun bindActionMode(itemId: Long){
            selectableLayout?.apply {
                isSelectionMode = isActionMode
                setSelected(isSelected(itemId))
            }
        }

        fun bindActionModeAnimate(itemId: Long){
            selectableLayout?.apply {
                isSelectionMode = isActionMode
                setSelectedAnimate(isSelected(itemId))
            }
        }
    }

    enum class Payload{
        SELECTION_MODE,
        HIGHLIGHT
    }

    companion object {
        private val indexLabelExtractor: (StargazersListItemUiModel) -> CharSequence =
            { uiModel ->
                when (uiModel) {
                    is StargazerItem -> uiModel.stargazer.getDisplayName().first().toString().uppercase()
                    is GroupItem -> "\uD83D\uDC65"
                    is SeparatorItem -> uiModel.indexText
                }
            }

        private val isSelectable: ((rv: RecyclerView, item: AdapterItem) -> Boolean) =
            { rv, item -> (rv.adapter as StargazersAdapter).getItemByPosition(item.position) !is SeparatorItem }

        private val selectableIdsProvider: (currentList: List<StargazersListItemUiModel>) -> List<Long> =
            { listItems -> listItems.filter {it !is SeparatorItem}.map { it.toStableId() } }

        private val diffCallback = object :DiffUtil.ItemCallback<StargazersListItemUiModel>() {
            override fun areItemsTheSame(
                oldItem: StargazersListItemUiModel,
                newItem: StargazersListItemUiModel
            ): Boolean {
                if (oldItem is StargazerItem && newItem is StargazerItem) {
                    return oldItem.stargazer == newItem.stargazer
                }
                if (oldItem is SeparatorItem && newItem is SeparatorItem) {
                    return oldItem.indexText == newItem.indexText
                }
                return false
            }

            override fun areContentsTheSame(
                oldItem: StargazersListItemUiModel,
                newItem: StargazersListItemUiModel
            ): Boolean {
                return oldItem == newItem
            }
        }
    }

}