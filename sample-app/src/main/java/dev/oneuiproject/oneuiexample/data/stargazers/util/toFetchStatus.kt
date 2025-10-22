package dev.oneuiproject.oneuiexample.data.stargazers.util

import dev.oneuiproject.oneuiexample.data.stargazers.model.FetchState

fun Int?.toFetchStatus(): FetchState = if (this != null){
        FetchState.entries[this]
    } else FetchState.NOT_INIT