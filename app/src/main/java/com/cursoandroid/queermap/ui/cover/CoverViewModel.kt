package com.cursoandroid.queermap.ui.cover

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cursoandroid.queermap.util.IdlingResourceProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CoverUiState(
    val showTitle: Boolean = false,
    val navigateToLogin: Boolean = false,
    val navigateToSignUp: Boolean = false
)

@HiltViewModel
class CoverViewModel @Inject constructor(
    private val idlingResourceProvider: IdlingResourceProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(CoverUiState())
    val uiState: StateFlow<CoverUiState> = _uiState

    init {
        showTitleWithDelay()
    }

    private fun showTitleWithDelay() {
        idlingResourceProvider.increment()

        viewModelScope.launch {
            delay(1300)
            _uiState.value = _uiState.value.copy(showTitle = true)
            idlingResourceProvider.decrement()
        }
    }

    fun onLoginClicked() {
        _uiState.value = _uiState.value.copy(navigateToLogin = true)
    }

    fun onSignUpClicked() {
        _uiState.value = _uiState.value.copy(navigateToSignUp = true)
    }

    fun onNavigated() {
        _uiState.value = _uiState.value.copy(navigateToLogin = false, navigateToSignUp = false)
    }
}