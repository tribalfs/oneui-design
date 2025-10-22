package dev.oneuiproject.oneuiexample.ui.main.fragments.recyclerview.stargazers.util

import androidx.core.text.isDigitsOnly
import dev.oneuiproject.oneui.ktx.containsAllTokensOf
import dev.oneuiproject.oneuiexample.data.stargazers.model.Stargazer
import dev.oneuiproject.oneuiexample.ui.main.fragments.recyclerview.stargazers.model.StargazersListItemUiModel


fun List<Stargazer>.toFilteredStargazerUiModelList(query: String): List<StargazersListItemUiModel> {
    val list = mutableListOf<StargazersListItemUiModel>()

    var previousChar: String? = null
    for (i in indices) {
        val item = this[i]
        val showItem = item.getSearchableString().containsAllTokensOf(query)
        if (showItem) {
            val char = item.getDisplayName()[0].toString().run { if (this.isDigitsOnly()) "#" else this.uppercase() }
            if (char != previousChar) {
                list.add(StargazersListItemUiModel.SeparatorItem(char))
                previousChar = char
            }
            list.add(StargazersListItemUiModel.StargazerItem(item))
        }
    }
    return list
}


fun List<StargazersListItemUiModel>.toStringsList(): List<String> {
    return map { model ->
        when(model){
            is StargazersListItemUiModel.GroupItem -> model.groupName
            is StargazersListItemUiModel.SeparatorItem -> model.indexText
            is StargazersListItemUiModel.StargazerItem -> model.stargazer.getDisplayName().run{
                if (!first().isLetterOrDigit()) "#" else this}
        }
    }
}

fun List<StargazersListItemUiModel>.toIndexCharsArray(): Array<String> {
    return filterIsInstance<StargazersListItemUiModel.SeparatorItem>()
        .map {
            it.indexText
        }
        .toTypedArray()
}
