package com.example.carcatalogue.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.carcatalogue.R
import com.example.carcatalogue.data.api.RetrofitClient
import com.example.carcatalogue.data.model.ChangePasswordRequest
import com.example.carcatalogue.databinding.FragmentChangePasswordBinding
import kotlinx.coroutines.launch

class ChangePasswordFragment : Fragment() {

    private var _binding: FragmentChangePasswordBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChangePasswordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnSave.setOnClickListener {
            submit()
        }
    }

    private fun submit() {
        val oldPassword = binding.etOldPassword.text?.toString().orEmpty()
        val newPassword = binding.etNewPassword.text?.toString().orEmpty()
        val confirm = binding.etConfirmPassword.text?.toString().orEmpty()

        binding.tilOldPassword.error = null
        binding.tilNewPassword.error = null
        binding.tilConfirmPassword.error = null

        var hasError = false
        if (oldPassword.isBlank()) {
            binding.tilOldPassword.error = getString(R.string.error_required_field)
            hasError = true
        }
        if (newPassword.length < 6) {
            binding.tilNewPassword.error = getString(R.string.error_password_short)
            hasError = true
        }
        if (newPassword != confirm) {
            binding.tilConfirmPassword.error = getString(R.string.error_password_mismatch)
            hasError = true
        }
        if (hasError) return

        lifecycleScope.launch {
            binding.progressBar.isVisible = true
            binding.btnSave.isEnabled = false
            try {
                val response = RetrofitClient.apiService.changePassword(
                    ChangePasswordRequest(oldPassword = oldPassword, newPassword = newPassword)
                )

                if (response.isSuccessful) {
                    Toast.makeText(requireContext(), "Пароль изменён", Toast.LENGTH_SHORT).show()
                    findNavController().navigateUp()
                } else {
                    Toast.makeText(requireContext(), "Не удалось изменить пароль", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Не удалось изменить пароль", Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressBar.isVisible = false
                binding.btnSave.isEnabled = true
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
