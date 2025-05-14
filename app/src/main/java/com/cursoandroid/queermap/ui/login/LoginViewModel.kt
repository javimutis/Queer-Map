package com.cursoandroid.queermap.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cursoandroid.queermap.domain.usecase.LoginWithEmailUseCase
import com.cursoandroid.queermap.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val loginWithEmailUseCase: LoginWithEmailUseCase,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState

    fun login(email: String, password: String, remember: Boolean, saveCredentials: (String, String) -> Unit) {
        viewModelScope.launch {
            _uiState.value = LoginUiState(isLoading = true)
            val result = loginWithEmailUseCase(email, password)
            if (result.isSuccess) {
                val user = result.getOrNull()
                if (remember) saveCredentials(email, password)
                user?.let {
                    val firestoreResult = authRepository.verifyUserInFirestore(it.uid)
                    if (firestoreResult.isSuccess) {
                        _uiState.value = LoginUiState(isSuccess = true)
                    } else {
                        _uiState.value = LoginUiState(errorMessage = "Usuario no existe en Firestore")
                    }
                }
            } else {
                _uiState.value = LoginUiState(errorMessage = result.exceptionOrNull()?.message)
            }
        }
    }
}
