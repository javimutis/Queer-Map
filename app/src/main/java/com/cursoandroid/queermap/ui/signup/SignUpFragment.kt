package com.cursoandroid.queermap.ui.signup

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.cursoandroid.queermap.R
import com.cursoandroid.queermap.databinding.FragmentSignupBinding
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointBackward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.snackbar.Snackbar
import com.squareup.picasso.Picasso
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
    private val args: SignUpFragmentArgs by navArgs()

    private val googleSignInLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                viewModel.onEvent(SignUpEvent.OnGoogleSignInResult(result.data))
            } else {
                viewModel.onEvent(SignUpEvent.ShowMessage("Inicio de sesión con Google cancelado o fallido."))
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSignupBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.setSocialLoginData(args.isSocialLoginFlow, args.socialUserEmail, args.socialUserName)

        setupListeners()
        observeState()
        observeEvents()
        observeGoogleSignInIntent()
        loadSocialIcons()
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

        binding.ivGoogleSignIn.setOnClickListener {
            viewModel.onEvent(SignUpEvent.OnGoogleSignUpClicked)
        }
        binding.ivFacebookLSignIn.setOnClickListener {
            viewModel.onEvent(SignUpEvent.OnFacebookSignUpClicked)
            com.facebook.login.LoginManager.getInstance().logInWithReadPermissions(this, listOf("email", "public_profile"))
        }
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                binding.progressBar.visibility =
                    if (state.isLoading) View.VISIBLE else View.GONE

                binding.tilEmail.error = null
                binding.tilPassword.error = null
                binding.tilRepeatPassword.error = null
                binding.tilUser.error = null
                binding.tilName.error = null
                binding.tilBirthday.error = null

                if (state.isSocialLoginFlow) {
                    binding.tilEmail.visibility = View.GONE
                    binding.tilPassword.visibility = View.GONE
                    binding.tilRepeatPassword.visibility = View.GONE
                    binding.ivGoogleSignIn.visibility = View.GONE
                    binding.ivFacebookLSignIn.visibility = View.GONE
                    binding.btnRegister.text = "Completar mi Perfil"

                } else {
                    binding.tilEmail.visibility = View.VISIBLE
                    binding.tilPassword.visibility = View.VISIBLE
                    binding.tilRepeatPassword.visibility = View.VISIBLE
                    binding.ivGoogleSignIn.visibility = View.VISIBLE
                    binding.ivFacebookLSignIn.visibility = View.VISIBLE
                    binding.btnRegister.text = "Registrarme"
                }


                if (state.isEmailInvalid) {
                    binding.tilEmail.error = "Por favor, ingresa un email válido."
                }
                if (state.isPasswordInvalid) {
                    binding.tilPassword.error = "La contraseña debe tener al menos 8 caracteres."
                }
                if (state.doPasswordsMismatch) {
                    binding.tilRepeatPassword.error = "Las contraseñas no coinciden."
                }
                if (state.isBirthdayInvalid) {
                    binding.tilBirthday.error = "Por favor, ingresa una fecha de nacimiento válida."
                }

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
                        findNavController().navigate(R.id.action_signupFragment_to_mapFragment)
                    }
                    is SignUpEvent.ShowMessage -> showSnackbar(event.message)
                    else -> Unit
                }
            }
        }
    }

    private fun observeGoogleSignInIntent() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.launchGoogleSignIn.collect { intent ->
                googleSignInLauncher.launch(intent)
            }
        }
    }

    private fun showSnackbar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }

    private fun showModernDatePicker() {
        val builder = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Selecciona tu fecha de nacimiento")
            .setTheme(R.style.ThemeOverlay_QueerMap_MaterialDatePicker)

        val currentBirthDateText = binding.tietBirthday.text.toString()
        if (currentBirthDateText.isNotEmpty()) {
            try {
                val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val date = sdf.parse(currentBirthDateText)
                if (date != null) builder.setSelection(date.time)
            } catch (_: Exception) {
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

    private fun loadSocialIcons() {
        Picasso.get().load(R.drawable.google_icon).into(binding.ivGoogleSignIn)
        Picasso.get().load(R.drawable.facebook_icon).into(binding.ivFacebookLSignIn)
    }
}