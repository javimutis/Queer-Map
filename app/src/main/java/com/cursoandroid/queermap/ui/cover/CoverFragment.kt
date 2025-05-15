package com.cursoandroid.queermap.ui.cover

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.cursoandroid.queermap.R
import com.cursoandroid.queermap.databinding.FragmentCoverBinding

class CoverFragment : Fragment() {

    private var _binding: FragmentCoverBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCoverBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(0, 0, 0, systemBars.bottom)
            insets
        }
        @Suppress("DEPRECATION")
        requireActivity().window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE



        showTitle()

        binding.coverLoginButton.setOnClickListener {
            findNavController().navigate(R.id.action_cover_to_login)
        }

        binding.coverSigninButton.setOnClickListener {
//            findNavController().navigate(R.id.action_cover_to_signin)
        }
    }

    private fun showTitle() {
        binding.titleTextView.visibility = View.INVISIBLE

        Handler(Looper.getMainLooper()).postDelayed({
            val fadeIn = AnimationUtils.loadAnimation(requireContext(), android.R.anim.fade_in)
            binding.titleTextView.visibility = View.VISIBLE
            binding.titleTextView.startAnimation(fadeIn)
        }, 1300)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
