package com.example.carcatalogue.ui.catalogue

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.carcatalogue.R
import com.example.carcatalogue.data.api.RetrofitClient
import com.example.carcatalogue.data.model.CarListItemResponse
import com.example.carcatalogue.databinding.FragmentCatalogueBinding
import kotlinx.coroutines.*
import retrofit2.HttpException
import kotlin.collections.map

class CatalogueFragment : Fragment() {

    private var _binding: FragmentCatalogueBinding? = null
    private val binding get() = _binding!!

    private val adapter = CarAdapter { carId ->
        navigateToDetail(carId)
    }

    private var currentFilter: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCatalogueBinding.inflate(inflater, container, false)
        return binding.root
    }
    private lateinit var filterButtons: MutableList<Button>

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        filterButtons = mutableListOf()

        setupRecyclerView()
        loadBrands() // ← загружаем бренды вместо статических фильтров
        loadCars(null)

        binding.searchInput.setOnEditorActionListener { _, _, _ ->
            val query = binding.searchInput.text.toString().trim()
            loadCars(if (query.isEmpty()) null else query)
            true
        }
    }



    private fun loadBrands() {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val response = RetrofitClient.apiService.getBrands(page = 0, size = 100)
                if (response.isSuccessful && response.body() != null) {
                    val brands = response.body()!!.content.map { it.brand }.distinct()
                    renderBrandFilters(brands)
                }
            } catch (e: Exception) {
                Log.e("API", "Failed to load brands", e)
                // Можно показать ошибку или оставить пустым
            }
        }
    }

    private fun renderBrandFilters(brands: List<String>) {
        val context = requireContext()
        val container = binding.filterContainer
        container.removeAllViews()
        filterButtons.clear()

        // Кнопка "All"
        val allButton = Button(context).apply {
            text = "All"
            setBackgroundResource(R.drawable.rounded_input)
            backgroundTintList = ContextCompat.getColorStateList(context, R.color.blue)
            setTextColor(ContextCompat.getColor(context, R.color.white))
            setOnClickListener {
                selectBrandFilter(null)
                updateSelectedButton(this)
            }
        }
        container.addView(allButton)
        filterButtons.add(allButton)

        // Кнопки брендов
        brands.forEach { brand ->
            val button = Button(context).apply {
                text = brand
                setBackgroundResource(R.drawable.rounded_input)
                backgroundTintList = ContextCompat.getColorStateList(context, R.color.white)
                setTextColor(ContextCompat.getColor(context, R.color.black))
                setOnClickListener {
                    selectBrandFilter(brand)
                    updateSelectedButton(this)
                }
            }
            container.addView(button)
            filterButtons.add(button)
        }

        // По умолчанию выбран "All"
        selectBrandFilter(null)
        updateSelectedButton(allButton)
    }

    private var selectedBrand: String? = null

    private fun selectBrandFilter(brand: String?) {
        selectedBrand = brand
        loadCars(searchQuery = binding.searchInput.text.toString().trim().takeIf { it.isNotEmpty() }, brand = brand)
    }

    private fun updateSelectedButton(selected: Button) {
        val context = requireContext()
        filterButtons.forEach { btn ->
            if (btn == selected) {
                btn.backgroundTintList = ContextCompat.getColorStateList(context, R.color.blue)
                btn.setTextColor(ContextCompat.getColor(context, R.color.white))
            } else {
                btn.backgroundTintList = ContextCompat.getColorStateList(context, R.color.white)
                btn.setTextColor(ContextCompat.getColor(context, R.color.black))
            }
        }
    }
    private fun setupRecyclerView() {
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = this@CatalogueFragment.adapter
        }
    }



    private fun updateFilterButtons(selected: View, allButtons: List<Pair<View, String?>>) {
        allButtons.forEach { (button, _) ->
            button.setBackgroundResource(R.drawable.rounded_input)
        }
        selected.setBackgroundResource(R.drawable.rounded_input)
        selected.backgroundTintList = resources.getColorStateList(R.color.blue)
    }

    private fun loadCars(searchQuery: String? = null, brand: String? = null) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val response = RetrofitClient.apiService.getCatalogue(
                    brand = brand,
                    model = searchQuery?.takeIf { it.isNotEmpty() },
                    page = 0,
                    size = 20
                )
                if (response.isSuccessful && response.body() != null) {
                    val cars = response.body()!!.content
                    adapter.submitList(cars)
                } else {
                    Toast.makeText(requireContext(), "No cars found", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("API", "Failed to load cars", e)
                Toast.makeText(requireContext(), "Network error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showErrorMessage(message: String) {
        // Можно использовать Snackbar или Toast
        // Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
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