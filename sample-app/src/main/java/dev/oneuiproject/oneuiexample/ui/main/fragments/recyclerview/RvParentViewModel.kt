package dev.oneuiproject.oneuiexample.ui.main.fragments.recyclerview

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class RvParentViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    val isTabLayoutEnabledStateFlow: StateFlow<Boolean> =
        savedStateHandle.getStateFlow(KEY_TAB_ENABLED, true)

    var isTabLayoutEnabled
        get() = isTabLayoutEnabledStateFlow.value
        set(value) { savedStateHandle[KEY_TAB_ENABLED] = value }

    companion object {
        const val KEY_TAB_ENABLED = "tabEnabled"
    }
}

