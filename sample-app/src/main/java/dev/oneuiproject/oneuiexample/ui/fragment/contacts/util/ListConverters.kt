package dev.oneuiproject.oneuiexample.ui.fragment.contacts.util

import androidx.core.text.isDigitsOnly
import dev.oneuiproject.oneui.ktx.containsAllTokensOf
import dev.oneuiproject.oneuiexample.data.Contact
import dev.oneuiproject.oneuiexample.ui.fragment.contacts.model.ContactsListItemUiModel

fun List<ContactsListItemUiModel>.toStringsList(): List<String> {
    return map { model ->
        when(model){
            is ContactsListItemUiModel.GroupItem -> model.groupName
            is ContactsListItemUiModel.SeparatorItem -> model.indexText
            is ContactsListItemUiModel.ContactItem -> model.contact.name
        }
    }
}

fun List<ContactsListItemUiModel>.toIndexCharsArray(): Array<String> {
    return filterIsInstance<ContactsListItemUiModel.SeparatorItem>()
        .map {
            it.indexText
        }
        .toTypedArray()
}

fun List<Contact>.toFilteredContactUiModelList(query: String): List<ContactsListItemUiModel> {
    val list = mutableListOf<ContactsListItemUiModel>()

    if ("Groups".containsAllTokensOf(query)) {
        list.add(ContactsListItemUiModel.GroupItem("Groups"))
    }

    var previousChar: String? = null
    for (i in indices) {
        val item = this[i]
        val name = item.name
        val showItem = "$name ${item.number}".containsAllTokensOf(query)
        if (showItem) {
            val char = name[0].toString().run { if (this.isDigitsOnly()) "#" else this.uppercase() }
            if (char != previousChar) {
                list.add(ContactsListItemUiModel.SeparatorItem(char))
                previousChar = char
            }
            list.add(ContactsListItemUiModel.ContactItem(item))
        }
    }
    return list
}