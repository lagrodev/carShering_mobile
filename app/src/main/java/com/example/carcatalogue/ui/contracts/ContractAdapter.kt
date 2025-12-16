package com.example.carcatalogue.ui.contracts

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.carcatalogue.R
import com.example.carcatalogue.data.model.ContractResponse
import com.example.carcatalogue.data.model.ContractState
import com.example.carcatalogue.databinding.ItemContractBinding
import java.text.SimpleDateFormat
import java.util.*

class ContractAdapter(
    private val onContractClick: (Long) -> Unit
) : ListAdapter<ContractResponse, ContractAdapter.ContractViewHolder>(ContractDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContractViewHolder {
        val binding = ItemContractBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ContractViewHolder(binding, onContractClick)
    }

    override fun onBindViewHolder(holder: ContractViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ContractViewHolder(
        private val binding: ItemContractBinding,
        private val onContractClick: (Long) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(contract: ContractResponse) {
            binding.tvCarName.text = "${contract.brand} ${contract.model}"
            binding.tvCarDetails.text = "${contract.carClass} • ${contract.yearOfIssue} • ${contract.gosNumber}"
            binding.tvStartDate.text = formatDate(contract.startDate)
            binding.tvEndDate.text = formatDate(contract.endDate)
            binding.tvTotalCost.text = "${String.format("%.0f", contract.totalCost)} ₽"
            binding.chipStatus.text = getStatusText(contract.state)
            
            // Set status chip color
            val statusColor = when (contract.state) {
                ContractState.ACTIVE -> R.color.success
                ContractState.PENDING -> R.color.warning
                ContractState.CANCELLED -> R.color.error
                ContractState.COMPLETED -> R.color.success
                ContractState.AWAITING_CANCELLATION -> R.color.warning
            }
            binding.chipStatus.setChipBackgroundColorResource(statusColor)
            
            binding.root.setOnClickListener {
                onContractClick(contract.id)
            }
        }
        
        private fun formatDate(dateString: String): String {
            return try {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val outputFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
                val date = inputFormat.parse(dateString)
                outputFormat.format(date ?: Date())
            } catch (e: Exception) {
                dateString
            }
        }
        
        private fun getStatusText(state: ContractState): String {
            return when (state) {
                ContractState.ACTIVE -> "Активен"
                ContractState.PENDING -> "Ожидает"
                ContractState.CANCELLED -> "Отменен"
                ContractState.COMPLETED -> "Завершен"
                ContractState.AWAITING_CANCELLATION -> "Ожидает отмены"
            }
        }
    }

    class ContractDiffCallback : DiffUtil.ItemCallback<ContractResponse>() {
        override fun areItemsTheSame(oldItem: ContractResponse, newItem: ContractResponse): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ContractResponse, newItem: ContractResponse): Boolean {
            return oldItem == newItem
        }
    }
}
