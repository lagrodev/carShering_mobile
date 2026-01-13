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
import com.example.carcatalogue.data.model.UpdateProfileRequest
import com.example.carcatalogue.data.preferences.TokenManager
import com.example.carcatalogue.data.api.RetrofitClient
import com.example.carcatalogue.databinding.FragmentEditProfileBinding
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class EditProfileFragment : Fragment() {

    private var _binding: FragmentEditProfileBinding? = null
    private val binding get() = _binding!!

    private lateinit var tokenManager: TokenManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tokenManager = TokenManager(requireContext())

        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnSave.setOnClickListener {
            saveProfile()
        }

        loadProfile()
    }

    private fun loadProfile() {
        lifecycleScope.launch {
            binding.progressBar.isVisible = true
            try {
                val response = RetrofitClient.apiService.getProfile()
                if (response.isSuccessful) {
                    val user = response.body()
                    binding.etFirstName.setText(user?.firstName.orEmpty())
                    binding.etLastName.setText(user?.lastName.orEmpty())
                    binding.etPhone.setText(user?.phone.orEmpty())
                    return@launch
                }

                // Fallback: cached full name + email (phone not cached)
                val cachedName = tokenManager.getUserName().first().orEmpty()
                val parts = cachedName.trim().split(" ").filter { it.isNotBlank() }
                binding.etFirstName.setText(parts.getOrNull(0).orEmpty())
                binding.etLastName.setText(parts.drop(1).joinToString(" "))
                binding.etPhone.setText("")
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Не удалось загрузить профиль", Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressBar.isVisible = false
            }
        }
    }

    private fun saveProfile() {
        val firstName = binding.etFirstName.text?.toString()?.trim().orEmpty().ifBlank { null }
        val lastName = binding.etLastName.text?.toString()?.trim().orEmpty().ifBlank { null }
        val phone = binding.etPhone.text?.toString()?.trim().orEmpty().ifBlank { null }

        binding.tilFirstName.error = null
        binding.tilLastName.error = null
        binding.tilPhone.error = null

        lifecycleScope.launch {
            binding.progressBar.isVisible = true
            binding.btnSave.isEnabled = false
            try {
                val response = RetrofitClient.apiService.updateProfile(
                    UpdateProfileRequest(firstName = firstName, lastName = lastName, phone = phone)
                )

                if (response.isSuccessful) {
                    val displayName = listOfNotNull(firstName, lastName).joinToString(" ").trim()
                    if (displayName.isNotEmpty()) {
                        tokenManager.saveUserName(displayName)
                    }

                    Toast.makeText(requireContext(), "Профиль обновлён", Toast.LENGTH_SHORT).show()
                    findNavController().navigateUp()
                } else {
                    Toast.makeText(requireContext(), "Не удалось сохранить профиль", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Не удалось сохранить профиль", Toast.LENGTH_SHORT).show()
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
