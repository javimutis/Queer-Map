package com.cursoandroid.queermap.ui.welcome

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import com.cursoandroid.queermap.R
import com.cursoandroid.queermap.databinding.FragmentWelcomeBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest

@AndroidEntryPoint
class WelcomeFragment : Fragment() {

    private var _binding: FragmentWelcomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: WelcomeViewModel by viewModels()

    // Ciclo de vida del fragmento
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWelcomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(requireActivity().window, false)

        val window = requireActivity().window
        val controller = WindowInsetsControllerCompat(window, window.decorView)

        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        requireActivity().window.decorView.setBackgroundColor(
            ContextCompat.getColor(requireContext(), R.color.background_color)
        )

        observeUiState()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // Observación de estado
    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.uiState.collectLatest { shouldNavigate ->
                if (shouldNavigate) {
                    navigateToCover()
                    viewModel.onNavigated()
                }
            }
        }
    }

    // Navegación
    private fun navigateToCover() {
        findNavController().navigate(
            R.id.action_welcome_to_cover,
            null,
            navOptions {
                popUpTo(R.id.welcomeFragment) {
                    inclusive = true
                }
            }
        )
    }
}