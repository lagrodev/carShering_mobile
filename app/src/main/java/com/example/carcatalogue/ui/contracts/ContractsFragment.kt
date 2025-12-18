package com.example.carcatalogue.ui.contracts

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.carcatalogue.R
import com.example.carcatalogue.data.api.RetrofitClient
import com.example.carcatalogue.databinding.FragmentContractsBinding
import kotlinx.coroutines.launch

class ContractsFragment : Fragment() {
    
    private var _binding: FragmentContractsBinding? = null
    private val binding get() = _binding!!
    
    private val adapter = ContractAdapter { contractId ->
        navigateToContractDetail(contractId)
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentContractsBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        setupSwipeRefresh()
        setupCta()
        loadContracts()
    }

    private fun setupCta() {
        binding.btnBrowseCars.setOnClickListener {
            findNavController().navigate(R.id.action_contractsFragment_to_catalogueFragment)
        }
        binding.btnBrowseCarsEmpty.setOnClickListener {
            findNavController().navigate(R.id.action_contractsFragment_to_catalogueFragment)
        }
    }
    
    private fun setupRecyclerView() {
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@ContractsFragment.adapter
        }
    }
    
    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            loadContracts()
        }
    }
    
    private fun loadContracts() {
        lifecycleScope.launch {
            try {
                Log.d("ContractsFragment", "Loading contracts")
                
                val response = RetrofitClient.apiService.getAllContracts(page = 0, size = 50)
                
                binding.swipeRefresh.isRefreshing = false
                
                Log.d("ContractsFragment", "Response code: ${response.code()}")
                
                if (response.isSuccessful) {
                    val pageResponse = response.body()
                    val contracts = pageResponse?.content ?: emptyList()
                    Log.d("ContractsFragment", "Loaded ${contracts.size} contracts")
                    
                    if (contracts.isEmpty()) {
                        showEmptyState()
                    } else {
                        binding.emptyState.visibility = View.GONE
                        binding.recyclerView.visibility = View.VISIBLE
                        adapter.submitList(contracts)
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e("ContractsFragment", "API Error: ${response.code()} - $errorBody")
                    if (response.code() == 401) {
                        Toast.makeText(requireContext(), "Нужно войти", Toast.LENGTH_SHORT).show()
                        findNavController().navigate(R.id.loginFragment)
                    } else {
                        Toast.makeText(requireContext(), "Ошибка загрузки контрактов: ${response.code()}", Toast.LENGTH_SHORT).show()
                        showEmptyState()
                    }
                }
            } catch (e: Exception) {
                binding.swipeRefresh.isRefreshing = false
                Log.e("ContractsFragment", "Failed to load contracts", e)
                Toast.makeText(requireContext(), "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
                showEmptyState()
            }
        }
    }
    
    private fun showEmptyState() {
        binding.emptyState.visibility = View.VISIBLE
        binding.recyclerView.visibility = View.GONE
    }
    
    private fun navigateToContractDetail(contractId: Long) {
        // TODO: Navigate to contract detail when created
        Toast.makeText(requireContext(), "Contract #$contractId", Toast.LENGTH_SHORT).show()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
