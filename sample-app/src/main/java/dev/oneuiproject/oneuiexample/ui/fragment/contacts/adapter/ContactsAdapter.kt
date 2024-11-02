package dev.oneuiproject.oneuiexample.ui.fragment.contacts.adapter

import android.app.ActionBar.LayoutParams
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.MarginLayoutParams
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.sec.sesl.tester.R
import dev.oneuiproject.oneui.delegates.MultiSelector
import dev.oneuiproject.oneui.delegates.MultiSelectorDelegate
import dev.oneuiproject.oneui.delegates.SectionIndexerDelegate
import dev.oneuiproject.oneui.delegates.SemSectionIndexer
import dev.oneuiproject.oneui.utils.SearchHighlighter
import dev.oneuiproject.oneui.widget.Separator
import dev.oneuiproject.oneuiexample.ui.fragment.contacts.model.ContactsListItemUiModel
import dev.oneuiproject.oneuiexample.ui.fragment.contacts.model.ContactsListItemUiModel.ContactItem
import dev.oneuiproject.oneuiexample.ui.fragment.contacts.model.ContactsListItemUiModel.GroupItem
import dev.oneuiproject.oneuiexample.ui.fragment.contacts.model.ContactsListItemUiModel.SeparatorItem

class ContactsAdapter (
    private val context: Context
) : RecyclerView.Adapter<ContactsAdapter.ViewHolder>(),

    MultiSelector<Long> by MultiSelectorDelegate(isSelectable = { it != SeparatorItem.VIEW_TYPE }),

    SemSectionIndexer<ContactsListItemUiModel> by SectionIndexerDelegate(context, labelExtractor = { getLabel(it) }) {

    init {
        setHasStableIds(true)
    }

    private val stringHighlight = SearchHighlighter()

    private val asyncListDiffer = AsyncListDiffer(this,
        object : DiffUtil.ItemCallback<ContactsListItemUiModel>() {
            override fun areItemsTheSame(oldItem: ContactsListItemUiModel, newItem: ContactsListItemUiModel): Boolean {
                if (oldItem is ContactItem && newItem is ContactItem){
                    return  oldItem.contact == newItem.contact
                }
                if (oldItem is SeparatorItem && newItem is SeparatorItem){
                    return  oldItem.indexText == newItem.indexText
                }
                return false
            }
            override fun areContentsTheSame(oldItem: ContactsListItemUiModel, newItem: ContactsListItemUiModel): Boolean {
                return oldItem == newItem
            }
        })

    var onClickItem: ((ContactsListItemUiModel, Int) -> Unit)? = null

    var onLongClickItem: (() -> Unit)? = null

    fun submitList(listItems: List<ContactsListItemUiModel>){
        updateSections(listItems, true)
        asyncListDiffer.submitList(listItems)
        updateSelectableIds(listItems.filter {it !is SeparatorItem}.map { it.toStableId() } )
    }

    var highlightWord = ""
        set(value) {
            if (value != field) {
                field = value
                notifyItemRangeChanged(0, itemCount, Payload.HIGHLIGHT)
            }
        }

    private val currentList: List<ContactsListItemUiModel> get() = asyncListDiffer.currentList

    fun getItemByPosition(position: Int) = currentList[position]

    override fun getItemId(position: Int) = currentList[position].toStableId()

    override fun getItemCount(): Int = currentList.size

    override fun getItemViewType(position: Int): Int {
        return when(currentList[position]){
            is GroupItem -> GroupItem.VIEW_TYPE
            is ContactItem -> ContactItem.VIEW_TYPE
            is SeparatorItem -> SeparatorItem.VIEW_TYPE
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val context = parent.context

        when(viewType){
            GroupItem.VIEW_TYPE,
            ContactItem.VIEW_TYPE -> {
                val inflater = LayoutInflater.from(context)
                val view = inflater.inflate(
                    R.layout.view_contacts_list_item, parent, false
                )
                return ViewHolder(view, false).apply {
                    itemView.apply {
                        setOnClickListener {
                            bindingAdapterPosition.let{
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
                    Payload.SELECTION_MODE -> holder.bindActionMode(getItemId(position))
                    Payload.HIGHLIGHT -> {
                        when (item){
                            is ContactItem -> holder.bindNameContact(item.contact.name, item.contact.number)
                            is GroupItem -> holder.bindNameContact(item.groupName, null)
                            else -> Unit
                        }
                    }
                }
            }
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        when(val item = currentList[position]){
            is GroupItem -> holder.bind(getItemId(position), item.groupName, R.drawable.indexscroll_group_icon, null)
            is ContactItem -> {
                val imageRes = if (item.contact.name == "Tribalfs") {
                    R.drawable.about_page_avatar_tribalfs
                }else {
                    R.drawable.indexscroll_item_icon
                }
                holder.bind(getItemId(position), item.contact.name, imageRes, item.contact.number)
            }
            is SeparatorItem -> holder.nameView.text = item.indexText
        }
    }

    inner class ViewHolder (itemView: View, var isSeparator: Boolean) :
        RecyclerView.ViewHolder(itemView) {

        var nameView: TextView
        private var imageView: ImageView? = null
        private var numberView: TextView? = null
        private var checkBox: CheckBox? = null

        init {
            if (isSeparator) {
                nameView = itemView as TextView
            } else {
                checkBox = itemView.findViewById(R.id.contact_item_checkbox)!!
                nameView = itemView.findViewById(R.id.contact_item_name)
                imageView = itemView.findViewById(R.id.contact_item_icon)
                numberView = itemView.findViewById(R.id.contact_item_number)
            }
        }

        fun bind(itemId: Long, name: String, imageRes: Int, number: String?){
            imageView!!.setImageResource(imageRes)
            bindNameContact(name, number)
            bindActionMode(itemId)

        }

        fun bindNameContact(name: String, number: String?){
            nameView.text =  stringHighlight(name, highlightWord)
            number?.let{
                numberView!!.apply{
                    text = stringHighlight(it, highlightWord)
                    isVisible = true
                }
            } ?: run { numberView?.isVisible = false }
        }

        fun bindActionMode(itemId: Long){
            checkBox?.apply {
                isVisible = isActionMode
                isChecked = isSelected(itemId)
            }
        }
    }

    enum class Payload{
        SELECTION_MODE,
        HIGHLIGHT
    }

    companion object{
        private const val TAG = "ContactsAdapter"
        fun getLabel(uiModel: ContactsListItemUiModel): String{
            return when (uiModel) {
                is ContactItem -> uiModel.contact.name
                is GroupItem -> "\uD83D\uDC65"
                is SeparatorItem -> uiModel.indexText
            }
        }
    }

}