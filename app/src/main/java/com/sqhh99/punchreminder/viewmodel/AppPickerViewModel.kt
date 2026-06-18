package com.sqhh99.punchreminder.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sqhh99.punchreminder.domain.model.InstalledApp
import com.sqhh99.punchreminder.domain.usecase.InstalledAppFilter
import com.sqhh99.punchreminder.system.launcher.InstalledAppProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class AppPickerUiState(
    val apps: List<InstalledApp> = emptyList(),
    val query: String = "",
    val loading: Boolean = true,
)

class AppPickerViewModel(
    private val provider: InstalledAppProvider,
    private val ownPackage: String,
    private val filter: InstalledAppFilter = InstalledAppFilter(),
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppPickerUiState())
    val uiState: StateFlow<AppPickerUiState> = _uiState.asStateFlow()

    private var rawApps: List<InstalledApp> = emptyList()

    init {
        viewModelScope.launch {
            rawApps = withContext(Dispatchers.Default) { provider.loadLaunchableApps() }
            applyFilter()
        }
    }

    fun setQuery(query: String) {
        _uiState.update { it.copy(query = query) }
        applyFilter()
    }

    private fun applyFilter() {
        val query = _uiState.value.query
        _uiState.update {
            it.copy(apps = filter.filter(rawApps, ownPackage, query), loading = false)
        }
    }
}
