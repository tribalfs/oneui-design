package dev.oneuiproject.oneuiexample.ui.main.fragments.recyclerview.icons.util

import android.content.Context
import dev.oneuiproject.oneui.ktx.containsAllTokensOf
import dev.oneuiproject.oneuiexample.ui.main.fragments.recyclerview.icons.IconListItemUiModel

fun List<Int>.toFilteredIconsUiModelList(
    context: Context,
    query: String
): List<IconListItemUiModel> =
    map {
        IconListItemUiModel(
            it,
            context.getResources().getResourceEntryName(it)
        )
    }
        .filter { query == "" || it.name.containsAllTokensOf(query) }
