package dev.oneuiproject.oneuiexample.ui.main.fragments.recyclerview.icons.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.sec.sesl.tester.databinding.ViewIconListviewItemBinding
import dev.oneuiproject.oneui.layout.ToolbarLayout.AllSelectorState
import dev.oneuiproject.oneui.recyclerview.adapter.IndexedSelectableListAdapter
import dev.oneuiproject.oneui.recyclerview.model.AdapterItem
import dev.oneuiproject.oneui.utils.SearchHighlighter
import dev.oneuiproject.oneui.widget.SelectableLinearLayout
import dev.oneuiproject.oneuiexample.ui.main.fragments.recyclerview.icons.IconListItemUiModel
import dev.oneuiproject.oneuiexample.ui.main.fragments.recyclerview.stargazers.adapter.StargazersAdapter

class IconsAdapter(
    onAllSelectorStateChanged: ((AllSelectorState) -> Unit),
    onBlockActionMode: (() -> Unit)
):
    IndexedSelectableListAdapter<IconListItemUiModel, IconsAdapter.ViewHolder, Int>(
        indexLabelExtractor,
        onAllSelectorStateChanged,
        onBlockActionMode,
        selectableIdsProvider,
        selectionIdProvider,
        null,
        null,
        diffCallback
) {

    var onClickItem: ((IconListItemUiModel, Int) -> Unit)? = null
    var onLongClickItem: (() -> Unit)? = null

    var highlightWord: String = ""
        set(value) {
            if (value != field) {
                field = value
                notifyItemRangeChanged(0, itemCount, StargazersAdapter.Payload.HIGHLIGHT)
            }
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view =
            ViewIconListviewItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(view).apply {
            itemView.apply {
                setOnClickListener {
                    bindingAdapterPosition.let {
                        onClickItem?.invoke(currentList[it], it)
                    }
                }
                setOnLongClickListener {
                    onLongClickItem?.invoke()
                    true
                }
            }
        }
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) = holder.bind(currentList[position])

    override fun onBindViewHolder(holder: ViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isEmpty()) {
            onBindViewHolder(holder, position)
        } else {
            val item = currentList[position]
            for (payload in payloads.toSet()) {
                when (payload) {
                    StargazersAdapter.Payload.SELECTION_MODE -> holder.bindSelected(item)
                    StargazersAdapter.Payload.HIGHLIGHT -> holder.bindText(item.name)
                }
            }
        }
    }

    inner class ViewHolder internal constructor(binding: ViewIconListviewItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        private val stringHighlight = SearchHighlighter(binding.root.context)//needs theme context
        private var imageView: ImageView = binding.iconListItemIcon
        private var textView: TextView = binding.iconListItemText

        fun bind(item: IconListItemUiModel) {
            bindSelected(item)
            bindText(item.name)
            imageView.setImageResource(item.id)
            (itemView as? SelectableLinearLayout)?.apply {
                isSelectionMode = isActionMode
                setSelected(isSelected(item.id))
            }
        }

        fun bindText(itemName: String) {
            textView.text = stringHighlight(itemName, highlightWord)
        }

        fun bindSelected(item: IconListItemUiModel) {
            itemView.setSelected(isSelected(item.id))
        }
    }

    companion object {
        private val indexLabelExtractor: (IconListItemUiModel) -> CharSequence =
            { item ->
                val firstChar = item.name.replace("ic_oui_", "")[0]
                if (Character.isDigit(firstChar)) {
                    "#"
                } else {
                    firstChar.toString().uppercase()
                }
            }

        private val selectableIdsProvider: (currentList: List<IconListItemUiModel>) -> List<Int> =
            { list -> list.map { it.id } }

        private val diffCallback = object : DiffUtil.ItemCallback<IconListItemUiModel>() {
            override fun areItemsTheSame(oldItem: IconListItemUiModel, newItem: IconListItemUiModel) = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: IconListItemUiModel, newItem: IconListItemUiModel) = oldItem.name == newItem.name
        }

        private val selectionIdProvider: ((RecyclerView, AdapterItem) -> Int) =
            { rv, item -> (rv.adapter as IconsAdapter).getItem(item.position).id }

    }
}
