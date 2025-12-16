package com.example.carcatalogue.ui.auth

import android.os.Bundle
import android.util.Patterns
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
import com.example.carcatalogue.databinding.FragmentRegisterBinding
import kotlinx.coroutines.launch

class RegisterFragment : Fragment() {
    
    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: AuthViewModel by viewModels()
    private lateinit var tokenManager: TokenManager
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        tokenManager = TokenManager(requireContext())
        
        setupClickListeners()
        observeRegisterResult()
    }
    
    private fun setupClickListeners() {
        binding.btnRegister.setOnClickListener {
            val login = binding.etLogin.text.toString().trim()
            val lastName = binding.etLastName.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            val confirmPassword = binding.etConfirmPassword.text.toString().trim()
            
            if (validateInput(login, lastName, email, password, confirmPassword)) {
                viewModel.register(login, password, lastName, email)
            }
        }
        
        binding.tvLogin.setOnClickListener {
            findNavController().navigateUp()
        }
    }
    
    private fun validateInput(
        login: String,
        lastName: String,
        email: String,
        password: String,
        confirmPassword: String
    ): Boolean {
        var isValid = true
        
        if (login.isEmpty() || login.length < 6) {
            binding.tilLogin.error = "Логин должен содержать минимум 6 символов"
            isValid = false
        } else {
            binding.tilLogin.error = null
        }
        
        if (lastName.isEmpty()) {
            binding.tilLastName.error = getString(R.string.error_required_field)
            isValid = false
        } else {
            binding.tilLastName.error = null
        }
        
        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = getString(R.string.error_invalid_email)
            isValid = false
        } else {
            binding.tilEmail.error = null
        }
        
        if (password.isEmpty() || password.length < 6) {
            binding.tilPassword.error = getString(R.string.error_password_short)
            isValid = false
        } else {
            binding.tilPassword.error = null
        }
        
        if (password != confirmPassword) {
            binding.tilConfirmPassword.error = getString(R.string.error_password_mismatch)
            isValid = false
        } else {
            binding.tilConfirmPassword.error = null
        }
        
        return isValid
    }
    
    private fun observeRegisterResult() {
        viewModel.registerResult.observe(viewLifecycleOwner) { result ->
            when (result) {
                is Result.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.btnRegister.isEnabled = false
                }
                is Result.Success -> {
                    binding.progressBar.visibility = View.GONE
                    binding.btnRegister.isEnabled = true
                    
                    Toast.makeText(requireContext(), "Регистрация успешна! Войдите в систему", Toast.LENGTH_LONG).show()
                    findNavController().navigateUp()
                }
                is Result.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.btnRegister.isEnabled = true
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
