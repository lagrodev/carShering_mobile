package com.example.carcatalogue.ui.catalogue

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.carcatalogue.R
import com.example.carcatalogue.data.model.CarListItemResponse
import com.example.carcatalogue.databinding.ItemCarBinding

class CarAdapter(private val onItemClick: (Long) -> Unit) :
    ListAdapter<CarListItemResponse, CarAdapter.CarViewHolder>(CarDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CarViewHolder {
        val binding = ItemCarBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CarViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CarViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }

    inner class CarViewHolder(private val binding: ItemCarBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(car: CarListItemResponse) {
            binding.carBrandModel.text = "${car.brand} ${car.model}"
            binding.carYear.text = car.yearOfIssue.toString()
            binding.carPrice.text = "${car.rent} ₽/day"

            // Заглушка для изображения — можно заменить на реальный URL позже
            binding.carImage.load("https://via.placeholder.com/150?text=${car.brand}+${car.model}") {
                placeholder(R.drawable.ic_car_placeholder)
                error(R.drawable.ic_car_placeholder)
            }

            binding.root.setOnClickListener {
                onItemClick(car.id)
            }
        }
    }

    class CarDiffCallback : DiffUtil.ItemCallback<CarListItemResponse>() {
        override fun areItemsTheSame(oldItem: CarListItemResponse, newItem: CarListItemResponse): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: CarListItemResponse, newItem: CarListItemResponse): Boolean {
            return oldItem == newItem
        }
    }
}