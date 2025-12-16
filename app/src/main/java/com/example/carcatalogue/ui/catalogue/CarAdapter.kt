package com.example.carcatalogue.ui.catalogue

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.carcatalogue.R
import com.example.carcatalogue.data.model.CarListItemResponse
import com.example.carcatalogue.databinding.ItemCarPremiumBinding

class CarAdapter(private val onItemClick: (Long) -> Unit) :
    ListAdapter<CarListItemResponse, CarAdapter.CarViewHolder>(CarDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CarViewHolder {
        val binding = ItemCarPremiumBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CarViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CarViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }

    inner class CarViewHolder(private val binding: ItemCarPremiumBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(car: CarListItemResponse) {
            // Название и модель
            binding.tvCarName.text = "${car.brand} ${car.model}"
            
            // Год
            binding.tvYear.text = car.yearOfIssue.toString()
            
            // Класс
            binding.tvCarClass.text = car.carClass
            
            // Цена
            binding.tvPrice.text = car.rent.toInt().toString()
            
            // Изображение - используем заглушку, т.к. CarListItemResponse не содержит imageUrl
            binding.ivCarImage.load("https://via.placeholder.com/600x400/667eea/FFFFFF?text=${car.brand}+${car.model}") {
                crossfade(true)
                placeholder(R.drawable.ic_car)
                error(R.drawable.ic_car)
            }
            
            // Избранное
            val favoriteIcon = if (car.favorite) R.drawable.ic_favorite else R.drawable.ic_favorite_border
            binding.fabFavorite.setImageResource(favoriteIcon)
            
            binding.fabFavorite.setOnClickListener {
                // TODO: Toggle favorite
            }

            binding.btnBookNow.setOnClickListener {
                onItemClick(car.id)
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