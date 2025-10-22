package dev.oneuiproject.oneuiexample.data.stargazers.model

data class StargazersSettings(
    val isTextModeIndexScroll: Boolean = false,
    val autoHideIndexScroll: Boolean = true,
    val searchOnActionMode: ActionModeSearch = ActionModeSearch.DISMISS,
    val actionModeShowCancel: Boolean = false,
    val lastRefresh: Long = 0L,
)