package dev.oneuiproject.oneuiexample.ui.main.fragments.recyclerview.icons.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.sec.sesl.tester.databinding.ViewIconListviewItemBinding
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.FragmentScoped
import dev.oneuiproject.oneui.delegates.MultiSelector
import dev.oneuiproject.oneui.delegates.MultiSelectorDelegate
import dev.oneuiproject.oneui.delegates.SectionIndexerDelegate
import dev.oneuiproject.oneui.delegates.SemSectionIndexer
import dev.oneuiproject.oneui.utils.SearchHighlighter
import dev.oneuiproject.oneui.widget.SelectableLinearLayout
import dev.oneuiproject.oneuiexample.ui.main.fragments.recyclerview.icons.IconListItemUiModel
import dev.oneuiproject.oneuiexample.ui.main.fragments.recyclerview.contacts.adapter.ContactsAdapter
import java.util.Locale
import javax.inject.Inject

@FragmentScoped
class IconsAdapter @Inject constructor(
    @ApplicationContext private val context: Context
) : RecyclerView.Adapter<IconsAdapter.ViewHolder?>(),
    SemSectionIndexer<IconListItemUiModel> by SectionIndexerDelegate(
        context,
        labelExtractor = { getIndexerLabel(context, it) }),
    MultiSelector<Int> by MultiSelectorDelegate() {

    private val asyncListDiffer = AsyncListDiffer(
        this,
        object : DiffUtil.ItemCallback<IconListItemUiModel>() {
            override fun areItemsTheSame(
                oldItem: IconListItemUiModel,
                newItem: IconListItemUiModel
            ): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(
                oldItem: IconListItemUiModel,
                newItem: IconListItemUiModel
            ): Boolean {
                return oldItem.name == newItem.name
            }
        })

    private val currentList: List<IconListItemUiModel> get() = asyncListDiffer.currentList

    var onClickItem: ((IconListItemUiModel, Int) -> Unit)? = null
    var onLongClickItem: (() -> Unit)? = null

    var highlightWord = ""
        set(value) {
            if (value != field) {
                field = value
                notifyItemRangeChanged(0, itemCount, ContactsAdapter.Payload.HIGHLIGHT)
            }
        }

    fun submitList(list: List<IconListItemUiModel>) {
        updateSections(list, false)
        asyncListDiffer.submitList(list)
        updateSelectableIds(list.map { it.id })
    }

    fun getItem(position: Int): IconListItemUiModel = currentList[position]

    override fun getItemCount(): Int = currentList.size


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = ViewIconListviewItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
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
        }else{
            val item = currentList[position]
            for (payload in payloads.toSet()) {
                when(payload){
                    ContactsAdapter.Payload.SELECTION_MODE -> holder.bindSelected(item)
                    ContactsAdapter.Payload.HIGHLIGHT -> holder.bindText(item.name)
                }
            }
        }
    }

    val selectedIdsAsIntArray: IntArray
        get() {
            val selectedIds = getSelectedIds().asSet()
            val intArray = IntArray(selectedIds.size)
            var index = 0
            for (id in selectedIds) {
                intArray[index++] = id
            }
            return intArray
        }

    inner class ViewHolder internal constructor(binding: ViewIconListviewItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        private val stringHighlight = SearchHighlighter(binding.root.context)//needs theme context
        private var imageView: ImageView = binding.iconListItemIcon
        private  var textView: TextView = binding.iconListItemText

        fun bind( item: IconListItemUiModel){
            bindSelected(item)
            bindText(item.name)
            imageView.setImageResource(item.id)
                (itemView as? SelectableLinearLayout)?.apply {
                    isSelectionMode = isActionMode
                    setSelected(isSelected(item.id))
                }
        }

        fun bindText(itemName: String){
            textView.text = stringHighlight(itemName, highlightWord)
        }

        fun bindSelected(item: IconListItemUiModel) {
            itemView.setSelected(isSelected(item.id))
        }
    }

    companion object {
        fun getIndexerLabel(context: Context, icon: IconListItemUiModel): String {
            val firstChar = icon.name.replace("ic_oui_", "")[0]
            if (Character.isDigit(firstChar)) {
                return "#"
            }
            return firstChar.toString().uppercase(Locale.getDefault())
        }
    }
}
