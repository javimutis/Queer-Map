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
    // Inyecta directamente tus mocks a través de este constructor de prueba
    // Hilt aún puede usar el constructor sin argumentos si lo configuras.
    // O puedes usar @Inject constructor() para que Hilt lo use por defecto.
    private val googleSignInDataSource: GoogleSignInDataSource? = null,
    private val facebookSignInDataSource: FacebookSignInDataSource? = null,
    private val googleSignInLauncher: ActivityResultLauncher<Intent>? = null,
    private val callbackManager: CallbackManager? = null
) : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding

    private val viewModel: LoginViewModel by viewModels()

    // Si estás usando el constructor de prueba, estas propiedades ahora vienen de ahí.
    // Si Hilt las inyecta, se hará a través de los @Inject regulares (que Hilt usa para el constructor sin args)
    // Para el entorno de producción (cuando no se usa el constructor de prueba), Hilt inyectará las dependencias
    // a través del constructor por defecto (o un constructor @Inject).
    // Si GoogleSignInDataSource y FacebookSignInDataSource son clases que Hilt provee,
    // puedes mantener las propiedades @Inject lateinit var y usar la lógica de fallback,
    // pero la forma más limpia para tests es inyectar directamente si se puede.
    //
    // Para simplificar, vamos a asumir que para tests, SIEMPRE USAMOS EL CONSTRUCTOR DE TEST.
    // En producción, Hilt seguirá inyectando a través del constructor (si añades @Inject constructor())
    // o a través de las propiedades con @Inject.
    //
    // La forma más robusta es que estas dependencias sean inyectadas en el constructor de la clase
    // y luego Hilt las provea. Si ya las tienes con @Inject lateinit var,
    // entonces la `get()` lógica debe ser muy cuidadosa.

    // **OPCIÓN MÁS SEGURA Y LIMPIA PARA ELIMINAR `lateinit var` EN TESTS**
    // Pasa las dependencias como parámetros del constructor principal del fragmento.
    // Esto es lo que Hilt preferiría si pudieras hacer inyección al constructor de un Fragment.
    // Como no es nativo de Hilt para Fragmentos, usamos la FragmentFactory.

    // Esto hará que tus getters dependan de si los parámetros del constructor de prueba son nulos o no.
    // En el caso de Hilt en producción, estos parámetros serían nulos, y entonces se usarían los @Inject
    // (si los mantienes). O simplemente Hilt manejaría estas inyecciones si no usas el constructor de prueba.

    // Si quieres mantener el patrón actual de `testDataSource ?: _dataSource`,
    // entonces las `_dataSource` deben seguir siendo `lateinit var @Inject`.
    // Pero el problema es que `_googleSignInDataSource` no está inicializada cuando se accede.

    // Vamos a la solución más agresiva para el test, **cambiando la forma en que se accede a ellas.**
    // Si los `testGoogleSignInDataSource` y `testFacebookSignInDataSource` son siempre no nulos en el test,
    // no hay necesidad de `_googleSignInDataSource`.

    // SOLUCIÓN FINAL: Haz que el constructor de prueba provea las dependencias necesarias.
    // Y en el fragmento, si no se proveen por el constructor, que use las inyectadas por Hilt.

    // Mantener las propiedades @Inject para la producción normal de Hilt
    @Inject internal lateinit var _googleSignInDataSource: GoogleSignInDataSource
    @Inject internal lateinit var _facebookSignInDataSource: FacebookSignInDataSource

    // Ajusta los getters para que usen las propiedades del constructor de prueba si están disponibles,
    // de lo contrario, las inyectadas por Hilt.
    internal val actualGoogleSignInDataSource: GoogleSignInDataSource
        get() = googleSignInDataSource ?: _googleSignInDataSource

    internal val actualFacebookSignInDataSource: FacebookSignInDataSource
        get() = facebookSignInDataSource ?: _facebookSignInDataSource


    // Mantener esto para tu lógica de test que interactúa con el launcher.
    internal var testGoogleSignInLauncher: ActivityResultLauncher<Intent>? = null

    // Mantener esto para tu lógica de test que interactúa con el callbackManager.
    internal var testCallbackManager: CallbackManager? = null


    // NUEVAS PROPIEDADES PARA CONTROLAR EL LOGGING EN TESTS
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal var testLogHelper: ((String, String) -> Unit)? = null
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal var testLogEHelper: ((String, String, Throwable?) -> Unit)? = null
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal var testLogWHelper: ((String, String) -> Unit)? = null


    private val actualCallbackManager: CallbackManager by lazy {
        callbackManager ?: CallbackManager.Factory.create()
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

        val currentGoogleSignInLauncher = googleSignInLauncher ?: registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                handleGoogleSignInResult(result.data)
            } else {
                showSnackbar("Inicio de sesión cancelado")
            }
        }
        // Asigna el launcher para que la prueba pueda accederlo si es necesario
        this.testGoogleSignInLauncher = currentGoogleSignInLauncher // <-- Importante para el test

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
        actualFacebookSignInDataSource.registerCallback( // Usar `actualFacebookSignInDataSource`
            actualCallbackManager, // Usar `actualCallbackManager`
            object : FacebookCallback<LoginResult> {
                override fun onSuccess(result: LoginResult) {
                    logD("LoginFragment", "Facebook Login Success: ${result.accessToken.token}")
                    lifecycleScope.launch {
                        viewModel.loginWithFacebook(result.accessToken.token)
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
            actualFacebookSignInDataSource.logInWithReadPermissions( // Usar `actualFacebookSignInDataSource`
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
            googleSignInLauncher?.launch(actualGoogleSignInDataSource.getSignInIntent()) // Usar actualGoogleSignInDataSource
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
                actualGoogleSignInDataSource.handleSignInResult(data) // Usar actualGoogleSignInDataSource
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