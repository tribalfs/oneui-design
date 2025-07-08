package dev.oneuiproject.oneuiexample.ui.main.fragments.recyclerview.icons

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.oneuiproject.oneui.delegates.AllSelectorState
import dev.oneuiproject.oneuiexample.data.IconsRepo
import dev.oneuiproject.oneuiexample.ui.main.fragments.recyclerview.icons.util.toFilteredIconsUiModelList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class IconListItemUiModel(
    val id: Int,
    val name: String
)

data class IconsUiState(
    val itemsList: List<IconListItemUiModel> = emptyList(),
    val query: String = "",
    val noItemText: String = "No icons"
)


@HiltViewModel
class IconsViewModel @Inject constructor(
    @param:ApplicationContext private val appContext: Context,
    iconsRepo: IconsRepo,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _queryStateFlow = MutableStateFlow("")

    @OptIn(FlowPreview::class)
    val iconsListStateFlow: StateFlow<IconsUiState?> = combine(
        iconsRepo.iconsFlow, _queryStateFlow.debounce(300)
    ) { list, query ->
        IconsUiState(
            itemsList = list.toFilteredIconsUiModelList(appContext, query),
            query = query,
            noItemText = if (query.isEmpty()) "No icons" else "No results found."
        )
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, WhileSubscribed(3_000), null)


    val allSelectorStateFlow = MutableStateFlow(AllSelectorState())
    private val isActionModeStateFlow: StateFlow<Boolean> = savedStateHandle.getStateFlow(KEY_IS_ACTION_MODE, false)

    var isActionMode
        get() = isActionModeStateFlow.value
        set(value) { savedStateHandle[KEY_IS_ACTION_MODE] = value }

    var initialSelectedIds
        get() = savedStateHandle.get<Array<Int>>(KEY_ACTION_MODE_SELECTED_IDS)
        set(value) {
            savedStateHandle[KEY_ACTION_MODE_SELECTED_IDS] = value
        }

    fun setQuery(query: String?){ _queryStateFlow.value = query ?: "" }

    private val isSearchModeStateFlow: StateFlow<Boolean> =
        savedStateHandle.getStateFlow(KEY_IS_SEARCH_MODE, false)

    var isSearchMode
        get() = isSearchModeStateFlow.value
        set(value) {
            savedStateHandle[KEY_IS_SEARCH_MODE] = value
        }

    val showFabStateFlow =
        combine(isSearchModeStateFlow, isActionModeStateFlow) { isSearchMode, isActionMode ->
            !isSearchMode && !isActionMode
        }.stateIn(viewModelScope, WhileSubscribed(5_000), false)


    companion object {
        const val KEY_IS_ACTION_MODE = "isActionMode"
        const val KEY_ACTION_MODE_SELECTED_IDS = "selectedIds"
        const val KEY_IS_SEARCH_MODE = "isSearchMode"
    }
}

