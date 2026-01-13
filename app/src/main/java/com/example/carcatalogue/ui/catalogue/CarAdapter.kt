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

class CarAdapter(
    private val onItemClick: (Long) -> Unit,
    private val onFavoriteClick: ((Long, Boolean) -> Unit)? = null
) :
    ListAdapter<CarListItemResponse, CarAdapter.CarViewHolder>(CarDiffCallback()) {

    private val defaultPhotos = intArrayOf(
        R.drawable.default_car_photo_1,
        R.drawable.default_car_photo_2,
        R.drawable.default_car_photo_3,
        R.drawable.default_car_photo_4
    )

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
            
            // Изображение - загружаем из URL или показываем дефолтное "фото"
            if (!car.imageUrl.isNullOrEmpty()) {
                binding.ivCarImage.load(car.imageUrl) {
                    crossfade(true)
                    placeholder(defaultPhotoRes(car))
                    error(defaultPhotoRes(car))
                }
            } else {
                binding.ivCarImage.load(defaultPhotoRes(car)) {
                    crossfade(false)
                }
            }
            
            // Избранное
            val favoriteIcon = if (car.favorite) R.drawable.ic_favorite else R.drawable.ic_favorite_border
            binding.fabFavorite.setImageResource(favoriteIcon)
            
            binding.fabFavorite.setOnClickListener {
                onFavoriteClick?.invoke(car.id, car.favorite)
            }

            binding.btnBookNow.setOnClickListener {
                onItemClick(car.id)
            }
            
            binding.root.setOnClickListener {
                onItemClick(car.id)
            }
        }

        private fun defaultPhotoRes(car: CarListItemResponse): Int {
            return when (car.carClass.uppercase()) {
                "ECONOMY" -> R.drawable.default_car_photo_3
                "COMFORT" -> R.drawable.default_car_photo_1
                "BUSINESS" -> R.drawable.default_car_photo_4
                "PREMIUM", "LUXURY" -> R.drawable.default_car_photo_2
                else -> defaultPhotos[(kotlin.math.abs(car.id) % defaultPhotos.size).toInt()]
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