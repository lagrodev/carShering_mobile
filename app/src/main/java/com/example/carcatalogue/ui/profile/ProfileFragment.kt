package com.example.carcatalogue.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.carcatalogue.R
import com.example.carcatalogue.data.api.RetrofitClient
import com.example.carcatalogue.data.preferences.TokenManager
import com.example.carcatalogue.databinding.FragmentProfileBinding
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ProfileFragment : Fragment() {
    
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var tokenManager: TokenManager
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        tokenManager = TokenManager(requireContext())
        
        loadUserProfile()
        setupClickListeners()
    }
    
    private fun loadUserProfile() {
        lifecycleScope.launch {
            try {
                // First try to load from API
                val response = RetrofitClient.apiService.getProfile()
                
                if (response.isSuccessful && response.body() != null) {
                    val user = response.body()!!
                    val fullName = buildString {
                        user.firstName?.let { append(it).append(" ") }
                        user.lastName?.let { append(it) }
                    }.trim()
                    binding.tvUserName.text = fullName.ifEmpty { user.login }
                    binding.tvUserEmail.text = user.email
                    
                    // Save to TokenManager for offline use
                    if (fullName.isNotEmpty()) {
                        tokenManager.saveUserName(fullName)
                    }
                    tokenManager.saveUserEmail(user.email)
                } else {
                    // Fallback to cached data
                    val userName = tokenManager.getUserName().first()
                    val userEmail = tokenManager.getUserEmail().first()
                    
                    binding.tvUserName.text = userName ?: "Гость"
                    binding.tvUserEmail.text = userEmail ?: ""
                }
            } catch (e: Exception) {
                android.util.Log.e("ProfileFragment", "Failed to load profile", e)
                
                // Fallback to cached data
                val userName = tokenManager.getUserName().first()
                val userEmail = tokenManager.getUserEmail().first()
                
                binding.tvUserName.text = userName ?: "Гость"
                binding.tvUserEmail.text = userEmail ?: ""
                
                Toast.makeText(requireContext(), "Не удалось загрузить профиль", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun setupClickListeners() {
        binding.cardEditProfile.setOnClickListener {
            Toast.makeText(requireContext(), "Edit Profile - Coming soon", Toast.LENGTH_SHORT).show()
        }
        
        binding.cardDocuments.setOnClickListener {
            Toast.makeText(requireContext(), "Documents - Coming soon", Toast.LENGTH_SHORT).show()
        }
        
        binding.cardChangePassword.setOnClickListener {
            Toast.makeText(requireContext(), "Change Password - Coming soon", Toast.LENGTH_SHORT).show()
        }
        
        binding.btnLogout.setOnClickListener {
            logout()
        }
    }
    
    private fun logout() {
        lifecycleScope.launch {
            tokenManager.clearToken()
            RetrofitClient.authToken = null
            
            Toast.makeText(requireContext(), "Вы вышли из системы", Toast.LENGTH_SHORT).show()
            findNavController().navigate(R.id.action_profileFragment_to_loginFragment)
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
