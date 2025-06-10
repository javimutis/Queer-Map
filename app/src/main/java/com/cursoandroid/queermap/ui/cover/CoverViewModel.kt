// Queermap/ui/cover/CoverViewModel.kt
package com.cursoandroid.queermap.ui.cover

import android.util.Log // Importa Log para depuración
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.cursoandroid.queermap.util.EspressoIdlingResource // Importa EspressoIdlingResource

data class CoverUiState(
    val showTitle: Boolean = false,
    val navigateToLogin: Boolean = false,
    val navigateToSignUp: Boolean = false)


class CoverViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(CoverUiState())
    val uiState: StateFlow<CoverUiState> = _uiState

    init {
        showTitleWithDelay()
    }

    private fun showTitleWithDelay() {
        // Incrementa el IdlingResource antes de iniciar la operación asíncrona
        EspressoIdlingResource.increment()
        Log.d("IdlingResourceLog", "CoverViewModel: INCREMENTADO (delay iniciado). ¿Ahora inactivo? ${EspressoIdlingResource.countingIdlingResource.isIdleNow}")

        viewModelScope.launch {
            delay(1300)
            _uiState.value = _uiState.value.copy(showTitle = true)
            // Decrementa el IdlingResource cuando la operación asíncrona ha terminado
            EspressoIdlingResource.decrement()
            Log.d("IdlingResourceLog", "CoverViewModel: DECREMENTADO (delay finalizado). ¿Ahora inactivo? ${EspressoIdlingResource.countingIdlingResource.isIdleNow}")
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