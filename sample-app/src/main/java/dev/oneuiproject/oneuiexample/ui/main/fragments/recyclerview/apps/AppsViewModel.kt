package dev.oneuiproject.oneuiexample.ui.main.fragments.recyclerview.apps

import androidx.apppickerview.widget.AppPickerView.TYPE_LIST
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.oneuiproject.oneuiexample.data.AppsRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject


data class AppsScreenState(
    val listTypeSelected: Int = TYPE_LIST,
    val appList: ArrayList<String> = ArrayList(),
    val showSystem: Boolean = false,
    val systemAppsCount: Int = 0,
    val isLoading:Boolean = true
)

@HiltViewModel
class AppsViewModel  @Inject constructor(
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
            SharingStarted.WhileSubscribed(3_000),
            AppsScreenState()
        )

    fun toggleShowSystem() = viewModelScope.launch {
        val current =  appPickerUiStateFlow.value.showSystem
        appsRepo.setShowSystemApps(!current)
    }
}