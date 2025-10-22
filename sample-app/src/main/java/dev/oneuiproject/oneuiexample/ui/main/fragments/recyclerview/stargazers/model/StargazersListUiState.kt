package dev.oneuiproject.oneuiexample.ui.main.fragments.recyclerview.stargazers.model

import dev.oneuiproject.oneuiexample.data.stargazers.model.FetchState

data class StargazersListUiState(
    val itemsList: List<StargazersListItemUiModel> = emptyList(),
    val query: String = "",
    val noItemText: String = "No contacts",
    val fetchStatus: FetchState = FetchState.INITED
)