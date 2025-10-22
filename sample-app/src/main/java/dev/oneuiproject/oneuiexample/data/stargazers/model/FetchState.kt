package dev.oneuiproject.oneuiexample.data.stargazers.model

enum class FetchState{
    NOT_INIT,
    INITING,
    INIT_ERROR,
    INITED,
    REFRESHING,
    REFRESH_ERROR,
    REFRESHED
}

