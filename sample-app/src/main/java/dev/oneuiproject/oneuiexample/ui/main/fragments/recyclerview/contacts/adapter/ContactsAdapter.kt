package dev.oneuiproject.oneuiexample.ui.main.fragments.recyclerview.contacts.adapter

import android.app.ActionBar.LayoutParams
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.MarginLayoutParams
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.sec.sesl.tester.R
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.FragmentScoped
import dev.oneuiproject.oneui.delegates.MultiSelector
import dev.oneuiproject.oneui.delegates.MultiSelectorDelegate
import dev.oneuiproject.oneui.delegates.SectionIndexerDelegate
import dev.oneuiproject.oneui.delegates.SemSectionIndexer
import dev.oneuiproject.oneui.ktx.dpToPx
import dev.oneuiproject.oneui.utils.SearchHighlighter
import dev.oneuiproject.oneui.widget.SelectableLinearLayout
import dev.oneuiproject.oneui.widget.Separator
import dev.oneuiproject.oneuiexample.ui.main.fragments.recyclerview.contacts.model.ContactsListItemUiModel
import javax.inject.Inject

@FragmentScoped
class ContactsAdapter @Inject constructor (
    @ApplicationContext private val context: Context
) : RecyclerView.Adapter<ContactsAdapter.ViewHolder>(),
    MultiSelector<Long> by MultiSelectorDelegate(isSelectable = { it != ContactsListItemUiModel.SeparatorItem.Companion.VIEW_TYPE }),
    SemSectionIndexer<ContactsListItemUiModel> by SectionIndexerDelegate(context, labelExtractor = { getLabel(it) }) {

    init {
        setHasStableIds(true)
    }

    private val asyncListDiffer = AsyncListDiffer(this,
        object : DiffUtil.ItemCallback<ContactsListItemUiModel>() {
            override fun areItemsTheSame(oldItem: ContactsListItemUiModel, newItem: ContactsListItemUiModel): Boolean {
                if (oldItem is ContactsListItemUiModel.ContactItem && newItem is ContactsListItemUiModel.ContactItem){
                    return  oldItem.contact == newItem.contact
                }
                if (oldItem is ContactsListItemUiModel.SeparatorItem && newItem is ContactsListItemUiModel.SeparatorItem){
                    return  oldItem.indexText == newItem.indexText
                }
                return false
            }
            override fun areContentsTheSame(oldItem: ContactsListItemUiModel, newItem: ContactsListItemUiModel): Boolean {
                return oldItem == newItem
            }
        })

    private val currentList: List<ContactsListItemUiModel> get() = asyncListDiffer.currentList

    var onClickItem: ((ContactsListItemUiModel, Int) -> Unit)? = null

    var onLongClickItem: (() -> Unit)? = null

    fun submitList(listItems: List<ContactsListItemUiModel>){
        updateSections(listItems, true)
        asyncListDiffer.submitList(listItems)
        updateSelectableIds(listItems.filter {it !is ContactsListItemUiModel.SeparatorItem }.map { it.toStableId() } )
    }

    var highlightWord = ""
        set(value) {
            if (value != field) {
                field = value
                notifyItemRangeChanged(0, itemCount, Payload.HIGHLIGHT)
            }
        }


    fun getItemByPosition(position: Int) = currentList[position]

    override fun getItemId(position: Int) = currentList[position].toStableId()

    override fun getItemCount(): Int = currentList.size

    override fun getItemViewType(position: Int): Int {
        return when(currentList[position]){
            is ContactsListItemUiModel.GroupItem -> ContactsListItemUiModel.GroupItem.Companion.VIEW_TYPE
            is ContactsListItemUiModel.ContactItem -> ContactsListItemUiModel.ContactItem.Companion.VIEW_TYPE
            is ContactsListItemUiModel.SeparatorItem -> ContactsListItemUiModel.SeparatorItem.Companion.VIEW_TYPE
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val context = parent.context

        when(viewType){
            ContactsListItemUiModel.GroupItem.Companion.VIEW_TYPE,
            ContactsListItemUiModel.ContactItem.Companion.VIEW_TYPE -> {
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

            ContactsListItemUiModel.SeparatorItem.Companion.VIEW_TYPE -> {
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
                            is ContactsListItemUiModel.ContactItem -> holder.bindNameContact(item.contact.name, item.contact.number)
                            is ContactsListItemUiModel.GroupItem -> holder.bindNameContact(item.groupName, null)
                            else -> Unit
                        }
                    }
                }
            }
        }
    }

    override fun onViewRecycled(holder: ViewHolder) {
        super.onViewRecycled(holder).also {
            holder.selectableLinearLayout?.apply {
                setOverlayCornerRadius(18f.dpToPx(resources).toFloat())
            }
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        when(val item = currentList[position]){
            is ContactsListItemUiModel.GroupItem -> holder.bind(getItemId(position), item.groupName, R.drawable.indexscroll_group_icon, null)
            is ContactsListItemUiModel.ContactItem -> {
                val imageRes = if (item.contact.name == "Tribalfs") {
                    holder.selectableLinearLayout!!.setOverlayCornerRadius(20f)
                    R.drawable.about_page_avatar_tribalfs
                }else {
                    R.drawable.indexscroll_item_icon
                }
                holder.bind(getItemId(position), item.contact.name, imageRes, item.contact.number)
            }
            is ContactsListItemUiModel.SeparatorItem -> holder.nameView.text = item.indexText
        }
    }

    inner class ViewHolder (itemView: View, var isSeparator: Boolean) :
        RecyclerView.ViewHolder(itemView) {

        var nameView: TextView
        private var imageView: ImageView? = null
        private var numberView: TextView? = null
        var selectableLinearLayout: SelectableLinearLayout? = null
        private val stringHighlight = SearchHighlighter(itemView.context)

        init {
            if (isSeparator) {
                nameView = itemView as TextView
            } else {
                selectableLinearLayout = itemView.findViewById(R.id.selectable_layout)!!
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
            selectableLinearLayout?.apply {
                isSelectionMode = isActionMode
                setSelected(isSelected(itemId))
            }
        }

        fun bindActionModeAnimate(itemId: Long){
            selectableLinearLayout?.apply {
                isSelectionMode = isActionMode
                setSelectedAnimate(isSelected(itemId))
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
                is ContactsListItemUiModel.ContactItem -> uiModel.contact.name
                is ContactsListItemUiModel.GroupItem -> "\uD83D\uDC65"
                is ContactsListItemUiModel.SeparatorItem -> uiModel.indexText
            }
        }
    }

}