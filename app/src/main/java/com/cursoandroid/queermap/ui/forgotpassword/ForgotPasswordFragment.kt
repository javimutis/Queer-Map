package com.cursoandroid.queermap.ui.forgotpassword

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.cursoandroid.queermap.R
import com.cursoandroid.queermap.databinding.FragmentForgotPasswordBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ForgotPasswordFragment : Fragment(R.layout.fragment_forgot_password) {

    private var _binding: FragmentForgotPasswordBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ForgotPasswordViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentForgotPasswordBinding.bind(view)

        binding.sendResetButton.setOnClickListener {
            val email = binding.emailEditText.text.toString().trim()
            if (email.isNotEmpty() && email.contains("@")) {
                viewModel.sendPasswordReset(email)
            } else {
                showToast("Ingrese un correo válido")
            }
        }

        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                binding.progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE
                state.message?.let { showToast(it) }
                if (state.isSuccess) {
                    findNavController().popBackStack() // O ir a otra pantalla si deseas
                }
            }
        }
    }

    private fun showToast(msg: String) =
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
