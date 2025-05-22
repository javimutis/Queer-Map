package com.cursoandroid.queermap.ui.welcome

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class WelcomeViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(false)
    val uiState: StateFlow<Boolean> = _uiState

    init {
        viewModelScope.launch {
            delay(2000)
            _uiState.value = true
        }
    }

    fun onNavigated() {
        _uiState.value = false
    }
}
