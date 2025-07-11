package com.cursoandroid.queermap.ui.login

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log // Keep this import for Log.d
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.VisibleForTesting
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.cursoandroid.queermap.R
import com.cursoandroid.queermap.data.source.remote.FacebookSignInDataSource
import com.cursoandroid.queermap.data.source.remote.GoogleSignInDataSource
import com.cursoandroid.queermap.databinding.FragmentLoginBinding
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginResult
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject


@AndroidEntryPoint
class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding

    private val viewModel: LoginViewModel by viewModels()

    @Inject
    internal lateinit var _googleSignInDataSource: GoogleSignInDataSource

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal var testGoogleSignInDataSource: GoogleSignInDataSource? = null
    internal val googleSignInDataSource: GoogleSignInDataSource
        get() = testGoogleSignInDataSource ?: _googleSignInDataSource

    @Inject
    internal lateinit var _facebookSignInDataSource: FacebookSignInDataSource

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal var testFacebookSignInDataSource: FacebookSignInDataSource? = null
    internal val facebookSignInDataSource: FacebookSignInDataSource
        get() = testFacebookSignInDataSource ?: _facebookSignInDataSource


    internal lateinit var googleSignInLauncher: ActivityResultLauncher<Intent>

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal var testGoogleSignInLauncher: ActivityResultLauncher<Intent>? = null

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal var testCallbackManager: CallbackManager? = null

    // NUEVAS PROPIEDADES PARA CONTROLAR EL LOGGING EN TESTS
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal var testLogHelper: ((String, String) -> Unit)? = null

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal var testLogEHelper: ((String, String, Throwable?) -> Unit)? = null

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal var testLogWHelper: ((String, String) -> Unit)? = null


    private val callbackManager: CallbackManager by lazy {
        testCallbackManager ?: CallbackManager.Factory.create()
    }


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

        googleSignInLauncher = testGoogleSignInLauncher ?: registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                handleGoogleSignInResult(result.data)
            } else {
                showSnackbar("Inicio de sesión cancelado")
            }
        }
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

    private fun initFacebookLogin() {
        facebookSignInDataSource.registerCallback(
            callbackManager,
            object : FacebookCallback<LoginResult> {
                override fun onSuccess(result: LoginResult) {
                    // USA LA FUNCIÓN AUXILIAR logD
                    logD("LoginFragment", "Facebook Login Success: ${result.accessToken.token}")
                    lifecycleScope.launch {
                        viewModel.loginWithFacebook(result.accessToken.token)
                    }
                }

                override fun onCancel() {
                    // USA LA FUNCIÓN AUXILIAR logD
                    logD("LoginFragment", "Facebook Login Cancelled")
                    showSnackbar("Inicio de sesión con Facebook cancelado.")
                }

                override fun onError(error: FacebookException) {
                    val errorMessage = error.message ?: "Error desconocido en Facebook Login."
                    // USA LA FUNCIÓN AUXILIAR logE
                    logE("LoginFragment", "Facebook Login Error: $errorMessage", error)
                    showSnackbar("Error: $errorMessage")
                }
            })

        binding?.btnFacebookLogin?.setOnClickListener {
            facebookSignInDataSource.logInWithReadPermissions(
                this,
                listOf("email", "public_profile")
            )
        }
    }

    // NUEVAS FUNCIONES AUXILIARES PARA LOGGING
    private fun logD(tag: String, msg: String) {
        testLogHelper?.invoke(tag, msg) ?: Log.d(tag, msg)
    }

    private fun logE(tag: String, msg: String, tr: Throwable? = null) {
        testLogEHelper?.invoke(tag, msg, tr) ?: run {
            if (tr != null) {
                Log.e(tag, msg, tr)
            } else {
                Log.e(tag, msg)
            }
        }
    }

    private fun logW(tag: String, msg: String) {
        testLogWHelper?.invoke(tag, msg) ?: Log.w(tag, msg)
    }
    // FIN DE NUEVAS FUNCIONES AUXILIARES PARA LOGGING


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

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal fun handleGoogleSignInResult(data: Intent?) {
        lifecycleScope.launch {
            // USA LAS FUNCIONES AUXILIARES logD y logE
            logD("LoginFragment", "Inside handleGoogleSignInResult, data: $data")
            val result: com.cursoandroid.queermap.util.Result<String> =
                googleSignInDataSource.handleSignInResult(data)
            logD("LoginFragment", "Result from handleSignInResult: $result")
            when (result) {
                is com.cursoandroid.queermap.util.Result.Success<String> -> {
                    val idToken = result.data
                    logD("LoginFragment", "Entering Success branch, idToken: $idToken")
                    logD(
                        "LoginFragment",
                        "About to call viewModel.loginWithGoogle with idToken: $idToken"
                    )
                    viewModel.loginWithGoogle(idToken)
                    logD("LoginFragment", "Finished calling viewModel.loginWithGoogle")
                }

                is com.cursoandroid.queermap.util.Result.Failure -> {
                    val errorMessage = result.exception.message ?: "Error desconocido"
                    logE("LoginFragment", "Google Sign-In failed: $errorMessage")
                    showSnackbar("Error en Sign-In: $errorMessage")
                }
            }
            logD("LoginFragment", "Exiting handleGoogleSignInResult coroutine")
        }
    }

    private fun showSnackbar(message: String) {
        binding?.root?.let {
            Snackbar.make(it, message, Snackbar.LENGTH_LONG).show()
        }
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            // FIX: Corrected typo from viewLifecycleowner to viewLifecycleOwner
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding?.let { currentBinding ->
                        currentBinding.progressBar.visibility =
                            if (state.isLoading) View.VISIBLE else View.GONE

                        state.errorMessage?.let { msg ->
                            showSnackbar(msg)
                            // Considera añadir viewModel.clearErrorMessage() aquí si lo tienes.
                        }
                        currentBinding.etEmailLogin.setText(state.email)
                        currentBinding.etPassword.setText(state.password)
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
                            val bundle = bundleOf(
                                "socialUserEmail" to event.socialUserEmail,
                                "socialUserName" to event.socialUserName,
                                "isSocialLoginFlow" to event.isSocialLoginFlow
                            )
                            findNavController().navigate(R.id.signupFragment, bundle)
                        }

                    }
                }
            }
        }
    }
}
