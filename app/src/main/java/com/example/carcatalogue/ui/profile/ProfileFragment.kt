package com.example.carcatalogue.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import androidx.core.view.WindowInsetsCompat
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

        applySystemInsets()
        
        loadUserProfile()
        setupClickListeners()
    }

    override fun onResume() {
        super.onResume()
        // If user edited profile or changed password, refresh.
        if (this::tokenManager.isInitialized) {
            loadUserProfile()
        }
    }

    private fun applySystemInsets() {
        val initialTop = binding.profileScroll.paddingTop
        ViewCompat.setOnApplyWindowInsetsListener(binding.profileScroll) { v, insets ->
            val status = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            v.updatePadding(top = initialTop + status.top)
            insets
        }
        ViewCompat.requestApplyInsets(binding.profileScroll)
    }
    
    private fun loadUserProfile() {
        lifecycleScope.launch {
            try {
                binding.progressBar.isVisible = true
                binding.tvDriverRating.text = "5.0"
                // First try to load from API
                val response = RetrofitClient.apiService.getProfile()
                
                if (response.isSuccessful && response.body() != null) {
                    val user = response.body()!!
                    val fullName = buildString {
                        user.firstName?.let { append(it).append(" ") }
                        user.lastName?.let { append(it) }
                    }.trim()
                    val displayName = fullName.ifEmpty { user.login }

                    binding.tvUserName.text = displayName
                    binding.tvUserEmail.text = user.email
                    binding.tvUserEmail.isVisible = !user.email.isNullOrBlank()
                    binding.tvUserInitials.text = initialsFrom(displayName)
                    
                    // Save to TokenManager for offline use
                    if (fullName.isNotEmpty()) {
                        tokenManager.saveUserName(fullName)
                    }
                    tokenManager.saveUserEmail(user.email)
                } else {
                    // Fallback to cached data
                    val userName = tokenManager.getUserName().first()
                    val userEmail = tokenManager.getUserEmail().first()
                    
                    val displayName = userName ?: "Гость"
                    binding.tvUserName.text = displayName
                    binding.tvUserEmail.text = userEmail ?: ""
                    binding.tvUserEmail.isVisible = !userEmail.isNullOrBlank()
                    binding.tvUserInitials.text = initialsFrom(displayName)
                }

                loadUserStats()
            } catch (e: Exception) {
                android.util.Log.e("ProfileFragment", "Failed to load profile", e)
                
                // Fallback to cached data
                val userName = tokenManager.getUserName().first()
                val userEmail = tokenManager.getUserEmail().first()
                
                val displayName = userName ?: "Гость"
                binding.tvUserName.text = displayName
                binding.tvUserEmail.text = userEmail ?: ""
                binding.tvUserEmail.isVisible = !userEmail.isNullOrBlank()
                binding.tvUserInitials.text = initialsFrom(displayName)

                loadUserStats()
                
                Toast.makeText(requireContext(), "Не удалось загрузить профиль", Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressBar.isVisible = false
            }
        }
    }

    private suspend fun loadUserStats() {
        try {
            val statsResponse = RetrofitClient.apiService.getOverviewStats()
            val stats = statsResponse.body()

            binding.tvStatsTotalRides.text = (stats?.totalRides ?: 0).toString()
            binding.tvStatsTotalSpent.text = formatRub(stats?.totalSpent ?: 0L)
            binding.tvStatsAvgDrive.text = formatHours(stats?.averageTimeDrive)
        } catch (e: Exception) {
            android.util.Log.w("ProfileFragment", "Failed to load stats", e)
            binding.tvStatsTotalRides.text = "0"
            binding.tvStatsTotalSpent.text = formatRub(0L)
            binding.tvStatsAvgDrive.text = formatHours(null)
        }
    }

    private fun formatRub(amount: Long): String {
        val spaced = amount.toString().reversed().chunked(3).joinToString(" ").reversed()
        return "$spaced ₽"
    }

    private fun formatHours(hours: Double?): String {
        val value = hours ?: 0.0
        val rounded = if (value % 1.0 == 0.0) value.toInt().toString() else String.format("%.1f", value)
        return "$rounded ч"
    }

    private fun initialsFrom(name: String): String {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return "?"

        val parts = trimmed
            .split(" ", "-", "_", "\t", "\n")
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        val first = parts.getOrNull(0)?.firstOrNull()
        val second = parts.getOrNull(1)?.firstOrNull()

        val initials = buildString {
            if (first != null) append(first)
            if (second != null) append(second)
        }

        return initials.ifEmpty { trimmed.take(2) }.uppercase()
    }
    
    private fun setupClickListeners() {
        binding.cardEditProfile.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_editProfileFragment)
        }
        
        binding.cardDocuments.setOnClickListener {
            Toast.makeText(requireContext(), "Documents - Coming soon", Toast.LENGTH_SHORT).show()
        }
        
        binding.cardChangePassword.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_changePasswordFragment)
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
