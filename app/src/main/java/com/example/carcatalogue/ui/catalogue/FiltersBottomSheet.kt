package com.example.carcatalogue.ui.catalogue

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.example.carcatalogue.R
import com.example.carcatalogue.databinding.BottomSheetFiltersBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import android.content.res.ColorStateList

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

    override fun onStart() {
        super.onStart()
        val dialog = dialog as? BottomSheetDialog ?: return
        val bottomSheet = dialog.findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet) ?: return
        val behavior = BottomSheetBehavior.from(bottomSheet)

        // Make sure the sheet can actually grow: if its container stays WRAP_CONTENT,
        // some devices/layouts end up showing only the bottom-most view.
        bottomSheet.layoutParams = bottomSheet.layoutParams.apply {
            height = ViewGroup.LayoutParams.MATCH_PARENT
        }

        behavior.isFitToContents = true
        behavior.skipCollapsed = true
        behavior.peekHeight = resources.displayMetrics.heightPixels
        behavior.state = BottomSheetBehavior.STATE_EXPANDED
    }

    private fun observeViewModel() {
        // Загрузка брендов  из API
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.brands.collect { brands ->
                binding.chipGroupBrand.removeAllViews()
                brands.forEach { brand ->
                    val chip = createChip(brand)
                    binding.chipGroupBrand.addView(chip)
                }
                // Восстановить выбранные бренды если есть
                val selectedBrands = currentFilters?.brands.orEmpty().toSet()
                if (selectedBrands.isNotEmpty()) {
                    (0 until binding.chipGroupBrand.childCount).forEach { i ->
                        val chip = binding.chipGroupBrand.getChildAt(i) as? Chip
                        chip?.isChecked = chip?.text?.toString() in selectedBrands
                    }
                }
            }
        }


        // Загрузка классов автомобилей из API
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.carClasses.collect { classes ->
                binding.chipGroupClass.removeAllViews()
                classes.forEach { carClass ->
                    val chip = createChip(carClass)
                    binding.chipGroupClass.addView(chip)
                }
                // Восстановить выбранные классы если есть
                val selectedClasses = currentFilters?.carClasses.orEmpty().toSet()
                if (selectedClasses.isNotEmpty()) {
                    (0 until binding.chipGroupClass.childCount).forEach { i ->
                        val chip = binding.chipGroupClass.getChildAt(i) as? Chip
                        chip?.isChecked = chip?.text?.toString() in selectedClasses
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

        // Загрузка мин/макс цен из API - АТОМАРНО в одном потоке
        viewLifecycleOwner.lifecycleScope.launch {
            combine(viewModel.minPrice, viewModel.maxPrice) { min, max ->
                Pair(min, max)
            }.collect { (minPrice, maxPrice) ->
                try {
                    // Округляем до целых чисел чтобы избежать проблем с float precision
                    val newMin = kotlin.math.floor(minPrice.toDouble()).toFloat()
                    val newMax = kotlin.math.ceil(maxPrice.toDouble()).toFloat()
                    
                    // Проверяем что границы валидны
                    if (newMin >= newMax || newMax - newMin < 10f) {
                        return@collect
                    }
                    
                    // Обновляем slider атомарно в UI потоке через post
                    binding.sliderPrice.post {
                        try {
                            val currentValues = binding.sliderPrice.values
                            
                            // Корректируем текущие значения - округляем чтобы точно были внутри границ
                            val correctedMinValue = kotlin.math.ceil(currentValues[0].coerceIn(newMin, newMax - 1f).toDouble()).toFloat()
                            val correctedMaxValue = kotlin.math.floor(currentValues[1].coerceIn(newMin + 1f, newMax).toDouble()).toFloat()
                            
                            // Убеждаемся что значения валидны
                            val finalMin = correctedMinValue.coerceAtMost(correctedMaxValue - 1f)
                            val finalMax = correctedMaxValue.coerceAtLeast(finalMin + 1f)
                            
                            // АТОМАРНО устанавливаем ВСЕ параметры
                            binding.sliderPrice.clearOnChangeListeners()
                            binding.sliderPrice.valueFrom = newMin
                            binding.sliderPrice.valueTo = newMax
                            binding.sliderPrice.values = listOf(finalMin, finalMax)
                            
                            // Восстанавливаем listener
                            binding.sliderPrice.addOnChangeListener { _, _, _ ->
                                try {
                                    val values = binding.sliderPrice.values
                                    binding.etMinPrice.setText(values[0].toInt().toString())
                                    binding.etMaxPrice.setText(values[1].toInt().toString())
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                            
                            // Обновляем текстовые поля
                            binding.etMinPrice.setText(finalMin.toInt().toString())
                            binding.etMaxPrice.setText(finalMax.toInt().toString())
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun createChip(text: String): Chip {
        val context = requireContext()
        val checkedBackground = ContextCompat.getColor(context, R.color.primary_container)
        val uncheckedBackground = ContextCompat.getColor(context, R.color.surface_variant)
        val checkedText = ContextCompat.getColor(context, R.color.on_primary_container)
        val uncheckedText = ContextCompat.getColor(context, R.color.text_primary)
        val checkedStroke = ContextCompat.getColor(context, R.color.primary)
        val uncheckedStroke = ContextCompat.getColor(context, R.color.divider)

        val checkedState = intArrayOf(android.R.attr.state_checked)
        val defaultState = intArrayOf()

        val backgroundColors = ColorStateList(
            arrayOf(checkedState, defaultState),
            intArrayOf(checkedBackground, uncheckedBackground)
        )
        val textColors = ColorStateList(
            arrayOf(checkedState, defaultState),
            intArrayOf(checkedText, uncheckedText)
        )
        val strokeColors = ColorStateList(
            arrayOf(checkedState, defaultState),
            intArrayOf(checkedStroke, uncheckedStroke)
        )

        val strokeWidthPx = 1f * resources.displayMetrics.density

        return Chip(context, null, com.google.android.material.R.style.Widget_Material3_Chip_Filter).apply {
            this.text = text
            isCheckable = true
            isClickable = true
            isFocusable = true
            chipBackgroundColor = backgroundColors
            setTextColor(textColors)
            chipStrokeColor = strokeColors
            chipStrokeWidth = strokeWidthPx
            isCheckedIconVisible = true
            setCheckedIconResource(R.drawable.ic_check)
            checkedIconTint = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.primary))
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
            try {
                val values = binding.sliderYear.values
                binding.etMinYear.setText(values[0].toInt().toString())
                binding.etMaxYear.setText(values[1].toInt().toString())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        binding.etMinYear.addTextChangedListener {
            it?.toString()?.toIntOrNull()?.let { year ->
                try {
                    val values = binding.sliderYear.values
                    val newMin = year.toFloat().coerceIn(2000f, values[1] - 1f)
                    
                    if (year in 2000..2025 && newMin < values[1]) {
                        binding.sliderYear.values = listOf(newMin, values[1])
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        binding.etMaxYear.addTextChangedListener {
            it?.toString()?.toIntOrNull()?.let { year ->
                try {
                    val values = binding.sliderYear.values
                    val newMax = year.toFloat().coerceIn(values[0] + 1f, 2025f)
                    
                    if (year in 2000..2025 && newMax > values[0]) {
                        binding.sliderYear.values = listOf(values[0], newMax)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun setupPriceSlider() {
        binding.sliderPrice.apply {
            stepSize = 0f
            // Начальные широкие границы
            valueFrom = 0f
            valueTo = 100000f
            values = listOf(500f, 20000f)
        }

        binding.sliderPrice.addOnChangeListener { _, _, _ ->
            try {
                val values = binding.sliderPrice.values
                binding.etMinPrice.setText(values[0].toInt().toString())
                binding.etMaxPrice.setText(values[1].toInt().toString())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        binding.etMinPrice.addTextChangedListener {
            it?.toString()?.toIntOrNull()?.let { price ->
                try {
                    val min = binding.sliderPrice.valueFrom.toInt()
                    val max = binding.sliderPrice.valueTo.toInt()
                    val values = binding.sliderPrice.values
                    val newMin = price.toFloat().coerceIn(min.toFloat(), values[1] - 1f)
                    
                    if (price in min..max && newMin < values[1]) {
                        binding.sliderPrice.values = listOf(newMin, values[1])
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        binding.etMaxPrice.addTextChangedListener {
            it?.toString()?.toIntOrNull()?.let { price ->
                try {
                    val min = binding.sliderPrice.valueFrom.toInt()
                    val max = binding.sliderPrice.valueTo.toInt()
                    val values = binding.sliderPrice.values
                    val newMax = price.toFloat().coerceIn(values[0] + 1f, max.toFloat())
                    
                    if (price in min..max && newMax > values[0]) {
                        binding.sliderPrice.values = listOf(values[0], newMax)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
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
        val selectedClasses = binding.chipGroupClass.checkedChipIds
            .mapNotNull { id -> binding.chipGroupClass.findViewById<Chip>(id)?.text?.toString() }
            .distinct()

        val checkedBodyTypeChipId = binding.chipGroupBodyType.checkedChipId
        val bodyType = if (checkedBodyTypeChipId != View.NO_ID) {
            binding.chipGroupBodyType.findViewById<Chip>(checkedBodyTypeChipId)?.text?.toString()
        } else null

        val selectedBrands = binding.chipGroupBrand.checkedChipIds
            .mapNotNull { id -> binding.chipGroupBrand.findViewById<Chip>(id)?.text?.toString() }
            .distinct()

        val yearValues = binding.sliderYear.values
        val minYear = yearValues[0].toInt()
        val maxYear = yearValues[1].toInt()

        val priceValues = binding.sliderPrice.values
        val minCell = priceValues[0].toInt()
        val maxCell = priceValues[1].toInt()

        return CarFilters(
            brands = selectedBrands,
            model = null,
            minYear = minYear.takeIf { it > 2000 },
            maxYear = maxYear.takeIf { it < 2025 },
            bodyType = bodyType,
            carClasses = selectedClasses,
            dateStart = null,
            dateEnd = null,
            minCell = minCell.takeIf { it > binding.sliderPrice.valueFrom.toInt() },
            maxCell = maxCell.takeIf { it < binding.sliderPrice.valueTo.toInt() }
        )
    }

    private fun resetFilters() {
        binding.chipGroupClass.clearCheck()
        binding.chipGroupBodyType.clearCheck()
        binding.chipGroupBrand.clearCheck()
        
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
