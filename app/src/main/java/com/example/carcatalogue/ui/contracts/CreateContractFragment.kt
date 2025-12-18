package com.example.carcatalogue.ui.contracts

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.carcatalogue.R
import com.example.carcatalogue.data.api.RetrofitClient
import com.example.carcatalogue.data.model.CreateContractRequest
import com.example.carcatalogue.databinding.FragmentCreateContractBinding
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.time.format.DateTimeFormatter

class CreateContractFragment : Fragment() {
    
    private var _binding: FragmentCreateContractBinding? = null
    private val binding get() = _binding!!

    private val dateUiFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
    private val timeUiFormatter = DateTimeFormatter.ofPattern("HH:mm")

    private var selectedStartDate: LocalDate? = null
    private var selectedEndDate: LocalDate? = null
    private var selectedStartTime: LocalTime = LocalTime.of(10, 0)
    private var selectedEndTime: LocalTime = LocalTime.of(10, 0)
    private var dailyRate: Double = 0.0
    private var carId: Long = 0L
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreateContractBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        carId = arguments?.getLong("carId") ?: 0L
        dailyRate = (arguments?.getFloat("dailyRate") ?: 0f).toDouble()

        binding.toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        binding.tvCarId.text = "Авто #$carId"
        binding.tvDailyRate.text = "${String.format("%.0f", dailyRate)} ₽ / день"
        updateSelectedDatesText()

        binding.btnPickDates.setOnClickListener { openDateRangePicker() }
        binding.btnPickTimes.setOnClickListener { openTimePickers() }
        binding.btnCreate.setOnClickListener { createContract() }
    }

    private fun openDateRangePicker() {
        val picker = MaterialDatePicker.Builder.dateRangePicker()
            .setTitleText(getString(R.string.create_contract))
            .build()

        picker.addOnPositiveButtonClickListener { range ->
            val startMillis = range.first
            val endMillis = range.second
            if (startMillis != null && endMillis != null) {
                selectedStartDate = millisToLocalDate(startMillis)
                selectedEndDate = millisToLocalDate(endMillis)
                updateSelectedDatesText()
            }
        }

        picker.show(childFragmentManager, "dateRangePicker")
    }

    private fun createContract() {
        val startDate = selectedStartDate
        val endDate = selectedEndDate
        if (carId <= 0L) {
            Toast.makeText(requireContext(), "Некорректный carId", Toast.LENGTH_SHORT).show()
            return
        }
        if (startDate == null || endDate == null) {
            Toast.makeText(requireContext(), "Выберите даты аренды", Toast.LENGTH_SHORT).show()
            return
        }
        if (dailyRate <= 0.0) {
            Toast.makeText(requireContext(), "Некорректная цена аренды", Toast.LENGTH_SHORT).show()
            return
        }

        val startDateTime = startDate.atTime(selectedStartTime)
        val endDateTime = endDate.atTime(selectedEndTime)
        if (!endDateTime.isAfter(startDateTime)) {
            Toast.makeText(requireContext(), "Окончание должно быть позже начала", Toast.LENGTH_SHORT).show()
            return
        }

        val request = CreateContractRequest(
            carId = carId,
            dataStart = toApiDateTime(startDateTime),
            dataEnd = toApiDateTime(endDateTime),
            dailyRate = dailyRate
        )

        lifecycleScope.launch {
            binding.progressBar.isVisible = true
            binding.btnCreate.isEnabled = false
            try {
                val response = RetrofitClient.apiService.createContract(request)
                if (response.isSuccessful) {
                    val created = response.body()
                    Log.d("CreateContract", "Created contract id=${created?.id}")
                    Toast.makeText(requireContext(), "Аренда создана", Toast.LENGTH_SHORT).show()
                    val nav = findNavController()
                    if (!nav.popBackStack(R.id.contractsFragment, false)) {
                        nav.navigate(R.id.contractsFragment)
                    }
                } else {
                    if (response.code() == 401) {
                        Toast.makeText(requireContext(), "Нужно войти заново", Toast.LENGTH_SHORT).show()
                        findNavController().navigate(R.id.loginFragment)
                    } else {
                        val msg = response.errorBody()?.string()
                        val req = response.raw().request
                        Log.e(
                            "CreateContract",
                            "Create failed code=${response.code()} ${req.method} ${req.url} body=${msg ?: "<empty>"}"
                        )
                        Toast.makeText(
                            requireContext(),
                            msg ?: "Ошибка создания аренды (${response.code()})",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), e.message ?: "Ошибка сети", Toast.LENGTH_LONG).show()
            } finally {
                binding.progressBar.isVisible = false
                binding.btnCreate.isEnabled = true
            }
        }
    }

    private fun updateSelectedDatesText() {
        val start = selectedStartDate
        val end = selectedEndDate
        if (start == null || end == null) {
            binding.tvStartDateValue.text = "—"
            binding.tvEndDateValue.text = "—"
            binding.tvStartTimeValue.text = "—"
            binding.tvEndTimeValue.text = "—"
            binding.tvTotalValue.text = "—"
        } else {
            binding.tvStartDateValue.text = start.format(dateUiFormatter)
            binding.tvEndDateValue.text = end.format(dateUiFormatter)

            binding.tvStartTimeValue.text = selectedStartTime.format(timeUiFormatter)
            binding.tvEndTimeValue.text = selectedEndTime.format(timeUiFormatter)

            val days = (ChronoUnit.DAYS.between(start, end) + 1).coerceAtLeast(1)
            val total = days * dailyRate
            binding.tvTotalLabel.text = getString(R.string.total_cost) + " • $days дн."
            binding.tvTotalValue.text = "${String.format("%.0f", total)} ₽"
        }
    }

    private fun toApiDateTime(dateTime: LocalDateTime): String {
        // OpenAPI: format date-time (LocalDateTime)
        return dateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
    }

    private fun openTimePickers() {
        val startPicker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_24H)
            .setHour(selectedStartTime.hour)
            .setMinute(selectedStartTime.minute)
            .setTitleText("Время начала")
            .build()

        startPicker.addOnPositiveButtonClickListener {
            selectedStartTime = LocalTime.of(startPicker.hour, startPicker.minute)

            val endPicker = MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_24H)
                .setHour(selectedEndTime.hour)
                .setMinute(selectedEndTime.minute)
                .setTitleText("Время окончания")
                .build()

            endPicker.addOnPositiveButtonClickListener {
                selectedEndTime = LocalTime.of(endPicker.hour, endPicker.minute)
                updateSelectedDatesText()
            }

            endPicker.show(childFragmentManager, "endTimePicker")
            updateSelectedDatesText()
        }

        startPicker.show(childFragmentManager, "startTimePicker")
    }

    private fun millisToLocalDate(millis: Long): LocalDate {
        return Instant.ofEpochMilli(millis)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
