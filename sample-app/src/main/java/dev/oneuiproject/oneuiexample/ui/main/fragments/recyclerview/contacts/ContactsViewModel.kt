package dev.oneuiproject.oneuiexample.ui.main.fragments.recyclerview.contacts

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.oneuiproject.oneui.delegates.AllSelectorState
import dev.oneuiproject.oneuiexample.data.ActionModeSearch
import dev.oneuiproject.oneuiexample.data.ContactsRepo
import dev.oneuiproject.oneuiexample.data.ContactsSettings
import dev.oneuiproject.oneuiexample.ui.main.fragments.recyclerview.contacts.model.ContactsListItemUiModel
import dev.oneuiproject.oneuiexample.ui.main.fragments.recyclerview.contacts.util.toFilteredContactUiModelList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ContactsUiState(
    val itemsList: List<ContactsListItemUiModel> = emptyList(),
    val query: String = "",
    val noItemText: String = "No contacts"
)

@HiltViewModel
class ContactsViewModel @Inject constructor(
    private val contactsRepo: ContactsRepo,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _queryStateFlow = MutableStateFlow("")

    val contactsSettingsStateFlow = contactsRepo.contactsSettingsFlow
        .stateIn(viewModelScope, WhileSubscribed(5_000), ContactsSettings())

    fun setTextMode(textMode: Boolean) = viewModelScope.launch {
        contactsRepo.setIndexScrollMode(textMode)
    }

    fun setAutoHide(autoHide: Boolean) = viewModelScope.launch {
        contactsRepo.setIndexScrollAutoHide(autoHide)
    }

    fun setActionModeSearchMode(searchMode: ActionModeSearch) = viewModelScope.launch {
        contactsRepo.setActionModeSearchMode(searchMode)
    }

    fun setShowCancel(showCancel: Boolean) = viewModelScope.launch {
        contactsRepo.setShowCancel(showCancel)
    }

    @OptIn(FlowPreview::class)
    val contactsListStateFlow: StateFlow<ContactsUiState> = combine(
        contactsRepo.contactsFlow,
        _queryStateFlow/*.debounce(300)*/
    ) { list, query ->
        ContactsUiState(
            list.toFilteredContactUiModelList(query),
            query,
            if (query.isEmpty()) "No contacts" else "No results found."
        )
    }
        .flowOn(Dispatchers.IO)
        .stateIn(viewModelScope, WhileSubscribed(5_000), ContactsUiState())


    val allSelectorStateFlow = MutableStateFlow(AllSelectorState())
    private val isActionModeStateFlow: StateFlow<Boolean> =
        savedStateHandle.getStateFlow(KEY_IS_ACTION_MODE, false)

    var isActionMode
        get() = isActionModeStateFlow.value
        set(value) {
            savedStateHandle[KEY_IS_ACTION_MODE] = value
        }

    var initialSelectedIds
        get() = savedStateHandle.get<Array<Long>>(KEY_ACTION_MODE_SELECTED_IDS)
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

