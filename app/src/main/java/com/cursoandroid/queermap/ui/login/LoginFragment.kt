// app/src/main/java/com/cursoandroid/queermap/ui/login/LoginFragment.kt

package com.cursoandroid.queermap.ui.login

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.cursoandroid.queermap.R
import com.cursoandroid.queermap.data.source.remote.FacebookSignInDataSource
import com.cursoandroid.queermap.data.source.remote.GoogleSignInDataSource
import com.cursoandroid.queermap.databinding.FragmentLoginBinding
import com.facebook.AccessToken
import com.facebook.CallbackManager
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    // Propiedad 'viewModel' del Fragment. Para el test, la inyectaremos vía reflexión.
    // Aunque aquí es 'private val', el test la modificará en tiempo de ejecución.
    private val viewModel: LoginViewModel by viewModels()

    @Inject
    lateinit var googleSignInDataSource: GoogleSignInDataSource
    @Inject
    lateinit var facebookSignInDataSource: FacebookSignInDataSource

    private lateinit var googleSignInLauncher: androidx.activity.result.ActivityResultLauncher<Intent>
    private lateinit var callbackManager: CallbackManager

    // Lifecycle methods
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.loadUserCredentials()
        initGoogleSignInLauncher()
        initFacebookLogin()
        setupListeners()
        observeState()
        observeEvents()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        callbackManager.onActivityResult(requestCode, resultCode, data)
    }

    // Setup and Initialization
    private fun initGoogleSignInLauncher() {
        googleSignInLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                // AHORA ESTA FUNCIÓN ES ACCESIBLE PARA EL TEST
                handleGoogleSignInResult(result.data)
            } else {
                showSnackbar("Inicio de sesión cancelado")
            }
        }
    }

    private fun initFacebookLogin() {
        callbackManager = CallbackManager.Factory.create()
        facebookSignInDataSource.registerCallback(callbackManager)

        binding.btnFacebookLogin.setOnClickListener {
            facebookSignInDataSource.logInWithReadPermissions(
                this,
                listOf("email", "public_profile")
            )
        }
    }

    // UI Interaction Handling
    private fun setupListeners() {
        binding.btnLogin.setOnClickListener {
            val email = binding.etEmailLogin.text.toString()
            val password = binding.etPassword.text.toString()
            if (!isValidEmail(email)) {
                showSnackbar("Por favor ingresa un email válido")
            } else if (!isValidPassword(password)) {
                showSnackbar("La contraseña debe tener al menos 6 caracteres")
            } else {
                viewModel.loginWithEmail(email, password)
            }
        }

        binding.btnGoogleSignIn.setOnClickListener {
            googleSignInLauncher.launch(googleSignInDataSource.getSignInIntent())
        }

        binding.tvForgotPassword.setOnClickListener {
            viewModel.onForgotPasswordClicked()
        }

        binding.ivBack.setOnClickListener {
            viewModel.onBackPressed()
        }
        binding.tvSignUpBtn.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_signupFragment)
        }
    }

    // State and Event Observation
    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                if (state.isLoading) {
                    binding.progressBar.visibility = View.VISIBLE
                } else {
                    binding.progressBar.visibility = View.GONE
                }
                if (state.isEmailInvalid) {
                    showSnackbar("Por favor ingresa un email válido")
                }
                if (state.isPasswordInvalid) {
                    showSnackbar("La contraseña debe tener al menos 6 caracteres")
                }
                if (state.errorMessage != null) {
                    showSnackbar(state.errorMessage)
                }
                state.email?.let {
                    binding.etEmailLogin.setText(it)
                }
                state.password?.let {
                    binding.etPassword.setText(it)
                }
            }
        }
    }

    private fun observeEvents() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.event.collect { event ->
                when (event) {
                    is LoginEvent.ShowMessage -> showSnackbar(event.message)
                    is LoginEvent.NavigateToHome ->
                        findNavController().navigate(R.id.action_loginFragment_to_mapFragment)

                    is LoginEvent.NavigateToForgotPassword ->
                        findNavController().navigate(R.id.action_loginFragment_to_forgotPasswordFragment)

                    is LoginEvent.NavigateBack ->
                        findNavController().popBackStack()

                    is LoginEvent.NavigateToSignupWithArgs -> {
                        val directions =
                            LoginFragmentDirections.actionLoginFragmentToSignupFragment(
                                socialUserEmail = event.socialUserEmail,
                                socialUserName = event.socialUserName,
                                isSocialLoginFlow = event.isSocialLoginFlow
                            )
                        findNavController().navigate(directions)
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            facebookSignInDataSource.accessTokenChannel.collectLatest { result ->
                result.onSuccess { token ->
                    viewModel.loginWithFacebook(token)
                }.onFailure { exception ->
                    showSnackbar("Error: ${exception.message}")
                }
            }
        }
    }

    // CAMBIO: Hacer esta función interna para poder llamarla desde el test
    internal fun handleGoogleSignInResult(data: Intent?) {
        lifecycleScope.launch {
            googleSignInDataSource.handleSignInResult(data)
                .onSuccess { idToken ->
                    if (idToken != null) {
                        viewModel.loginWithGoogle(idToken)
                    } else {
                        showSnackbar("ID token no disponible")
                    }
                }
                .onFailure { exception ->
                    showSnackbar("Error en Sign-In: ${exception.message}")
                }
        }
    }

    // Utilities
    private fun showSnackbar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }

    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun isValidPassword(password: String): Boolean {
        return password.length >= 6
    }
}