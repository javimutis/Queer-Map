package com.cursoandroid.queermap.ui.login

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log // <--- ASEGÚRATE DE QUE ESTA IMPORTACIÓN ESTÉ AQUÍ
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.VisibleForTesting
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.cursoandroid.queermap.R
import com.cursoandroid.queermap.data.source.remote.FacebookSignInDataSource
import com.cursoandroid.queermap.data.source.remote.GoogleSignInDataSource
import com.cursoandroid.queermap.databinding.FragmentLoginBinding
import com.cursoandroid.queermap.util.Result // ¡¡¡Asegúrate de que esta sea la importación correcta a tu clase Result personalizada!!!
import com.facebook.CallbackManager
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject


@AndroidEntryPoint
class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding

    // Inyecta viewModel factory para crear viewModels con Hilt
    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    internal val viewModel: LoginViewModel
        get() = testViewModel ?: ViewModelProvider(
            this,
            viewModelFactory
        )[LoginViewModel::class.java]


    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal var testViewModel: LoginViewModel? = null

    // --- CAMBIOS AQUÍ para permitir inyección de mocks en tests ---
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
    // --- FIN CAMBIOS AQUÍ ---

    // MODIFICATION: Make googleSignInLauncher internal and add test version
    internal lateinit var googleSignInLauncher: ActivityResultLauncher<Intent> // Change from private

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal var testGoogleSignInLauncher: ActivityResultLauncher<Intent>? = null
    // END MODIFICATION

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
        // MODIFICATION START: Use testGoogleSignInLauncher if provided
        googleSignInLauncher = testGoogleSignInLauncher ?: registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                handleGoogleSignInResult(result.data)
            } else {
                showSnackbar("Inicio de sesión cancelado")
            }
        }
        // MODIFICATION END
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

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal fun handleGoogleSignInResult(data: Intent?) {
        // Breakpoint 1: Justo al inicio de la función handleGoogleSignInResult
        // Aquí verás que la función fue llamada.
        lifecycleScope.launch {
            Log.d("LoginFragment", "Inside handleGoogleSignInResult, data: $data") // LOG 1
            // Breakpoint 2: Antes de llamar a handleSignInResult del DataSource
            // Observa el valor de 'data' aquí.
            val result: com.cursoandroid.queermap.util.Result<String> = googleSignInDataSource.handleSignInResult(data)
            Log.d("LoginFragment", "Result from handleSignInResult: $result") // LOG 2
            // Breakpoint 3: Después de obtener el 'result' y antes del 'when'
            // Observa el valor de 'result' aquí.
            when (result) {
                is com.cursoandroid.queermap.util.Result.Success<String> -> {
                    val idToken = result.data
                    Log.d("LoginFragment", "Entering Success branch, idToken: $idToken") // LOG 3
                    Log.d("LoginFragment", "About to call viewModel.loginWithGoogle with idToken: $idToken") // LOG 4
                    // Breakpoint 4: Justo antes de la llamada a viewModel.loginWithGoogle
                    // Observa 'idToken'. Esta es la llamada crítica.
                    viewModel.loginWithGoogle(idToken)
                    Log.d("LoginFragment", "Finished calling viewModel.loginWithGoogle") // LOG 5
                    // Breakpoint 5: Después de la llamada a viewModel.loginWithGoogle
                    // Si llegas aquí, la llamada se ejecutó.
                }

                is com.cursoandroid.queermap.util.Result.Failure -> {
                    val errorMessage = result.exception.message ?: "Error desconocido"
                    Log.e("LoginFragment", "Google Sign-In failed: $errorMessage") // LOG 6
                    showSnackbar(errorMessage)
                }
            }
            Log.d("LoginFragment", "Exiting handleGoogleSignInResult coroutine") // LOG 7
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
    }
}
