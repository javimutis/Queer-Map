package com.cursoandroid.queermap.ui.signup

import android.app.DatePickerDialog
import android.content.Context
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.cursoandroid.queermap.R
import com.cursoandroid.queermap.databinding.FragmentSignupBinding
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.Calendar
import java.text.SimpleDateFormat
import java.util.Locale

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
        binding.etEmail.addTextChangedListener {
            viewModel.onEvent(SignUpEvent.OnEmailChanged(it.toString()))
        }

        binding.etPassword.addTextChangedListener {
            viewModel.onEvent(SignUpEvent.OnPasswordChanged(it.toString()))
        }

        binding.etRepeatPassword.addTextChangedListener {
            viewModel.onEvent(SignUpEvent.OnConfirmPasswordChanged(it.toString()))
        }

        binding.etName.addTextChangedListener {
            viewModel.onEvent(SignUpEvent.OnNameChanged(it.toString()))
        }

        binding.btnRegister.setOnClickListener {
            viewModel.onEvent(SignUpEvent.OnRegisterClicked)
        }

        binding.ivBack.setOnClickListener {
            viewModel.onBackPressed()
        }
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                binding.progressBar.visibility =
                    if (state.isLoading) View.VISIBLE else View.GONE

                if (state.isEmailInvalid) showSnackbar("Por favor ingresa un email válido")
                if (state.isPasswordInvalid) showSnackbar("La contraseña debe tener al menos 6 caracteres")
                if (state.doPasswordsMismatch) showSnackbar("Las contraseñas no coinciden")
                state.user?.let { binding.etUser.setText(it) }

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
                    else -> Unit
                }
            }
        }
    }

    private fun showSnackbar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }

    private fun togglePasswordVisibility(editText: TextInputEditText, eyeIcon: ImageView) {
        val isPasswordVisible = editText.inputType == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
        if (isPasswordVisible) {
            editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            eyeIcon.setImageResource(R.drawable.closed_eye)
        } else {
            editText.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            eyeIcon.setImageResource(R.drawable.open_eye)
        }
        editText.setSelection(editText.text?.length ?: 0)
    }

    private fun setupDatePicker(context: Context, birthdayEditText: TextInputEditText) {
        val calendar = Calendar.getInstance()
        val dateSetListener = DatePickerDialog.OnDateSetListener { _, year, month, day ->
            val selectedDate = Calendar.getInstance().apply {
                set(year, month, day)
            }
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            birthdayEditText.setText(sdf.format(selectedDate.time))
        }

        birthdayEditText.setOnClickListener {
            DatePickerDialog(
                context,
                dateSetListener,
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
    }
}
