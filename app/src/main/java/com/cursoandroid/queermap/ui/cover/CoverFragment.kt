package com.cursoandroid.queermap.ui.cover

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.cursoandroid.queermap.R
import com.cursoandroid.queermap.databinding.FragmentCoverBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest

@AndroidEntryPoint
class CoverFragment : Fragment() {

    private var _binding: FragmentCoverBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CoverViewModel by viewModels()

    // Ciclo de vida del fragmento
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCoverBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        applyInsets()
        setupListeners()
        observeUiState()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // Configuración inicial
    private fun applyInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(0, 0, 0, systemBars.bottom)
            insets
        }
    }

    // Manejo de interacciones de UI
    private fun setupListeners() {
        binding.btnCoverLogin.setOnClickListener {
            viewModel.onLoginClicked()
        }

        binding.btnCoverSignIn.setOnClickListener {
            // viewModel.onSignInClicked()
        }
    }

    // Observación de estado
    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.uiState.collectLatest { state ->
                if (state.showTitle) showTitleAnimation()
                if (state.navigateToLogin) {
                    navigateToLogin()
                    viewModel.onNavigated()
                }
            }
        }
    }

    private fun navigateToLogin() {
        findNavController().navigate(R.id.action_cover_to_login)
    }

    // Utilidades
    private fun showTitleAnimation() {
        val fadeIn = AnimationUtils.loadAnimation(requireContext(), android.R.anim.fade_in)
        binding.tvTitle.visibility = View.VISIBLE
        binding.tvTitle.startAnimation(fadeIn)
    }
}