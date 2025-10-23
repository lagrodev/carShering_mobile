package com.example.carcatalogue.ui.car_detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import coil.load
import com.example.carcatalogue.R
import com.example.carcatalogue.data.api.RetrofitClient
import com.example.carcatalogue.data.model.CarDetailResponse
import com.example.carcatalogue.databinding.FragmentCarDetailBinding
import kotlinx.coroutines.*

class CarDetailFragment : Fragment() {

    private var _binding: FragmentCarDetailBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCarDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val carId = arguments?.getLong("carId") ?: return

        loadCarDetails(carId)

        binding.rentButton.setOnClickListener {
            // TODO: Реализовать логику аренды
        }
    }

    private fun loadCarDetails(carId: Long) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val response = RetrofitClient.apiService.getCarDetail(carId)
                if (response.isSuccessful && response.body() != null) {
                    displayCar(response.body()!!)
                } else {
                    showError("Car not found")
                }
            } catch (e: Exception) {
                showError("Error loading car: ${e.message}")
            }
        }
    }

    private fun displayCar(car: CarDetailResponse) {
        binding.detailBrandModel.text = "${car.brand} ${car.model}"
        binding.detailBodyType.text = "Body: ${car.bodyType}"
        binding.detailClass.text = "Class: ${car.carClass}"
        binding.detailYear.text = "Year: ${car.yearOfIssue}"
        binding.detailRent.text = "${car.rent} ₽/day"
        binding.detailGosNumber.text = "Gos Number: ${car.gosNumber}"
        binding.detailVin.text = "VIN: ${car.vin}"
        binding.detailStatus.text = "Status: ${car.status}"

        binding.detailCarImage.load("https://via.placeholder.com/400x200?text=${car.brand}+${car.model}") {
            placeholder(R.drawable.ic_car_placeholder)
            error(R.drawable.ic_car_placeholder)
        }
    }

    private fun showError(message: String) {
        // Например, через Snackbar или Toast
        // Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}