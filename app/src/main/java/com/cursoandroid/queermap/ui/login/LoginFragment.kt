package com.cursoandroid.queermap.ui.login

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
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
class LoginFragment @JvmOverloads constructor(
    private val providedGoogleSignInLauncher: ActivityResultLauncher<Intent>? = null,
    private val providedCallbackManager: CallbackManager? = null,
    private val providedGoogleSignInDataSource: GoogleSignInDataSource? = null,
    private val providedFacebookSignInDataSource: FacebookSignInDataSource? = null
) : Fragment() {

    private val TAG = "LoginFragment"

    private var _binding: FragmentLoginBinding? = null

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal val binding get() = _binding

    private val viewModel: LoginViewModel by viewModels()

    // Estas son las dependencias que Hilt inyectará si el constructor no las provee.
    @Inject
    internal lateinit var hiltGoogleSignInDataSource: GoogleSignInDataSource

    @Inject
    internal lateinit var hiltFacebookSignInDataSource: FacebookSignInDataSource


    internal val actualGoogleSignInDataSource: GoogleSignInDataSource
        get() = providedGoogleSignInDataSource ?: hiltGoogleSignInDataSource

    internal val actualFacebookSignInDataSource: FacebookSignInDataSource
        get() = providedFacebookSignInDataSource ?: hiltFacebookSignInDataSource

    private val actualCallbackManager: CallbackManager by lazy {
        providedCallbackManager ?: CallbackManager.Factory.create()
    }

    // El ActivityResultLauncher es un poco diferente porque se registra en onViewCreated,
    // pero podemos hacer que el getter también priorice el del constructor.
    private var registeredGoogleSignInLauncher: ActivityResultLauncher<Intent>? = null

    // --- FIX IS HERE ---
    // Make actualGoogleSignInLauncher nullable
    internal val actualGoogleSignInLauncher: ActivityResultLauncher<Intent>? // <-- Changed to nullable
        get() = providedGoogleSignInLauncher ?: registeredGoogleSignInLauncher
    // --- END FIX ---

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal var testLogHelper: ((String, String) -> Unit)? = null

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal var testLogEHelper: ((String, String, Throwable?) -> Unit)? = null

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal var testLogWHelper: ((String, String) -> Unit)? = null


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

        if (providedGoogleSignInLauncher == null) {
            registeredGoogleSignInLauncher = registerForActivityResult(
                ActivityResultContracts.StartActivityForResult()
            ) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    handleGoogleSignInResult(result.data)
                } else {
                    showSnackbar("Inicio de sesión cancelado")
                }
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
        actualCallbackManager.onActivityResult(requestCode, resultCode, data)
    }

    private fun initFacebookLogin() {
        actualFacebookSignInDataSource.registerCallback(
            actualCallbackManager,
            object : FacebookCallback<LoginResult> {
                override fun onSuccess(result: LoginResult) {
                    logD("LoginFragment", "Facebook Login Success, result: $result")
                    lifecycleScope.launch {
                        try {
                            val accessToken = result.accessToken?.token
                            if (accessToken != null) {
                                logD("LoginFragment", "Facebook Access Token: $accessToken")
                                viewModel.loginWithFacebook(accessToken)
                            } else {
                                val errorMessage = "El token de acceso de Facebook es nulo. Por favor, inténtelo de nuevo."
                                logE("LoginFragment", errorMessage)
                                showSnackbar(errorMessage)
                            }
                        } catch (e: Exception) {
                            val errorMessage = "Error inesperado al procesar el token de Facebook: ${e.message ?: "detalle desconocido"}"
                            logE("LoginFragment", errorMessage, e)
                            showSnackbar(errorMessage)
                        }
                    }
                }

                override fun onCancel() {
                    logD("LoginFragment", "Facebook Login Cancelled")
                    showSnackbar("Inicio de sesión con Facebook cancelado.")
                }

                override fun onError(error: FacebookException) {
                    val errorMessage = error.message ?: "Error desconocido en Facebook Login."
                    logE("LoginFragment", "Facebook Login Error: $errorMessage", error)
                    showSnackbar("Error: $errorMessage")
                }
            })

        binding?.btnFacebookLogin?.setOnClickListener {
            actualFacebookSignInDataSource.logInWithReadPermissions(
                this,
                listOf("email", "public_profile")
            )
        }
    }

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

    private fun setupListeners() {
        binding?.btnLogin?.setOnClickListener {
            val email = binding?.etEmailLogin?.text.toString()
            val password = binding?.etPassword?.text.toString()
            viewModel.loginWithEmail(email, password)
        }

        binding?.btnGoogleSignIn?.setOnClickListener {
            actualGoogleSignInLauncher?.launch(actualGoogleSignInDataSource.getSignInIntent())
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
            logD("LoginFragment", "Inside handleGoogleSignInResult, data: $data")
            val result: com.cursoandroid.queermap.util.Result<String> =
                actualGoogleSignInDataSource.handleSignInResult(data)
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
                    logE("LoginFragment", "Google Sign-In failed: $errorMessage", result.exception)
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
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding?.let { currentBinding ->
                        currentBinding.progressBar.visibility =
                            if (state.isLoading) View.VISIBLE else View.GONE

                        state.errorMessage?.let { msg ->
                            showSnackbar(msg)
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
                        is LoginEvent.ShowMessage -> {
                            showSnackbar(event.message)
                            if (event.message.startsWith("Advertencia")) {
                                logW(TAG, event.message)
                            }
                        }
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