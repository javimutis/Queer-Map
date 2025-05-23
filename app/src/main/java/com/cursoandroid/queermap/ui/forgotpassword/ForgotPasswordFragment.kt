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

    // Ciclo de vida del fragmento
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentForgotPasswordBinding.bind(view)
        setupListeners()
        observeUiState()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // Manejo de interacciones de UI
    private fun setupListeners() {
        binding.btnResetPassword.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            if (ForgotPasswordValidator.isValidEmail(email)) {
                viewModel.sendPasswordReset(email)
            } else {
                showSnackbar("Ingrese un correo válido")
            }
        }

        binding.btnCancel.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    // Observación de estado y eventos
    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.btnResetPassword.isEnabled = !state.isLoading
                    binding.progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE

                    state.message?.let {
                        showSnackbar(it)
                        viewModel.clearMessage()
                    }

                    if (state.isSuccess) {
                        findNavController().popBackStack()
                    }
                }
            }
        }
    }

    // Utilidades
    private fun showSnackbar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }
}