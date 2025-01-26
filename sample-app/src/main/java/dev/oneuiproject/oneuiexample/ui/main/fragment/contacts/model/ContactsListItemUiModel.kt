package dev.oneuiproject.oneuiexample.ui.main.fragment.contacts.model

import dev.oneuiproject.oneuiexample.data.Contact

sealed class ContactsListItemUiModel{
    data class GroupItem(val groupName: String) : ContactsListItemUiModel(){
        companion object{ const val VIEW_TYPE = -1 }
    }

    data class ContactItem(val contact: Contact) : ContactsListItemUiModel(){
        companion object{ const val VIEW_TYPE = 0 }
    }
    data class SeparatorItem(val indexText: String) : ContactsListItemUiModel(){
        companion object{ const val VIEW_TYPE = 1 }
    }

    fun toStableId(): Long{
        return when(this){
            is GroupItem -> groupName.hashCode().toLong()
            is ContactItem -> contact.hashCode().toLong()
            is SeparatorItem -> indexText.hashCode().toLong()
        }
    }
}