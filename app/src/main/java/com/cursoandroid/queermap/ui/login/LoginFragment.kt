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
import com.cursoandroid.queermap.databinding.FragmentLoginBinding
import com.facebook.AccessToken
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.common.api.ApiException
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest // Import this
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val viewModel: LoginViewModel by viewModels()

    @Inject lateinit var googleSignInClient: GoogleSignInClient
    @Inject lateinit var facebookSignInDataSource: FacebookSignInDataSource

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
        checkLoginStatus()
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
        // Pass the activity result to Facebook SDK for handling login results
        callbackManager.onActivityResult(requestCode, resultCode, data)
    }

    // Setup and Initialization
    private fun initGoogleSignInLauncher() {
        googleSignInLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                handleGoogleSignInResult(result.data)
            } else {
                showSnackbar("Inicio de sesión cancelado")
            }
        }
    }

    private fun initFacebookLogin() {
        callbackManager = CallbackManager.Factory.create()
        // Register the callback with the FacebookSignInDataSource
        facebookSignInDataSource.registerCallback(callbackManager)

        binding.btnFacebookLogin.setOnClickListener {
            // Initiate Facebook login through the data source
            facebookSignInDataSource.logInWithReadPermissions(this, listOf("email", "public_profile"))
        }
    }

    // UI Interaction Handling
    private fun setupListeners() {
        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString()
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
            googleSignInLauncher.launch(googleSignInClient.signInIntent)
        }

        binding.tvForgotPassword.setOnClickListener {
            viewModel.onForgotPasswordClicked()
        }

        binding.ivBack.setOnClickListener {
            viewModel.onBackPressed()
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
                    binding.etEmail.setText(it)
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
                        findNavController().navigate(R.id.action_loginFragment_to_coverFragment)
                    is LoginEvent.NavigateToForgotPassword ->
                        findNavController().navigate(R.id.action_loginFragment_to_forgotPasswordFragment)
                    is LoginEvent.NavigateBack ->
                        findNavController().popBackStack()
                }
            }
        }

        // Observe Facebook access token results from the data source
        viewLifecycleOwner.lifecycleScope.launch {
            // Convert ReceiveChannel to Flow and then collect
            facebookSignInDataSource.accessTokenChannel.collectLatest { result ->
                result.onSuccess { token ->
                    viewModel.loginWithFacebook(token)
                }.onFailure { exception ->
                    showSnackbar("Error: ${exception.message}")
                }
            }
        }
    }

    // Business Logic
    private fun checkLoginStatus() {
        val accessToken = AccessToken.getCurrentAccessToken()
        val isLoggedIn = accessToken != null && !accessToken.isExpired
        if (isLoggedIn) {
            findNavController().navigate(R.id.action_loginFragment_to_coverFragment)
        }
    }

    private fun handleGoogleSignInResult(data: Intent?) {
        val task = GoogleSignIn.getSignedInAccountFromIntent(data)
        try {
            val account = task.getResult(ApiException::class.java)
            val idToken = account?.idToken
            if (idToken != null) {
                viewModel.loginWithGoogle(idToken)
            } else {
                showSnackbar("ID token no disponible")
            }
        } catch (e: ApiException) {
            showSnackbar("Error en Sign-In: ${e.message}")
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