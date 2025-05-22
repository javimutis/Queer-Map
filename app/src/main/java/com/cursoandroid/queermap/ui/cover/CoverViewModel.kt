package com.cursoandroid.queermap.ui.cover

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class CoverUiState(
    val showTitle: Boolean = false,
    val navigateToLogin: Boolean = false
)

class CoverViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(CoverUiState())
    val uiState: StateFlow<CoverUiState> = _uiState

    init {
        viewModelScope.launch {
            delay(1300)
            _uiState.value = _uiState.value.copy(showTitle = true)
        }
    }

    fun onLoginClicked() {
        _uiState.value = _uiState.value.copy(navigateToLogin = true)
    }

    fun onNavigated() {
        _uiState.value = _uiState.value.copy(navigateToLogin = false)
    }
}
