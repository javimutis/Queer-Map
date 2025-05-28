// ui/signup/SignUpFragment.kt
package com.cursoandroid.queermap.ui.signup

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.cursoandroid.queermap.R
import com.cursoandroid.queermap.databinding.FragmentSignupBinding
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointBackward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
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
        observeEvents() // Observar eventos one-shot
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupListeners() {
        binding.etEmail.addTextChangedListener { editable ->
            viewModel.onEvent(SignUpEvent.OnEmailChanged(editable.toString()))
            binding.tilEmail.error = null
        }

        binding.etPassword.addTextChangedListener { editable ->
            viewModel.onEvent(SignUpEvent.OnPasswordChanged(editable.toString()))
            binding.tilPassword.error = null
        }

        binding.etRepeatPassword.addTextChangedListener { editable ->
            viewModel.onEvent(SignUpEvent.OnConfirmPasswordChanged(editable.toString()))
            binding.tilRepeatPassword.error = null
        }

        binding.etUser.addTextChangedListener { editable ->
            viewModel.onEvent(SignUpEvent.OnUserChanged(editable.toString()))
            binding.tilUser.error = null
        }

        binding.etName.addTextChangedListener { editable ->
            viewModel.onEvent(SignUpEvent.OnFullNameChanged(editable.toString()))
            binding.tilName.error = null
        }

        binding.btnRegister.setOnClickListener {
            viewModel.onEvent(SignUpEvent.OnRegisterClicked)
        }

        binding.ivBack.setOnClickListener {
            viewModel.onBackPressed()
        }
        binding.tietBirthday.isFocusable = false
        binding.tietBirthday.isClickable = true
        binding.tietBirthday.setOnClickListener {
            showModernDatePicker()
            binding.tilBirthday.error = null
        }
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                binding.progressBar.visibility =
                    if (state.isLoading) View.VISIBLE else View.GONE

                // Limpiar errores antes de aplicar los nuevos
                binding.tilEmail.error = null
                binding.tilPassword.error = null
                binding.tilRepeatPassword.error = null
                binding.tilUser.error = null
                binding.tilName.error = null
                binding.tilBirthday.error = null

                // Mostrar errores de validación
                if (state.isEmailInvalid) {
                    binding.tilEmail.error = "Please enter a valid email."
                }
                if (state.isPasswordInvalid) {
                    binding.tilPassword.error = "Password must be at least 8 characters long."
                }
                if (state.doPasswordsMismatch) {
                    binding.tilRepeatPassword.error = "Passwords do not match."
                }
                if (state.isBirthdayInvalid) {
                    binding.tilBirthday.error = "Please enter a valid birthday."
                }

                // Actualizar campos de texto (aunque la entrada del usuario ya lo hace directamente)
                state.user?.let { newText ->
                    if (binding.etUser.text.toString() != newText) {
                        binding.etUser.setText(newText)
                        binding.etUser.setSelection(newText.length)
                    }
                }
                state.email?.let { newText ->
                    if (binding.etEmail.text.toString() != newText) {
                        binding.etEmail.setText(newText)
                        binding.etEmail.setSelection(newText.length)
                    }
                }
                state.password?.let { newText ->
                    if (binding.etPassword.text.toString() != newText) {
                        binding.etPassword.setText(newText)
                        binding.etPassword.setSelection(newText.length)
                    }
                }
                state.confirmPassword?.let { newText ->
                    if (binding.etRepeatPassword.text.toString() != newText) {
                        binding.etRepeatPassword.setText(newText)
                        binding.etRepeatPassword.setSelection(newText.length)
                    }
                }
                state.fullName?.let { newText ->
                    if (binding.etName.text.toString() != newText) {
                        binding.etName.setText(newText)
                        binding.etName.setSelection(newText.length)
                    }
                }
                state.birthday?.let { newText ->
                    if (binding.tietBirthday.text.toString() != newText) {
                        binding.tietBirthday.setText(newText)
                    }
                }
            }
        }
    }

    private fun observeEvents() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.event.collect { event ->
                when (event) {
                    is SignUpEvent.NavigateBack -> findNavController().popBackStack()
                    is SignUpEvent.NavigateToHome -> {
                        // Navegar a CoverFragment (temporalmente)
                        findNavController().navigate(R.id.action_signupFragment_to_coverFragment)
                    }
                    is SignUpEvent.ShowMessage -> showSnackbar(event.message)
                    // Estos eventos son para que el Fragment los envíe al ViewModel, no para que los observe del ViewModel.
                    // Ya se eliminaron los TODOs en la revisión anterior.
                    is SignUpEvent.OnBirthdayChanged,
                    is SignUpEvent.OnConfirmPasswordChanged,
                    is SignUpEvent.OnEmailChanged,
                    is SignUpEvent.OnFullNameChanged,
                    is SignUpEvent.OnPasswordChanged,
                    SignUpEvent.OnRegisterClicked,
                    is SignUpEvent.OnUserChanged -> Unit
                }
            }
        }
    }

    private fun showSnackbar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }

    private fun showModernDatePicker() {
        val builder = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Select your birth date")
            .setTheme(R.style.ThemeOverlay_QueerMap_MaterialDatePicker)

        val currentBirthDateText = binding.tietBirthday.text.toString()
        if (currentBirthDateText.isNotEmpty()) {
            try {
                val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val date = sdf.parse(currentBirthDateText)
                if (date != null) builder.setSelection(date.time)
            } catch (_: Exception) {
                // Ignore parsing errors, date picker will default to current date or initial selection
            }
        }

        val constraints = CalendarConstraints.Builder()
            .setValidator(DateValidatorPointBackward.now())
            .build()

        builder.setCalendarConstraints(constraints)

        val picker = builder.build()

        picker.addOnPositiveButtonClickListener { selection ->
            val date = Date(selection)
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val formattedDate = sdf.format(date)
            binding.tietBirthday.setText(formattedDate)
            viewModel.onEvent(SignUpEvent.OnBirthdayChanged(formattedDate))
            binding.tilBirthday.error = null
        }

        picker.show(parentFragmentManager, picker.toString())
    }
}