package com.example.carcatalogue.ui.catalogue

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.carcatalogue.R
import com.example.carcatalogue.databinding.FragmentCatalogueVibrantBinding
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest

class CatalogueFragment : Fragment() {

    private var _binding: FragmentCatalogueVibrantBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CatalogueViewModel by viewModels()

    private val adapter = CarAdapter { carId ->
        navigateToDetail(carId)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCatalogueVibrantBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupSwipeRefresh()
        setupFilters()
        observeViewModel()
    }

    private fun observeViewModel() {
        // Наблюдаем за состоянием UI
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                when (state) {
                    is CatalogueUiState.Loading -> {
                        binding.progressBar.isVisible = true
                        binding.rvCars.isVisible = false
                    }
                    is CatalogueUiState.Success -> {
                        binding.progressBar.isVisible = false
                        binding.swipeRefresh.isRefreshing = false
                        
                        if (state.cars.isEmpty()) {
                            binding.rvCars.isVisible = false
                            Toast.makeText(requireContext(), "Автомобили не найдены", Toast.LENGTH_SHORT).show()
                        } else {
                            binding.rvCars.isVisible = true
                            adapter.submitList(state.cars)
                        }
                    }
                    is CatalogueUiState.Error -> {
                        binding.progressBar.isVisible = false
                        binding.swipeRefresh.isRefreshing = false
                        Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun setupRecyclerView() {
        val linearLayoutManager = LinearLayoutManager(context)
        binding.rvCars.apply {
            layoutManager = linearLayoutManager
            adapter = this@CatalogueFragment.adapter
            
            // Добавляем слушатель для бесконечного скролла
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    
                    val visibleItemCount = linearLayoutManager.childCount
                    val totalItemCount = linearLayoutManager.itemCount
                    val firstVisibleItemPosition = linearLayoutManager.findFirstVisibleItemPosition()
                    
                    // Загружаем следующую страницу когда достигли конца списка
                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount - 5
                        && firstVisibleItemPosition >= 0) {
                        viewModel.loadNextPage()
                    }
                }
            })
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.loadCars()
        }
    }

    private fun setupFilters() {
        binding.btnFilters.setOnClickListener {
            // Показать bottom sheet с фильтрами
            showFiltersBottomSheet()
        }
    }

    private fun showFiltersBottomSheet() {
        val currentFilters = viewModel.getCurrentCarFilters()
        val bottomSheet = FiltersBottomSheet.newInstance(currentFilters) { filters ->
            viewModel.applyCarFilters(filters)
        }
        bottomSheet.show(parentFragmentManager, "filters")
    }

    private fun navigateToDetail(carId: Long) {
        val navController = findNavController()
        val action = CatalogueFragmentDirections.actionCatalogueFragmentToCarDetailFragment(carId)
        navController.navigate(action)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}