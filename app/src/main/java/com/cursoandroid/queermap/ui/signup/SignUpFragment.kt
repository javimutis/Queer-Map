package com.cursoandroid.queermap.ui.signup

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.cursoandroid.queermap.R
import com.cursoandroid.queermap.databinding.FragmentSignupBinding
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SignUpFragment : Fragment() {

    private var _binding: FragmentSignupBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SignUpViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSignupBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupListeners()
        observeState()
        observeEvents()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupListeners() {
        binding.btnRegister.setOnClickListener {
            val email = binding.etEmail.text.toString()
            val password = binding.etPassword.text.toString()
            val confirmPassword = binding.etRepeatPassword.text.toString()
            viewModel.onSignupClicked(email, password, confirmPassword)
        }

        binding.ivBack.setOnClickListener {
            viewModel.onBackPressed()
        }
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                binding.progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE

                if (state.isEmailInvalid) showSnackbar("Por favor ingresa un email válido")
                if (state.isPasswordInvalid) showSnackbar("La contraseña debe tener al menos 6 caracteres")
                if (state.doPasswordsMismatch) showSnackbar("Las contraseñas no coinciden")
                state.errorMessage?.let { showSnackbar(it) }

                state.email?.let { binding.etEmail.setText(it) }
                state.password?.let { binding.etPassword.setText(it) }
                state.confirmPassword?.let { binding.etRepeatPassword.setText(it) }
            }
        }
    }

    private fun observeEvents() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.event.collect { event ->
                when (event) {
                    is SignUpEvent.NavigateBack -> findNavController().popBackStack()
                    is SignUpEvent.NavigateToHome -> findNavController().navigate(R.id.action_signupFragment_to_coverFragment)
                    is SignUpEvent.ShowMessage -> showSnackbar(event.message)
                }
            }
        }
    }

    private fun showSnackbar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }
}
