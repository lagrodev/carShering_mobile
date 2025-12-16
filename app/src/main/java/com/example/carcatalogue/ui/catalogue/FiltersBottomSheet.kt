package com.example.carcatalogue.ui.catalogue

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.example.carcatalogue.R
import com.example.carcatalogue.databinding.BottomSheetFiltersBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip
import kotlinx.coroutines.launch

class FiltersBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetFiltersBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CatalogueViewModel by activityViewModels()

    private var onFiltersApplied: ((CarFilters) -> Unit)? = null
    private var currentFilters: CarFilters? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetFiltersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        currentFilters = arguments?.getParcelable(ARG_FILTERS)

        setupYearSlider()
        setupPriceSlider()
        setupButtons()
        observeViewModel()

        applyCurrentFilters()
    }

    private fun observeViewModel() {
        // Загрузка классов автомобилей из API
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.carClasses.collect { classes ->
                binding.chipGroupClass.removeAllViews()
                classes.forEach { carClass ->
                    val chip = createChip(carClass)
                    binding.chipGroupClass.addView(chip)
                }
                // Восстановить выбранный класс если есть
                currentFilters?.carClass?.let { selectedClass ->
                    (0 until binding.chipGroupClass.childCount).forEach { i ->
                        val chip = binding.chipGroupClass.getChildAt(i) as? Chip
                        if (chip?.text == selectedClass) {
                            chip.isChecked = true
                        }
                    }
                }
            }
        }

        // Загрузка типов кузова из API
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.bodyTypes.collect { types ->
                binding.chipGroupBodyType.removeAllViews()
                types.forEach { bodyType ->
                    val chip = createChip(bodyType)
                    binding.chipGroupBodyType.addView(chip)
                }
                // Восстановить выбранный тип если есть
                currentFilters?.bodyType?.let { selectedType ->
                    (0 until binding.chipGroupBodyType.childCount).forEach { i ->
                        val chip = binding.chipGroupBodyType.getChildAt(i) as? Chip
                        if (chip?.text == selectedType) {
                            chip.isChecked = true
                        }
                    }
                }
            }
        }

        // Загрузка мин/макс цен из API
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.minPrice.collect { minPrice ->
                binding.sliderPrice.stepSize = 0f
                binding.sliderPrice.valueFrom = minPrice.toFloat()
                val currentValues = binding.sliderPrice.values
                if (currentValues[0] < minPrice) {
                    binding.sliderPrice.values = listOf(minPrice.toFloat(), currentValues[1])
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.maxPrice.collect { maxPrice ->
                binding.sliderPrice.stepSize = 0f
                binding.sliderPrice.valueTo = maxPrice.toFloat()
                val currentValues = binding.sliderPrice.values
                if (currentValues[1] > maxPrice) {
                    binding.sliderPrice.values = listOf(currentValues[0], maxPrice.toFloat())
                }
            }
        }
    }

    private fun createChip(text: String): Chip {
        return Chip(requireContext()).apply {
            this.text = text
            isCheckable = true
            setChipBackgroundColorResource(R.color.surface_variant)
            setTextColor(resources.getColor(R.color.text_primary, null))
        }
    }

    private fun setupYearSlider() {
        binding.sliderYear.apply {
            stepSize = 0f
            valueFrom = 2000f
            valueTo = 2025f
            values = listOf(2000f, 2025f)
        }

        binding.sliderYear.addOnChangeListener { _, _, _ ->
            val values = binding.sliderYear.values
            binding.etMinYear.setText(values[0].toInt().toString())
            binding.etMaxYear.setText(values[1].toInt().toString())
        }

        binding.etMinYear.addTextChangedListener {
            it?.toString()?.toIntOrNull()?.let { year ->
                if (year in 2000..2025) {
                    val values = binding.sliderYear.values
                    binding.sliderYear.values = listOf(year.toFloat(), values[1])
                }
            }
        }

        binding.etMaxYear.addTextChangedListener {
            it?.toString()?.toIntOrNull()?.let { year ->
                if (year in 2000..2025) {
                    val values = binding.sliderYear.values
                    binding.sliderYear.values = listOf(values[0], year.toFloat())
                }
            }
        }
    }

    private fun setupPriceSlider() {
        binding.sliderPrice.apply {
            stepSize = 0f
            valueFrom = 500f
            valueTo = 20000f
            values = listOf(500f, 20000f)
        }

        binding.sliderPrice.addOnChangeListener { _, _, _ ->
            val values = binding.sliderPrice.values
            binding.etMinPrice.setText(values[0].toInt().toString())
            binding.etMaxPrice.setText(values[1].toInt().toString())
        }

        binding.etMinPrice.addTextChangedListener {
            it?.toString()?.toIntOrNull()?.let { price ->
                val min = binding.sliderPrice.valueFrom.toInt()
                val max = binding.sliderPrice.valueTo.toInt()
                if (price in min..max) {
                    val values = binding.sliderPrice.values
                    binding.sliderPrice.values = listOf(price.toFloat(), values[1])
                }
            }
        }

        binding.etMaxPrice.addTextChangedListener {
            it?.toString()?.toIntOrNull()?.let { price ->
                val min = binding.sliderPrice.valueFrom.toInt()
                val max = binding.sliderPrice.valueTo.toInt()
                if (price in min..max) {
                    val values = binding.sliderPrice.values
                    binding.sliderPrice.values = listOf(values[0], price.toFloat())
                }
            }
        }
    }

    private fun setupButtons() {
        binding.btnApply.setOnClickListener {
            val filters = collectFilters()
            onFiltersApplied?.invoke(filters)
            dismiss()
        }

        binding.btnReset.setOnClickListener {
            resetFilters()
        }
    }

    private fun applyCurrentFilters() {
        currentFilters?.let { filters ->
            // Год
            if (filters.minYear != null && filters.maxYear != null) {
                binding.sliderYear.values = listOf(
                    filters.minYear.toFloat(),
                    filters.maxYear.toFloat()
                )
                binding.etMinYear.setText(filters.minYear.toString())
                binding.etMaxYear.setText(filters.maxYear.toString())
            }

            // Цена
            if (filters.minCell != null && filters.maxCell != null) {
                binding.sliderPrice.values = listOf(
                    filters.minCell.toFloat(),
                    filters.maxCell.toFloat()
                )
                binding.etMinPrice.setText(filters.minCell.toString())
                binding.etMaxPrice.setText(filters.maxCell.toString())
            }
        }
    }

    private fun collectFilters(): CarFilters {
        val checkedClassChipId = binding.chipGroupClass.checkedChipId
        val carClass = if (checkedClassChipId != View.NO_ID) {
            binding.chipGroupClass.findViewById<Chip>(checkedClassChipId)?.text?.toString()
        } else null

        val checkedBodyTypeChipId = binding.chipGroupBodyType.checkedChipId
        val bodyType = if (checkedBodyTypeChipId != View.NO_ID) {
            binding.chipGroupBodyType.findViewById<Chip>(checkedBodyTypeChipId)?.text?.toString()
        } else null

        val yearValues = binding.sliderYear.values
        val minYear = yearValues[0].toInt()
        val maxYear = yearValues[1].toInt()

        val priceValues = binding.sliderPrice.values
        val minCell = priceValues[0].toInt()
        val maxCell = priceValues[1].toInt()

        return CarFilters(
            brand = null,
            model = null,
            minYear = minYear.takeIf { it > 2000 },
            maxYear = maxYear.takeIf { it < 2025 },
            bodyType = bodyType,
            carClass = carClass,
            dateStart = null,
            dateEnd = null,
            minCell = minCell.takeIf { it > binding.sliderPrice.valueFrom.toInt() },
            maxCell = maxCell.takeIf { it < binding.sliderPrice.valueTo.toInt() }
        )
    }

    private fun resetFilters() {
        binding.chipGroupClass.clearCheck()
        binding.chipGroupBodyType.clearCheck()
        
        binding.sliderYear.values = listOf(2000f, 2025f)
        binding.etMinYear.setText("2000")
        binding.etMaxYear.setText("2025")
        
        val minPrice = binding.sliderPrice.valueFrom
        val maxPrice = binding.sliderPrice.valueTo
        binding.sliderPrice.values = listOf(minPrice, maxPrice)
        binding.etMinPrice.setText(minPrice.toInt().toString())
        binding.etMaxPrice.setText(maxPrice.toInt().toString())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_FILTERS = "filters"

        fun newInstance(
            currentFilters: CarFilters? = null,
            onFiltersApplied: (CarFilters) -> Unit
        ): FiltersBottomSheet {
            return FiltersBottomSheet().apply {
                arguments = bundleOf(ARG_FILTERS to currentFilters)
                this.onFiltersApplied = onFiltersApplied
            }
        }
    }
}
