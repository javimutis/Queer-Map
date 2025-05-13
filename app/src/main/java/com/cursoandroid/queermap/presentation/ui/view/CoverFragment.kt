package com.cursoandroid.queermap.presentation.ui.view

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
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

        showTitle()

        binding.coverLoginButton.setOnClickListener {
            findNavController().navigate(R.id.action_cover_to_login)
        }

        binding.coverSigninButton.setOnClickListener {
            findNavController().navigate(R.id.action_cover_to_signin)
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
