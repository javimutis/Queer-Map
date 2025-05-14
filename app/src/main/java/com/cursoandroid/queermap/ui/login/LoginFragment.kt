package com.cursoandroid.queermap.ui.login

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.cursoandroid.queermap.R
import com.cursoandroid.queermap.databinding.FragmentLoginBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@AndroidEntryPoint
class LoginFragment : Fragment(R.layout.fragment_login) {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private val viewModel: LoginViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentLoginBinding.bind(view)

        binding.loginButton.setOnClickListener {
            val email = binding.emailEditText.text.toString().trim()
            val password = binding.passwordEditText.text.toString().trim()
            val remember = binding.rememberCheckBox.isChecked

            if (isValidEmail(email) && isValidPassword(password)) {
                // Corrección aquí: se pasa una referencia de función (de tipo correcto)
                viewModel.login(email, password, remember, ::saveCredentials)
            } else {
                showToast("Correo o contraseña inválidos")
            }
        }

        // Corrección aquí: se reemplaza collectIn por lifecycleScope.launch + collect
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                binding.progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE
                if (state.isSuccess) {
                    showToast("Inicio de sesión exitoso")
//                    findNavController().navigate(R.id.action_loginFragment_to_readTermsFragment)
                }
                state.errorMessage?.let { showToast(it) }
            }
        }
    }

    private fun saveCredentials(email: String, password: String) {
        val prefs = requireContext().getSharedPreferences("login", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putString("email", email)
            putString("password", password)
            apply()
        }
    }

    private fun isValidEmail(email: String) = email.contains("@")
    private fun isValidPassword(password: String) = password.length >= 6
    private fun showToast(msg: String) =
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
