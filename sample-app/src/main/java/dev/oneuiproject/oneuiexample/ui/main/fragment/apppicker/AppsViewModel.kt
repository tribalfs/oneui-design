package dev.oneuiproject.oneuiexample.ui.main.fragment.apppicker

import androidx.apppickerview.widget.AppPickerView.TYPE_LIST
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.oneuiproject.oneuiexample.data.AppsRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch


data class AppsScreenState(
    val listTypeSelected: Int = TYPE_LIST,
    val appList: ArrayList<String> = ArrayList(),
    val showSystem: Boolean = false,
    val systemAppsCount: Int = 0,
    val isLoading:Boolean = true
)

class AppsViewModel (
    private val appsRepo: AppsRepo
): ViewModel() {

    val appPickerUiStateFlow = combine(
        appsRepo.appsFlow,
        appsRepo.appPreferenceFlow
    ){ apps, prefs ->
        AppsScreenState(
            listTypeSelected = prefs.appPickerType,
            appList = ArrayList(
                apps
                    .filter {
                        prefs.showSystem || !it.isSystemApp
                    }.map {
                        it.packageName
                    }
            ),
            showSystem = prefs.showSystem,
            systemAppsCount = apps.count { it.isSystemApp },
            isLoading = false
        )
    }
        .flowOn(Dispatchers.Default)
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(),
            AppsScreenState()
        )

    fun toggleShowSystem() = viewModelScope.launch {
        val current =  appPickerUiStateFlow.value.showSystem
        appsRepo.setShowSystemApps(!current)
    }
}

class AppsViewModelFactory(private val appsRepo: AppsRepo) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AppsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AppsViewModel(appsRepo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
