package com.cursoandroid.queermap.ui.login

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.VisibleForTesting
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.repeatOnLifecycle
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
    private val binding get() = _binding

    // Inyecta viewModel factory para crear viewModels con Hilt
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory

    internal val viewModel: LoginViewModel
        get() = testViewModel ?: ViewModelProvider(this, viewModelFactory)[LoginViewModel::class.java]


    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal var testViewModel: LoginViewModel? = null

    @Inject internal lateinit var googleSignInDataSource: GoogleSignInDataSource
    @Inject internal lateinit var facebookSignInDataSource: FacebookSignInDataSource

    private lateinit var googleSignInLauncher: androidx.activity.result.ActivityResultLauncher<Intent>
    private lateinit var callbackManager: CallbackManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding?.root ?: View(context)
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
        if (::callbackManager.isInitialized) {
            callbackManager.onActivityResult(requestCode, resultCode, data)
        }
    }

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
        facebookSignInDataSource.registerCallback(callbackManager)

        binding?.btnFacebookLogin?.setOnClickListener {
            facebookSignInDataSource.logInWithReadPermissions(
                this,
                listOf("email", "public_profile")
            )
        }
    }

    private fun setupListeners() {
        binding?.btnLogin?.setOnClickListener {
            val email = binding?.etEmailLogin?.text.toString()
            val password = binding?.etPassword?.text.toString()
            viewModel.loginWithEmail(email, password)
        }

        binding?.btnGoogleSignIn?.setOnClickListener {
            googleSignInLauncher.launch(googleSignInDataSource.getSignInIntent())
        }

        binding?.tvForgotPassword?.setOnClickListener {
            viewModel.onForgotPasswordClicked()
        }

        binding?.ivBack?.setOnClickListener {
            viewModel.onBackPressed()
        }
        binding?.tvSignUpBtn?.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_signupFragment)
        }
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding?.let { currentBinding ->
                        // Usar currentBinding.progressBar.isInvisible para que el espacio se mantenga
                        // aunque no esté visible, evitando posibles reflows de layout.
                        currentBinding.progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE

                        if (state.errorMessage != null) {
                            showSnackbar(state.errorMessage)
                        }
                        state.email?.let { emailText ->
                            currentBinding.etEmailLogin.setText(emailText)
                        }
                        state.password?.let { passwordText ->
                            currentBinding.etPassword.setText(passwordText)
                        }
                    }
                }
            }
        }
    }

    private fun observeEvents() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
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
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                facebookSignInDataSource.accessTokenChannel.collectLatest { result ->
                    result.onSuccess { token ->
                        viewModel.loginWithFacebook(token)
                    }.onFailure { exception ->
                        showSnackbar("Error: ${exception.message}")
                    }
                }
            }
        }
    }

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

    private fun showSnackbar(message: String) {
        binding?.root?.let {
            Snackbar.make(it, message, Snackbar.LENGTH_SHORT).show()
        }
    }
}