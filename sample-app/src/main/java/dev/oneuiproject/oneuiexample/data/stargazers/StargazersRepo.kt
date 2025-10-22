package dev.oneuiproject.oneuiexample.data.stargazers

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.oneuiproject.oneuiexample.data.sampleAppPreferences
import dev.oneuiproject.oneuiexample.data.stargazers.db.StargazersDB
import dev.oneuiproject.oneuiexample.data.stargazers.model.ActionModeSearch
import dev.oneuiproject.oneuiexample.data.stargazers.model.FetchState
import dev.oneuiproject.oneuiexample.data.stargazers.model.RefreshResult
import dev.oneuiproject.oneuiexample.data.stargazers.model.Stargazer
import dev.oneuiproject.oneuiexample.data.stargazers.model.StargazersSettings
import dev.oneuiproject.oneuiexample.data.stargazers.network.NetworkDataSource
import dev.oneuiproject.oneuiexample.data.stargazers.util.toFetchStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext


class StargazersRepo @Inject constructor(
    @ApplicationContext appContext: Context,
): CoroutineScope {

    override val coroutineContext: CoroutineContext = Dispatchers.IO + SupervisorJob()
    private val dataStore: DataStore<Preferences> = appContext.sampleAppPreferences
    private val database = StargazersDB.getDatabase(appContext, this, repoProvider =  { this })

    val stargazersFlow: Flow<List<Stargazer>> = database.stargazerDao().getAllStargazers()

    private val refreshMutex = Mutex()

    suspend fun refreshStargazers(callback: ((result: RefreshResult) -> Unit)? = null) =
        withContext(Dispatchers.IO) {
            if (!refreshMutex.tryLock()) {
                callback?.invoke(RefreshResult.UpdateRunning)
                return@withContext
            }

            setOnStartFetchStatus()
            NetworkDataSource.fetchStargazers()
                .onSuccess {
                    database.stargazerDao().replaceAll(it)
                    updateLastRefresh(System.currentTimeMillis())
                    setOnFinishFetchStatus(true)
                    callback?.invoke(RefreshResult.Updated)
                }
                .onFailure {
                    setOnFinishFetchStatus(false)
                    when (it) {
                        is HttpException -> {
                            val result = when (val code = it.code()) {
                                401 -> RefreshResult.UnauthorizedError
                                403 -> RefreshResult.ForbiddenError
                                404 -> RefreshResult.NotFoundError
                                else -> RefreshResult.OtherHttpException(code, it.message())
                            }
                            callback?.invoke(result)
                        }
                        else -> {
                            callback?.invoke(RefreshResult.OtherException(it))
                        }
                    }
                }

            refreshMutex.unlock()
        }

    private val _fetchStatusFlow = MutableStateFlow(FetchState.NOT_INIT)
    val fetchStatusFlow: StateFlow<FetchState> = _fetchStatusFlow

    init {
        launch {
            val initialStatus = dataStore.data.map {
                it[PREF_STARGAZERS_INIT_FETCH_STATE].toFetchStatus()
            }.first()
            _fetchStatusFlow.value = initialStatus
        }
    }

    private suspend fun setOnStartFetchStatus() {
        val currentStatus = _fetchStatusFlow.value
        when (currentStatus) {
            FetchState.NOT_INIT, FetchState.INIT_ERROR -> setFetchStatus(FetchState.INITING)
            FetchState.INITED, FetchState.REFRESH_ERROR, FetchState.REFRESHED -> setFetchStatus(
                FetchState.REFRESHING
            )
            FetchState.INITING, FetchState.REFRESHING -> Unit
        }
    }

    private suspend fun setOnFinishFetchStatus(isSuccess: Boolean) {
        val currentStatus = _fetchStatusFlow.value
        when (currentStatus) {
            FetchState.REFRESHING -> setFetchStatus(if (isSuccess) FetchState.REFRESHED else FetchState.REFRESH_ERROR)
            FetchState.INITING -> setFetchStatus(if (isSuccess) FetchState.INITED else FetchState.INIT_ERROR)
            else -> Unit//we're not expecting this
        }
    }

    suspend fun setFetchStatus(state: FetchState) {
        _fetchStatusFlow.value = state
        dataStore.edit{
            it[PREF_STARGAZERS_INIT_FETCH_STATE] = state.ordinal
        }
    }

    suspend fun updateLastRefresh(timeMillis: Long) {
        dataStore.edit {
            it[PREF_STARGAZERS_LAST_REFRESH] = timeMillis
        }
    }

    suspend fun setIndexScrollMode(isTextMode: Boolean) {
        dataStore.edit {
            it[PREF_STARGAZERS_TEXT_MODE] = isTextMode
        }
    }

    suspend fun setIndexScrollAutoHide(autoHide: Boolean) {
        dataStore.edit {
            it[PREF_STARGAZERS_INDEXSCROLL_AUTO_HIDE] = autoHide
        }
    }

    suspend fun setActionModeSearchMode(searchMode: ActionModeSearch) {
        dataStore.edit {
            it[PREF_STARGAZERS_ACTIONMODE_KEEP_SEARCH] = searchMode.ordinal
        }
    }

    suspend fun setShowCancel(showCancel: Boolean) {
        dataStore.edit {
            it[PREF_STARGAZERS_ACTIONMODE_SHOW_CANCEL] = showCancel
        }
    }

    val stargazersSettingsFlow: Flow<StargazersSettings> = dataStore.data.map {
        StargazersSettings(
            it[PREF_STARGAZERS_TEXT_MODE] ?: false,
            it[PREF_STARGAZERS_INDEXSCROLL_AUTO_HIDE] ?: true,
            ActionModeSearch.entries[it[PREF_STARGAZERS_ACTIONMODE_KEEP_SEARCH] ?: 0],
            it[PREF_STARGAZERS_ACTIONMODE_SHOW_CANCEL] ?: false,
            lastRefresh = it[PREF_STARGAZERS_LAST_REFRESH] ?: 0,
        )
    }

    suspend fun getStargazersById(ids: IntArray) = database.stargazerDao().getStargazersById(ids)

    companion object {
        private val PREF_STARGAZERS_ACTIONMODE_KEEP_SEARCH = intPreferencesKey("actionModeSearch")
        private val PREF_STARGAZERS_ACTIONMODE_SHOW_CANCEL = booleanPreferencesKey("actionModeShowCancel")
        private val PREF_STARGAZERS_TEXT_MODE = booleanPreferencesKey("indexScrollTextMode")
        private val PREF_STARGAZERS_INDEXSCROLL_AUTO_HIDE = booleanPreferencesKey("indexScrollAutoHide")
        private val PREF_STARGAZERS_LAST_REFRESH = longPreferencesKey("lastRefresh")
        private val PREF_STARGAZERS_INIT_FETCH_STATE = intPreferencesKey("initFetch")

    }
}
