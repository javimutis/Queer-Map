package com.cursoandroid.queermap.ui.forgotpassword

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.cursoandroid.queermap.R
import com.cursoandroid.queermap.databinding.FragmentForgotPasswordBinding
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ForgotPasswordFragment : Fragment(R.layout.fragment_forgot_password) {

    private var _binding: FragmentForgotPasswordBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ForgotPasswordViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentForgotPasswordBinding.bind(view)
        setupListeners()
        observeUiState()
        observeEvents() // Nuevo observador de eventos
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupListeners() {
        binding.btnResetPassword.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            // La validación ahora la maneja el ViewModel y si no es válida,
            // el ViewModel emitirá un evento ShowMessage.
            viewModel.sendPasswordReset(email)
        }

        binding.btnCancel.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.btnResetPassword.isEnabled = !state.isLoading
                    binding.btnCancel.isEnabled = !state.isLoading // Deshabilitar cancelar también
                    binding.etEmail.isEnabled = !state.isLoading // Deshabilitar campo
                    binding.progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE
                }
            }
        }
    }

    private fun observeEvents() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.events.collect { event ->
                    when (event) {
                        is ForgotPasswordEvent.ShowMessage -> showSnackbar(event.message)
                        is ForgotPasswordEvent.NavigateBack -> findNavController().popBackStack()
                    }
                }
            }
        }
    }

    private fun showSnackbar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show() // Usar LENGTH_LONG para mensajes importantes
    }
}