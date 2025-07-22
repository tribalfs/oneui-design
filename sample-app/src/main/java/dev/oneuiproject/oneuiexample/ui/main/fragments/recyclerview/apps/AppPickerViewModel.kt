package dev.oneuiproject.oneuiexample.ui.main.fragments.recyclerview.apps

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import dev.oneuiproject.oneuiexample.ui.main.fragments.recyclerview.apps.model.ListTypes
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

class AppPickerViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val isSelectLayoutState: StateFlow<Boolean> =
        savedStateHandle.getStateFlow(KEY_IS_SELECT_LAYOUT_MODE, false)
    private val listTypeState: StateFlow<ListTypes> =
        savedStateHandle.getStateFlow(KEY_LIST_TYPE, ListTypes.LIST_TYPE)

    var isSelectLayout
        get() = isSelectLayoutState.value
        set(value) {
            savedStateHandle[KEY_IS_SELECT_LAYOUT_MODE] = value
        }

    var listType
        get() = listTypeState.value
        set(value) {
            savedStateHandle[KEY_LIST_TYPE] = value
        }

    companion object {
        const val KEY_IS_SELECT_LAYOUT_MODE = "isSelectLayout"
        const val KEY_LIST_TYPE = "listType"
    }
}