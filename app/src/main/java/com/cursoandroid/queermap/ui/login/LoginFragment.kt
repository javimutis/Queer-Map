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
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider // Keep for other ViewModels if any
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.cursoandroid.queermap.R
import com.cursoandroid.queermap.data.source.remote.FacebookSignInDataSource
import com.cursoandroid.queermap.data.source.remote.GoogleSignInDataSource
import com.cursoandroid.queermap.databinding.FragmentLoginBinding
import com.cursoandroid.queermap.util.Result
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

    // CAMBIO CLAVE: Permite que Hilt inyecte directamente el ViewModel.
    // En tests con @BindValue, Hilt inyectará el mock.
    // Para producción, Hilt inyectará la implementación real.
    // Esto es el patrón estándar para ViewModels inyectados por Hilt.
    private val viewModel: LoginViewModel by viewModels() // Use KTX viewModels() extension function

    // Puedes mantener estas para DataSources si las inyectas aquí y quieres mockear
    // Si tus DataSources ya son parte del constructor del ViewModel,
    // y el ViewModel es el que las usa, no necesitas inyectarlas directamente en el fragmento.
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


    // Mantén esto si necesitas mockear el launcher en el fragmento, si no, lo remueves
    internal lateinit var googleSignInLauncher: ActivityResultLauncher<Intent>
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal var testGoogleSignInLauncher: ActivityResultLauncher<Intent>? = null

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal var testCallbackManager: CallbackManager? = null

    // Propiedad lazy para el CallbackManager, que usará el mock en tests si se inyecta.
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
        viewModel.loadUserCredentials() // Ahora esto llamará al ViewModel inyectado (real o mock)

        // Asegúrate de usar 'googleSignInLauncher' del fragmento, no 'testGoogleSignInLauncher' para la inicialización normal
        // El 'testGoogleSignInLauncher' se usará si lo asignas en el test.
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
        facebookSignInDataSource.registerCallback(callbackManager, object : FacebookCallback<LoginResult> {
            override fun onSuccess(result: LoginResult) {
                Log.d("LoginFragment", "Facebook Login Success: ${result.accessToken.token}")
                lifecycleScope.launch {
                    viewModel.loginWithFacebook(result.accessToken.token)
                }
            }

            override fun onCancel() {
                Log.d("LoginFragment", "Facebook Login Cancelled")
                showSnackbar("Inicio de sesión con Facebook cancelado.")
            }

            override fun onError(error: FacebookException) {
                val errorMessage = error.message ?: "Error desconocido en Facebook Login."
                Log.e("LoginFragment", "Facebook Login Error: $errorMessage", error)
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
            Log.d("LoginFragment", "Inside handleGoogleSignInResult, data: $data")
            val result: com.cursoandroid.queermap.util.Result<String> = googleSignInDataSource.handleSignInResult(data)
            Log.d("LoginFragment", "Result from handleSignInResult: $result")
            when (result) {
                is com.cursoandroid.queermap.util.Result.Success<String> -> {
                    val idToken = result.data
                    Log.d("LoginFragment", "Entering Success branch, idToken: $idToken")
                    Log.d("LoginFragment", "About to call viewModel.loginWithGoogle with idToken: $idToken")
                    viewModel.loginWithGoogle(idToken)
                    Log.d("LoginFragment", "Finished calling viewModel.loginWithGoogle")
                }

                is com.cursoandroid.queermap.util.Result.Failure -> {
                    val errorMessage = result.exception.message ?: "Error desconocido"
                    Log.e("LoginFragment", "Google Sign-In failed: $errorMessage")
                    showSnackbar("Error en Sign-In: $errorMessage")
                }
            }
            Log.d("LoginFragment", "Exiting handleGoogleSignInResult coroutine")
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
    }
}