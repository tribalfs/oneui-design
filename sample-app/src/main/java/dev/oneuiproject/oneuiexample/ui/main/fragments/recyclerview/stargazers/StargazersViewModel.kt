package dev.oneuiproject.oneuiexample.ui.main.fragments.recyclerview.stargazers

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.oneuiproject.oneui.layout.ToolbarLayout
import dev.oneuiproject.oneuiexample.data.stargazers.StargazersRepo
import dev.oneuiproject.oneuiexample.data.stargazers.model.ActionModeSearch
import dev.oneuiproject.oneuiexample.data.stargazers.model.FetchState
import dev.oneuiproject.oneuiexample.data.stargazers.model.RefreshResult
import dev.oneuiproject.oneuiexample.data.stargazers.model.StargazersSettings
import dev.oneuiproject.oneuiexample.ui.main.fragments.recyclerview.stargazers.model.StargazersListUiState
import dev.oneuiproject.oneuiexample.ui.main.fragments.recyclerview.stargazers.util.isOnline
import dev.oneuiproject.oneuiexample.ui.main.fragments.recyclerview.stargazers.util.toFilteredStargazerUiModelList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class StargazersViewModel @Inject constructor(
    @param:ApplicationContext private val appCtx: Context,
    private val stargazersRepo: StargazersRepo,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _queryStateFlow = MutableStateFlow("")

    private val _stargazersListScreenStateFlow = MutableStateFlow(StargazersListUiState())
    val stargazersListScreenStateFlow = _stargazersListScreenStateFlow.asStateFlow()

    private val _userMessage: MutableStateFlow<String?> = MutableStateFlow(null)
    val userMessage: StateFlow<String?> = _userMessage

    fun setUserMessageShown() = _userMessage.update { null }

    init {
        viewModelScope.launch {
            launch {
                combine(
                    stargazersRepo.stargazersFlow,
                    _queryStateFlow,
                    stargazersRepo.fetchStatusFlow
                ) { stargazers, query, fetchStatus ->
                    val itemsList = stargazers.toFilteredStargazerUiModelList(query)
                    val noItemText = getNoItemText(fetchStatus, query)
                    StargazersListUiState(
                        itemsList = itemsList,
                        query = query,
                        noItemText = noItemText,
                        fetchStatus = fetchStatus
                    )
                }.collectLatest { uiState ->
                    _stargazersListScreenStateFlow.value = uiState
                }
            }
        }
    }


    private fun getNoItemText(fetchState: FetchState, query: String): String{
        return when (fetchState) {
            FetchState.INITING -> "Loading stargazers..."
            FetchState.INIT_ERROR -> "Error loading stargazers."
            //These will only be visible when rv is empty.
            FetchState.INITED, FetchState.REFRESHED -> if (query.isEmpty()) "No stargazers yet." else "No results found."
            FetchState.NOT_INIT, FetchState.REFRESHING, FetchState.REFRESH_ERROR -> ""
        }
    }

    fun refreshStargazers(notifyResult: Boolean = true) = viewModelScope.launch {
        if (!isOnline(appCtx)) {
            _userMessage.value = "No internet connection detected."
            return@launch
        }

        stargazersRepo.refreshStargazers sr@{ result ->
            if (!notifyResult) return@sr
            when (result){
                RefreshResult.UpdateRunning -> {
                    _userMessage.value = "Stargazer's already refreshing."
                }
                is RefreshResult.OtherException -> {
                    _userMessage.value = result.exception.message ?: "Error fetching stargazers."
                }
                RefreshResult.Updated -> {
                    _userMessage.value = "Latest stargazers data successfully fetched!"
                }
                RefreshResult.ForbiddenError -> {
                    _userMessage.value = "Rate limit exceeded! Try again later."
                }
                RefreshResult.UnauthorizedError -> {
                    _userMessage.value = "Bad credentials."
                }
                RefreshResult.NotFoundError -> {
                    _userMessage.value = "One or more repositories is missing."
                }
                is RefreshResult.OtherHttpException -> {
                    _userMessage.value = "HttpException (${result.code}: ${result.message})"
                }
            }
        }
    }

    val stargazersSettingsStateFlow = stargazersRepo.stargazersSettingsFlow
        .stateIn(viewModelScope, WhileSubscribed(5_000), StargazersSettings())

    fun setTextMode(textMode: Boolean) = viewModelScope.launch {
        stargazersRepo.setIndexScrollMode(textMode)
    }

    fun setAutoHide(autoHide: Boolean) = viewModelScope.launch {
        stargazersRepo.setIndexScrollAutoHide(autoHide)
    }

    fun setActionModeSearchMode(searchMode: ActionModeSearch) = viewModelScope.launch {
        stargazersRepo.setActionModeSearchMode(searchMode)
    }

    fun setShowCancel(showCancel: Boolean) = viewModelScope.launch {
        stargazersRepo.setShowCancel(showCancel)
    }

    val allSelectorStateFlow = MutableStateFlow(ToolbarLayout.AllSelectorState())
    private val isActionModeStateFlow: StateFlow<Boolean> =
        savedStateHandle.getStateFlow(KEY_IS_ACTION_MODE, false)

    var isActionMode
        get() = isActionModeStateFlow.value
        set(value) {
            savedStateHandle[KEY_IS_ACTION_MODE] = value
        }

    var initialSelectedIds
        get() = savedStateHandle.get<Set<Long>>(KEY_ACTION_MODE_SELECTED_IDS)
        set(value) {
            savedStateHandle[KEY_ACTION_MODE_SELECTED_IDS] = value
        }


    fun setQuery(query: String?) { _queryStateFlow.value = query ?: "" }

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

