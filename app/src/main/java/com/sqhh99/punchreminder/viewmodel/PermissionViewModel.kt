package com.sqhh99.punchreminder.viewmodel

import androidx.lifecycle.ViewModel
import com.sqhh99.punchreminder.domain.permission.PermissionDiagnostics
import com.sqhh99.punchreminder.domain.permission.PermissionItem
import com.sqhh99.punchreminder.system.permission.PermissionInspector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PermissionUiState(
    val items: List<PermissionItem> = emptyList(),
    val allGranted: Boolean = false,
)

/**
 * 权限诊断页状态（实现方案 §11）。每次进入/返回页面时 [refresh] 重新读取真实状态。
 */
class PermissionViewModel(
    private val inspector: PermissionInspector,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PermissionUiState())
    val uiState: StateFlow<PermissionUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        val items = inspector.inspect()
        _uiState.value = PermissionUiState(items = items, allGranted = PermissionDiagnostics.allGranted(items))
    }
}
