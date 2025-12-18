package com.example.carcatalogue.ui.contracts

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.core.content.ContextCompat
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
                ContractState.CONFIRMED -> R.color.success
                ContractState.ACTIVE -> R.color.success
                ContractState.PENDING -> R.color.warning
                ContractState.CANCELLATION_REQUESTED -> R.color.warning
                ContractState.CANCELLED -> R.color.error
                ContractState.COMPLETED -> R.color.success
                ContractState.AWAITING_CANCELLATION -> R.color.warning
                null -> R.color.divider
            }
            binding.chipStatus.setChipBackgroundColorResource(statusColor)

            val statusTextColor = when (contract.state) {
                ContractState.CONFIRMED,
                ContractState.ACTIVE,
                ContractState.PENDING,
                ContractState.CANCELLATION_REQUESTED,
                ContractState.CANCELLED,
                ContractState.COMPLETED,
                ContractState.AWAITING_CANCELLATION -> R.color.white
                null -> R.color.text_secondary
            }
            binding.chipStatus.setTextColor(ContextCompat.getColor(binding.root.context, statusTextColor))
            
            binding.root.setOnClickListener {
                onContractClick(contract.id)
            }
        }
        
        private fun formatDate(dateString: String): String {
            return try {
                val outputFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

                val date = runCatching {
                    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateString)
                }.getOrNull() ?: runCatching {
                    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).parse(dateString)
                }.getOrNull()

                if (date != null) outputFormat.format(date) else dateString
            } catch (e: Exception) {
                dateString
            }
        }
        
        private fun getStatusText(state: ContractState?): String {
            val context = binding.root.context
            return when (state) {
                ContractState.PENDING -> context.getString(R.string.contract_pending)
                ContractState.CONFIRMED -> context.getString(R.string.contract_confirmed)
                ContractState.ACTIVE -> context.getString(R.string.contract_active)
                ContractState.COMPLETED -> context.getString(R.string.contract_completed)
                ContractState.CANCELLED -> context.getString(R.string.contract_cancelled)
                ContractState.CANCELLATION_REQUESTED,
                ContractState.AWAITING_CANCELLATION -> context.getString(R.string.contract_cancellation_requested)
                null -> context.getString(R.string.contract_unknown)
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
