package dev.oneuiproject.oneuiexample.ui.fragment.contacts.util

import dev.oneuiproject.oneui.layout.ToolbarLayout
import dev.oneuiproject.oneuiexample.data.ActionModeSearch

fun ActionModeSearch.toSearchOnActionMode(listener: ToolbarLayout.SearchModeListener): ToolbarLayout.SearchOnActionMode =
    when(this){
        ActionModeSearch.DISMISS -> ToolbarLayout.SearchOnActionMode.Dismiss
        ActionModeSearch.NO_DISMISS -> ToolbarLayout.SearchOnActionMode.NoDismiss
        ActionModeSearch.CONCURRENT -> ToolbarLayout.SearchOnActionMode.Concurrent(listener)
    }