package com.example.menghitungemisi

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.menghitungemisi.databinding.HistoryItemBinding

class HistoryAdapter(private val historyList: List<EmissionData>) :
    RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val binding = HistoryItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return HistoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        val data = historyList[position]
        holder.bind(data)
    }

    override fun getItemCount(): Int = historyList.size

    inner class HistoryViewHolder(private val binding: HistoryItemBinding) : RecyclerView.ViewHolder(binding.root) {
        @SuppressLint("SetTextI18n")
        fun bind(data: EmissionData) {
            with(binding) {
                ccValueText.text = "CC: ${data.ccValue}"
                fuelValueText.text = "Fuel: ${data.fuelValue}"
                speedText.text = "Speed: ${data.speed} km/h"
                distanceKmText.text = "Distance: ${data.distanceKm} km"
                emissionText.text = "Emission: ${data.emission}"
            }
        }
    }
}