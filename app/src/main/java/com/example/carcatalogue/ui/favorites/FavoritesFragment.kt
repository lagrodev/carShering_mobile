package com.example.carcatalogue.ui.favorites

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.carcatalogue.R
import com.example.carcatalogue.databinding.FragmentFavoritesBinding
import com.example.carcatalogue.ui.catalogue.CarAdapter
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class FavoritesFragment : Fragment() {

    private var _binding: FragmentFavoritesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: FavoritesViewModel by viewModels()

    private val adapter = CarAdapter(
        onItemClick = { carId -> navigateToDetail(carId) },
        onFavoriteClick = { carId, isFavorite -> viewModel.toggleFavorite(carId, isFavorite) }
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFavoritesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupSwipeRefresh()
        setupEmptyState()
        observeViewModel()
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                when (state) {
                    is FavoritesUiState.Loading -> {
                        binding.loadingContainer.isVisible = true
                        binding.recyclerView.isVisible = false
                        binding.emptyState.isVisible = false
                    }
                    is FavoritesUiState.Success -> {
                        binding.loadingContainer.isVisible = false
                        binding.swipeRefresh.isRefreshing = false

                        if (state.cars.isEmpty()) {
                            binding.recyclerView.isVisible = false
                            binding.emptyState.isVisible = true
                        } else {
                            binding.recyclerView.isVisible = true
                            binding.emptyState.isVisible = false
                            adapter.submitList(state.cars)
                        }
                    }
                    is FavoritesUiState.Error -> {
                        binding.loadingContainer.isVisible = false
                        binding.swipeRefresh.isRefreshing = false
                        Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun setupRecyclerView() {
        val linearLayoutManager = LinearLayoutManager(context)
        binding.recyclerView.apply {
            layoutManager = linearLayoutManager
            adapter = this@FavoritesFragment.adapter

            // Infinite scroll
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)

                    val visibleItemCount = linearLayoutManager.childCount
                    val totalItemCount = linearLayoutManager.itemCount
                    val firstVisibleItemPosition = linearLayoutManager.findFirstVisibleItemPosition()

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
            viewModel.loadFavorites(refresh = true)
        }
        binding.swipeRefresh.setColorSchemeResources(R.color.primary)
    }

    private fun setupEmptyState() {
        binding.btnBrowseCatalogue.setOnClickListener {
            findNavController().navigate(R.id.catalogueFragment)
        }
    }

    private fun navigateToDetail(carId: Long) {
        val action = FavoritesFragmentDirections.actionFavoritesFragmentToCarDetailFragment(carId)
        findNavController().navigate(action)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
