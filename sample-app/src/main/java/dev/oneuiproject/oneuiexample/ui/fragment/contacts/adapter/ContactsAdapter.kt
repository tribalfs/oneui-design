package dev.oneuiproject.oneuiexample.ui.fragment.contacts.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.SectionIndexer
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.sec.sesl.tester.R
import dev.oneuiproject.oneui.widget.Separator

class ContactsAdapter(private val mContext: Context) : RecyclerView.Adapter<ContactsAdapter.ViewHolder>(), SectionIndexer {
        var mSections: MutableList<String> = ArrayList()
        var mPositionForSection: MutableList<Int> = ArrayList()
        var mSectionForPosition: MutableList<Int> = ArrayList()

        private var mContactsList: List<String> = emptyList()

        private fun updateSections() {
            mSections.add("")
            mPositionForSection.add(0)
            mSectionForPosition.add(0)

            for (i in 1 until mContactsList.size) {
                val letter = mContactsList[i]
                if (letter.length == 1) {
                    mSections.add(letter)
                    mPositionForSection.add(i)
                }
                mSectionForPosition.add(mSections.size - 1)
            }
        }

        fun submitList(contactsList: List<String>){
            mContactsList = contactsList
            notifyDataSetChanged()
            updateSections()
        }

        override fun getItemCount(): Int {
            return mContactsList.size
        }

        override fun getItemViewType(position: Int): Int {
            return if ((mContactsList[position].length == 1)) 1 else 0
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            if (viewType == 0) {
                val inflater = LayoutInflater.from(mContext)
                val view = inflater.inflate(
                    R.layout.sample3_view_indexscroll_listview_item, parent, false
                )
                return ViewHolder(view, false)
            } else {
                return ViewHolder(Separator(mContext), true)
            }
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            if (holder.isSeparator) {
                holder.textView!!.layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
                )
            } else {
                if (position == 0) {
                    holder.imageView!!.setImageResource(R.drawable.indexscroll_group_icon)
                } else {
                    holder.imageView!!.setImageResource(R.drawable.indexscroll_item_icon)
                }
            }
            holder.textView!!.text = mContactsList[position]
        }

        override fun getSections(): Array<Any> {
            return mSections.toTypedArray()
        }

        override fun getPositionForSection(sectionIndex: Int): Int {
            return mPositionForSection[sectionIndex]
        }

        override fun getSectionForPosition(position: Int): Int {
            return mSectionForPosition[position]
        }

        inner class ViewHolder internal constructor(itemView: View, var isSeparator: Boolean) :
            RecyclerView.ViewHolder(itemView) {
            var imageView: ImageView? = null
            var textView: TextView? = null

            init {
                if (isSeparator) {
                    textView = itemView as TextView
                } else {
                    imageView = itemView.findViewById(R.id.indexscroll_list_item_icon)
                    textView = itemView.findViewById(R.id.indexscroll_list_item_text)
                }
            }
        }
    }