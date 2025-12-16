package com.example.carcatalogue.ui.car_detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import coil.load
import com.example.carcatalogue.R
import com.example.carcatalogue.data.model.CarDetailResponse
import com.example.carcatalogue.databinding.FragmentCarDetailVibrantBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class CarDetailFragment : Fragment() {

    private var _binding: FragmentCarDetailVibrantBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CarDetailViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCarDetailVibrantBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val carId = arguments?.getLong("carId") ?: return

        setupToolbar()
        viewModel.loadCarDetails(carId)
        observeViewModel(carId)

        binding.btnBook.setOnClickListener {
            handleBooking(carId)
        }
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun observeViewModel(carId: Long) {
        // Наблюдаем за состоянием загрузки
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                when (state) {
                    is CarDetailUiState.Loading -> {
                        binding.progressBar.isVisible = true
                    }
                    is CarDetailUiState.Success -> {
                        binding.progressBar.isVisible = false
                        displayCar(state.car)
                    }
                    is CarDetailUiState.Error -> {
                        binding.progressBar.isVisible = false
                        showError(state.message)
                    }
                }
            }
        }

        // Наблюдаем за статусом избранного
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isFavorite.collectLatest { isFavorite ->
                val icon = if (isFavorite) {
                    R.drawable.ic_favorite
                } else {
                    R.drawable.ic_favorite_border
                }
                // binding.fabFavorite.setImageResource(icon)
            }
        }
    }

    private fun handleBooking(carId: Long) {
        // TODO: Проверить даты и создать контракт
        val contractRequest = viewModel.createContract(carId, 5000.0) // Временно
        if (contractRequest != null) {
            // Создать контракт через API
        } else {
            showError("Выберите даты аренды")
        }
    }

    private fun loadCarDetails(carId: Long) {
        // Метод больше не нужен - используется ViewModel
    }

    private fun displayCar(car: CarDetailResponse) {
        binding.tvCarName.text = "${car.brand} ${car.model}"
        binding.tvYear.text = "${car.yearOfIssue}"
        binding.tvCarClass.text = car.carClass
        binding.tvBodyType.text = car.bodyType
        binding.tvGosNumber.text = car.gosNumber
        binding.tvPrice.text = String.format("%.0f", car.rent)
        
        binding.collapsingToolbar.title = "${car.brand} ${car.model}"

        binding.ivCarImage.load("https://via.placeholder.com/400x200?text=${car.brand}+${car.model}") {
            crossfade(true)
            error(R.drawable.ic_car)
        }
    }

    private fun showError(message: String) {
        android.widget.Toast.makeText(requireContext(), message, android.widget.Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}