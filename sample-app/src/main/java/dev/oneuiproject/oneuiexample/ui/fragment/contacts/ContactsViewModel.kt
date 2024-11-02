package dev.oneuiproject.oneuiexample.ui.fragment.contacts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.oneuiproject.oneui.delegates.AllSelectorState
import dev.oneuiproject.oneuiexample.data.ContactsRepo
import dev.oneuiproject.oneuiexample.data.ContactsSettings
import dev.oneuiproject.oneuiexample.ui.fragment.contacts.model.ContactsListItemUiModel
import dev.oneuiproject.oneuiexample.ui.fragment.contacts.util.toFilteredContactUiModelList
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted.Companion.Lazily
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ContactsUiState(
    val itemsList: List<ContactsListItemUiModel> = emptyList(),
    val query: String = "",
    val noItemText: String = "No contacts"
)

class ContactsViewModel (
    private val contactsRepo: ContactsRepo
): ViewModel() {

    private val _queryStateFlow = MutableStateFlow("")

    val contactsSettingsStateFlow = contactsRepo.contactsSettingsFlow
        .stateIn(viewModelScope, Lazily, ContactsSettings())

    fun toggleTextMode() = viewModelScope.launch {
        contactsRepo.setIndexScrollMode(!contactsSettingsStateFlow.value.isTextModeIndexScroll)
    }

    fun toggleAutoHide() = viewModelScope.launch {
        contactsRepo.setIndexScrollAutoHide(!contactsSettingsStateFlow.value.autoHideIndexScroll)
    }

    fun toggleKeepSearch() = viewModelScope.launch {
        contactsRepo.setKeepSearchOnActionMode(!contactsSettingsStateFlow.value.keepSearchOnActionMode)
    }

    val contactsListStateFlow: StateFlow<ContactsUiState> = combine(
        contactsRepo.contactsFlow, _queryStateFlow
    ) { list, query ->
        ContactsUiState(
            list.toFilteredContactUiModelList(query),
            query,
            if (query.isEmpty()) "No contacts" else "No results found."
        )
    }.stateIn(viewModelScope, Lazily, ContactsUiState())


    private var setFilterJob: Job? = null
    fun setQuery(query: String?) = viewModelScope.launch {
        setFilterJob?.cancel()
        setFilterJob = launch {
            delay(200)
            _queryStateFlow.value = query ?: ""
        }
    }

    val allSelectorStateFlow: MutableStateFlow <AllSelectorState> = MutableStateFlow(AllSelectorState())
 }



class ContactsViewModelFactory(private val contactsRepo: ContactsRepo) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ContactsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ContactsViewModel(contactsRepo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
