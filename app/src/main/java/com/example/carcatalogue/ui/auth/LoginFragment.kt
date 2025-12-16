package com.example.carcatalogue.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.carcatalogue.R
import com.example.carcatalogue.data.api.RetrofitClient
import com.example.carcatalogue.data.model.Result
import com.example.carcatalogue.data.preferences.TokenManager
import com.example.carcatalogue.databinding.FragmentLoginPremiumBinding
import kotlinx.coroutines.launch

class LoginFragment : Fragment() {
    
    private var _binding: FragmentLoginPremiumBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: AuthViewModel by viewModels()
    private lateinit var tokenManager: TokenManager
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginPremiumBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        tokenManager = TokenManager(requireContext())
        
        setupClickListeners()
        observeLoginResult()
    }
    
    private fun setupClickListeners() {
        binding.btnLogin.setOnClickListener {
            val username = binding.etUsername.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            
            if (validateInput(username, password)) {
                viewModel.login(username, password)
            }
        }
        
        binding.tvRegister.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
        }
    }
    
    private fun validateInput(username: String, password: String): Boolean {
        if (username.isEmpty()) {
            binding.tilUsername.error = getString(R.string.error_required_field)
            return false
        }
        
        if (password.isEmpty()) {
            binding.tilPassword.error = getString(R.string.error_required_field)
            return false
        }
        
        if (password.length < 6) {
            binding.tilPassword.error = getString(R.string.error_password_short)
            return false
        }
        
        binding.tilUsername.error = null
        binding.tilPassword.error = null
        return true
    }
    
    private fun observeLoginResult() {
        viewModel.loginResult.observe(viewLifecycleOwner) { result ->
            when (result) {
                is Result.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.btnLogin.isEnabled = false
                }
                is Result.Success -> {
                    binding.progressBar.visibility = View.GONE
                    binding.btnLogin.isEnabled = true
                    
                    // Login successful - token is set in cookies/headers by the server
                    // Save username and fetch profile
                    lifecycleScope.launch {
                        val username = binding.etUsername.text.toString().trim()
                        tokenManager.saveUserName(username)
                        
                        // Try to fetch full profile
                        try {
                            val profileResponse = RetrofitClient.apiService.getProfile()
                            if (profileResponse.isSuccessful && profileResponse.body() != null) {
                                val user = profileResponse.body()!!
                                val fullName = buildString {
                                    user.firstName?.let { append(it).append(" ") }
                                    user.lastName?.let { append(it) }
                                }.trim()
                                if (fullName.isNotEmpty()) {
                                    tokenManager.saveUserName(fullName)
                                }
                                tokenManager.saveUserEmail(user.email)
                            }
                        } catch (e: Exception) {
                            // Profile fetch failed, but login was successful
                            android.util.Log.e("LoginFragment", "Failed to fetch profile", e)
                        }
                        
                        Toast.makeText(requireContext(), "Вход выполнен успешно", Toast.LENGTH_SHORT).show()
                        findNavController().navigate(R.id.action_loginFragment_to_catalogueFragment)
                    }
                }
                is Result.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.btnLogin.isEnabled = true
                    Toast.makeText(requireContext(), result.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
