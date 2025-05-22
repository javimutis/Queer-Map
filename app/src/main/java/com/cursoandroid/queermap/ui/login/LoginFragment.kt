package com.cursoandroid.queermap.ui.login

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.cursoandroid.queermap.R
import com.cursoandroid.queermap.data.source.remote.FacebookSignInDataSource
import com.cursoandroid.queermap.databinding.FragmentLoginBinding
import com.facebook.CallbackManager
import com.facebook.login.LoginResult
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.common.api.ApiException
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val loginViewModel: LoginViewModel by viewModels()

    @Inject lateinit var googleSignInClient: GoogleSignInClient
    @Inject lateinit var facebookSignInDataSource: FacebookSignInDataSource

    private lateinit var googleSignInLauncher: ActivityResultLauncher<Intent>
    private lateinit var callbackManager: CallbackManager

    // ----------------------- Ciclo de vida -----------------------

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initGoogleSignInLauncher()
        initFacebookLogin()
        setupListeners()
        observeViewModel()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        callbackManager.onActivityResult(requestCode, resultCode, data)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // ----------------------- Inicializaciones -----------------------

    private fun initGoogleSignInLauncher() {
        googleSignInLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                handleGoogleSignInResult(result.data)
            } else {
                showSnackbar("Inicio de sesión cancelado o fallido")
            }
        }
    }

    private fun initFacebookLogin() {
        callbackManager = facebookSignInDataSource.getCallbackManager()

        binding.btnFacebookLogin.setOnClickListener {
            facebookSignInDataSource.login(requireActivity())
        }

        facebookSignInDataSource.registerCallback(
            onSuccess = { result ->
                handleFacebookLogin(result)
            },
            onCancel = {
                showSnackbar("Inicio de sesión de Facebook cancelado")
            },
            onError = {
                showSnackbar("Error de Facebook Login: ${it.message}")
            }
        )
    }

    // ----------------------- Listeners -----------------------

    private fun setupListeners() {
        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text?.toString()?.trim()
            if (email.isNullOrEmpty()) {
                showSnackbar("Por favor, ingresa un correo válido")
            } else {
                // TODO: Enviar a ViewModel para procesar login con email
            }
        }

        binding.btnGoogleSignIn.setOnClickListener {
            val intent = googleSignInClient.signInIntent
            googleSignInLauncher.launch(intent)
        }

        binding.tvForgotPassword.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_forgotPasswordFragment)
        }

        binding.ivBack.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    // ----------------------- ViewModel & Resultados -----------------------

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            loginViewModel.uiState.collect { state ->
                when {
                    state.isLoading -> {
                        // mostrar loader si quieres
                    }
                    state.isSuccess -> {
                        // findNavController().navigate(R.id.action_loginFragment_to_homeFragment)
                    }
                    !state.errorMessage.isNullOrEmpty() -> {
                        showSnackbar(state.errorMessage)
                    }
                }
            }
        }
    }

    private fun handleFacebookLogin(result: LoginResult) {
        lifecycleScope.launch {
            try {
                val token = facebookSignInDataSource.handleFacebookAccessToken(result)
                loginViewModel.loginWithFacebook(token)
            } catch (e: Exception) {
                showSnackbar("Error al obtener token de Facebook")
            }
        }
    }

    private fun handleGoogleSignInResult(data: Intent?) {
        val task = GoogleSignIn.getSignedInAccountFromIntent(data)
        try {
            val account = task.getResult(ApiException::class.java)
            val idToken = account?.idToken
            if (idToken != null) {
                loginViewModel.loginWithGoogle(idToken)
            } else {
                showSnackbar("ID token no disponible")
            }
        } catch (e: ApiException) {
            showSnackbar("Error en Sign-In: ${e.message}")
        }
    }

    // ----------------------- Utilidades -----------------------

    private fun showSnackbar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }
}
